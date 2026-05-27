package pedigree

import kits.AnalysisType
import pedigree.BayesianNetwork.Linkage
import play.api.Logger
import probability.CalculationTypeService
import profile.{Profile, ProfileRepository}

import scala.concurrent.{ExecutionContext, Future}

// ---------------------------------------------------------------------------
// PedigreeGenotypificationService
// Modern implementation uses PedigreeMatcher directly (no Spark, no Akka actor).
// ---------------------------------------------------------------------------

trait PedigreeGenotypificationService:
  def generateGenotypificationAndFindMatches(pedigreeId: Long): Future[Either[String, Long]]
  def saveGenotypification(
    pedigree: PedigreeGenogram,
    profiles: Array[Profile],
    frequencyTable: BayesianNetwork.FrequencyTable,
    analysisType: AnalysisType,
    linkage: Linkage,
    mutationModel: Option[MutationModel]
  ): Future[Either[String, Long]]
  def calculateProbability(c: CalculateProbabilityScenarioPed): Double
  def calculateProbabilityActor(c: CalculateProbabilityScenarioPed): Future[Double]

@jakarta.inject.Singleton
class PedigreeGenotypificationServiceStub extends PedigreeGenotypificationService:
  override def generateGenotypificationAndFindMatches(pedigreeId: Long): Future[Either[String, Long]] = Future.successful(Right(pedigreeId))
  override def saveGenotypification(pedigree: PedigreeGenogram, profiles: Array[Profile], frequencyTable: BayesianNetwork.FrequencyTable, analysisType: AnalysisType, linkage: Linkage, mutationModel: Option[MutationModel]): Future[Either[String, Long]] = Future.successful(Right(pedigree._id))
  override def calculateProbability(c: CalculateProbabilityScenarioPed): Double = 0.0
  override def calculateProbabilityActor(c: CalculateProbabilityScenarioPed): Future[Double] = Future.successful(0.0)

// ---------------------------------------------------------------------------
// PedigreeGenotypificationServiceImpl
// ---------------------------------------------------------------------------

@jakarta.inject.Singleton
class PedigreeGenotypificationServiceImpl @jakarta.inject.Inject() (
  profileRepository: ProfileRepository,
  calculationTypeService: CalculationTypeService,
  bayesianNetworkService: BayesianNetworkService,
  pedigreeGenotypificationRepository: PedigreeGenotypificationRepository,
  pedigreeMatcher: PedigreeMatcher,
  pedigreeRepository: PedigreeRepository,
  mutationService: MutationService,
  mutationRepository: MutationRepository,
  pedigreeDataRepository: PedigreeDataRepository
)(using ec: ExecutionContext) extends PedigreeGenotypificationService:

  private val logger: Logger = Logger(this.getClass)

  override def generateGenotypificationAndFindMatches(pedigreeId: Long): Future[Either[String, Long]] =
    pedigreeRepository.get(pedigreeId).flatMap { pedigreeOpt =>
      val pedigree = pedigreeOpt.get
      val frequencyTableName = pedigree.frequencyTable
        .getOrElse(throw new RuntimeException("error.E0201"))
      val codes = pedigree.genogram.flatMap(_.globalCode).toList
      val result = (for
        profiles      <- profileRepository.findByCodes(codes)
        analysisType  <- calculationTypeService.getAnalysisTypeByCalculation(BayesianNetwork.name)
        frequencyTable <- bayesianNetworkService.getFrequencyTable(frequencyTableName)
        linkage       <- bayesianNetworkService.getLinkage()
        mutationModel <-
          if pedigree.mutationModelId.isDefined then mutationRepository.getMutationModel(pedigree.mutationModelId)
          else Future.successful(None)
      yield
        saveGenotypification(pedigree, profiles.toArray, frequencyTable._2, analysisType, linkage, mutationModel)
      ).flatMap(identity).recover { case err =>
        logger.error(s"generateGenotypificationAndFindMatches failed for pedigree=$pedigreeId", err)
        Left("error.E0630")
      }

      result.foreach {
        case Right(_) => pedigreeMatcher.findMatchesInBackGround(pedigreeId)
        case Left(_)  => ()
      }
      result
    }

  override def saveGenotypification(
    pedigree: PedigreeGenogram,
    profiles: Array[Profile],
    frequencyTable: BayesianNetwork.FrequencyTable,
    analysisType: AnalysisType,
    linkage: Linkage,
    mutationModel: Option[MutationModel]
  ): Future[Either[String, Long]] =
    mutationService.generateN(profiles, mutationModel).flatMap {
      case Left(err) => Future.successful(Left(err))
      case Right(_) =>
        val markers            = profileRepository.getProfilesMarkers(profiles)
        val unknowns           = pedigree.genogram.filter(_.unknown).map(_.alias.text).toArray
        val mutationModelType  = mutationModel.map(_.mutationType)
        mutationService.getMutationModelData(mutationModel, markers).flatMap { mutModelData =>
          mutationService.getAllPossibleAllelesByLocus().flatMap { n =>
            bayesianNetworkService.getGenotypification(
              pedigree, profiles, frequencyTable, analysisType, linkage,
              mutationModelType, mutModelData, n
            ).flatMap { genotypification =>
              val newGenotypification = genotypification.map(cpt => PlainCPT2(cpt.header, cpt.matrix.toArray))
              val pedigreeGeno = PedigreeGenotypification(
                pedigree._id,
                newGenotypification,
                pedigree.boundary,
                pedigree.frequencyTable.get,
                unknowns
              )
              pedigreeGenotypificationRepository.upsertGenotypification(pedigreeGeno)
            }
          }
        }.recoverWith { case err =>
          pedigreeRepository.changeStatus(pedigree.idCourtCase, PedigreeStatus.UnderConstruction)
          pedigreeDataRepository.getCourtCase(pedigree.idCourtCase).map { ccOpt =>
            val caseId = ccOpt.map(_.internalSampleCode).getOrElse("[unknown]")
            val msg = err match
              case _: PedigreeNotHavingUnknownException => s"error.E0212|$caseId"
              case _ =>
                s"Se encontró una excepción mientras se procesaba un pédigri del caso $caseId: ${err.getMessage}"
            logger.error(msg, err)
            Left(msg)
          }
        }
    }

  override def calculateProbability(c: CalculateProbabilityScenarioPed): Double =
    BayesianNetwork.calculateProbability(
      c.profiles, c.genogram, c.frequencyTable, c.analysisType,
      c.linkage, c.verbose, c.mutationModelType, c.mutationModelData,
      c.seenAlleles, c.locusRangeMap
    )

  override def calculateProbabilityActor(c: CalculateProbabilityScenarioPed): Future[Double] =
    Future(calculateProbability(c))
