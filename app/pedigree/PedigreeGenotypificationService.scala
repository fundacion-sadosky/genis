package pedigree

import javax.inject.{Inject, Singleton}
import akka.actor.{ActorRef, ActorSystem}
import kits.AnalysisType
import pedigree.BayesianNetwork.Linkage
import probability.CalculationTypeService
import profile.{Profile, ProfileRepository}
import play.api.i18n.Messages
import akka.pattern.ask
import akka.dispatch.MessageDispatcher
import akka.util.Timeout
import play.api.Logger
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration}

trait PedigreeGenotypificationService {
  def generateGenotypificationAndFindMatches(pedigreeId: Long): Future[Either[String, Long]]

  def saveGenotypification(
    pedigree: PedigreeGenogram,
    profiles: Array[Profile],
    frequencyTable: BayesianNetwork.FrequencyTable,
    analysisType: AnalysisType,
    linkage: Linkage,
    mutationModel: Option[MutationModel]
  ): Future[Either[String, Long]]

  def calculateProbability(c: CalculateProbabilityScenarioPed): (Double, Map[String, MarkerLRDetail])

  def calculateProbabilityActor(calculateProbabilityScenarioPed: CalculateProbabilityScenarioPed): Future[(Double, Map[String, MarkerLRDetail])]
}

@Singleton
class PedigreeGenotypificationServiceImpl @Inject()(
  akkaSystem: ActorSystem,
  profileRepository: ProfileRepository,
  calculationTypeService: CalculationTypeService,
  bayesianNetworkService: BayesianNetworkService,
  pedigreeGenotypificationRepository: PedigreeGenotypificationRepository,
  pedigreeSparkMatcher: PedigreeSparkMatcher,
  pedigreeRepository: PedigreeRepository,
  mutationService: MutationService,
  mutationRepository: MutationRepository,
  pedigreeDataRepository: PedigreeDataRepository
) extends PedigreeGenotypificationService {

  val logger: Logger = Logger(this.getClass)
  implicit val executionContext: MessageDispatcher = akkaSystem
    .dispatchers
    .lookup("play.akka.actor.pedigree-context")
  private val bayesianGenotypificationActor: ActorRef = akkaSystem
    .actorOf(
      BayesianGenotypificationActor
        .props(this)
    )

  override def generateGenotypificationAndFindMatches(
    pedigreeId: Long
  ): Future[Either[String, Long]] = {
    pedigreeRepository
      .get(pedigreeId)
      .flatMap {
        pedigreeOpt => {
          val pedigree = pedigreeOpt.get
          val frequencyTableName = pedigree
            .frequencyTable
            .getOrElse(throw new RuntimeException(Messages("error.E0201")))
          val codes = pedigree
            .genogram
            .flatMap(_.globalCode)
            .toList
          val result = {
            for {
              // Un familiar tipificado con un nombre de ACTBP2/SE33 distinto del que usa
              // la tabla de frecuencias en curso no empataría por nombre con la CPT y ese marcador
              // quedaría afuera; y un familiar con dropout ("0") en algún marcador no
              // debe aportar ese locus a la CPT. La huella y el universo que se persisten salen
              // de estos mismos perfiles normalizados, por eso la tabla se resuelve primero.
              frequencyTable <- bayesianNetworkService
                .getFrequencyTable(frequencyTableName)
              profiles <- profileRepository.findByCodes(codes)
                .map(_.map(BayesianNetwork.normalizarParaParentesco(_, frequencyTable._2)))
              analysisType <- calculationTypeService
                .getAnalysisTypeByCalculation(BayesianNetwork.name)
              linkage <- bayesianNetworkService.getLinkage()
              mutationModel <- if (pedigree.mutationModelId.isDefined) {
                mutationRepository
                  .getMutationModel(pedigree.mutationModelId)
              } else {
                Future.successful(None)
              }
            } yield {
              saveGenotypificationActor(
                pedigree,
                profiles.toArray,
                frequencyTable._2,
                analysisType,
                linkage,
                mutationModel
              )
            }
          }
          .flatMap(identity).recover {
            case err =>
              err.printStackTrace()
              Left(err.getMessage)
          }
          result.foreach {
            case Right(_) => pedigreeSparkMatcher
              .findMatchesInBackGround(pedigreeId)
            case Left(_) => ()
          }
          result
        }
    }
  }

  override def saveGenotypification(
    pedigree: PedigreeGenogram,
    profiles: Array[Profile],
    frequencyTable: BayesianNetwork.FrequencyTable,
    analysisType: AnalysisType,
    linkage: Linkage,
    mutationModel: Option[MutationModel]
  ): Future[Either[String, Long]] = {
    logger.info("--- SAVE GENO BEGIN ---")
    mutationService
      .generateN(
        profiles,
        mutationModel
      )
      .flatMap(
        result => {
          if(result.isRight) {
            val markers = profileRepository.
              getProfilesMarkers(profiles)
            val unknowns = pedigree
              .genogram
              .filter(_.unknown)
              .map(_.alias.text)
              .toArray
            val mutationModelType: Option[Long] = if (mutationModel.nonEmpty) {
              Some(mutationModel.get.mutationType)
            } else {
              None
            }
            mutationService
              .getMutationModelData(mutationModel, markers)
              .flatMap(
                mutationModelData => {
                  // El universo con que se construye la CPT: el panel mas los
                  // alelos observados en la familia. Se persiste con el documento.
                  val n = BayesianNetwork.extendNWithObservedAlleles(
                    BayesianNetwork.getNFromTable(frequencyTable),
                    profiles,
                    pedigree.genogram.toArray,
                    analysisType
                  )
                  // Las cuatro genotipificaciones (principal, estricta, H2 y
                  // con-marcador) salen de una sola composicion compartida con
                  // rebuildMarker y con las pruebas de validacion; el servicio bayesiano
                  // le suma lo que viene de la base (los rangos de locus).
                  bayesianNetworkService
                    .getFamilyGenotypification(
                      pedigree,
                      profiles,
                      frequencyTable,
                      analysisType,
                      linkage,
                      mutationModelType,
                      mutationModelData,
                      n
                    )
                    .flatMap {
                      genotypification =>
                        val pedigreeGenotypification =
                          PedigreeGenotypification(
                            pedigree._id,
                            genotypification.genotypification,
                            pedigree.boundary,
                            pedigree.frequencyTable.get,
                            unknowns,
                            genotypification.strictGenotypification,
                            genotypification.genotypificationH2,
                            genotypification.genotypificationWithMarkers,
                            SingleBandExclusions.byIndividual(
                              profiles,
                              pedigree.genogram.toArray,
                              frequencyTable
                            ),
                            universe = n,
                            familyFingerprint = Some(
                              CandidateUniverse.familyFingerprint(profiles, pedigree.genogram.toArray)
                            )
                          )
                        logger.info("--- SAVE GENO END / Main branch ---")
                        pedigreeGenotypificationRepository
                          .upsertGenotypification(
                            pedigreeGenotypification
                          )
                    }
                }
              )
              .recoverWith {
                case err =>
                  pedigreeRepository
                    .changeStatus(pedigree.idCourtCase, PedigreeStatus.UnderConstruction)
                  pedigreeDataRepository
                    .getCourtCase(pedigree.idCourtCase)
                    .map {
                      case None => "[unknown]"
                      case Some(x) => x.internalSampleCode
                    }
                    .map {
                      caseId =>
                        err match {
                          case _: PedigreeNotHavingUnknownException =>
                            Messages("error.E0212", caseId)
                          case e: Exception =>
                            s"""Se encontró una excepción mientras se procesaba
                            |un pédigri del caso $caseId: ${e.getMessage}"""
                              .stripMargin
                              .replaceAll("\n", "")
                        }
                    }
                    .map {
                      msg =>
                        logger.error(msg)
                        Left(msg)
                    }
              }
          } else {
            logger.info("--- SAVE GENO END / Secondary branch ---")
            Future.successful(Left(result.left.get))
          }
      }
    )
  }

  private def saveGenotypificationActor(
    pedigree: PedigreeGenogram,
    profiles: Array[Profile],
    frequencyTable: BayesianNetwork.FrequencyTable,
    analysisType: AnalysisType, linkage: Linkage,
    mutationModel: Option[MutationModel]
  ): Future[Either[String, Long]] = {
    implicit val timeout: Timeout = akka.util
      .Timeout(Some(Duration("30 days"))
      .collect { case d: FiniteDuration => d }.get)
    val result = (
      bayesianGenotypificationActor ?
        SaveGenotypification(
          pedigree,
          profiles,
          frequencyTable,
          analysisType,
          linkage,
          mutationModel
        )
      )
      .mapTo[Either[String, Long]]
    result
  }

  override def calculateProbability(
    c:CalculateProbabilityScenarioPed
  ):(Double, Map[String, MarkerLRDetail]) = {
    logger.info("--- CalculateProbability BEGIN ---")
    val probability = BayesianNetwork
      .calculateProbability(
        c.profiles,
        c.genogram,
        c.frequencyTable,
        c.analysisType,
        c.linkage,
        c.verbose,
        c.mutationModelType,
        c.mutationModelData,
        c.seenAlleles,
        c.locusRangeMap
      )
    logger.info("--- CalculateProbability END ---")
    probability
  }

  override def calculateProbabilityActor(
    calculateProbabilityScenarioPed:CalculateProbabilityScenarioPed
  ):Future[(Double, Map[String, MarkerLRDetail])] = {
    implicit val timeout: Timeout = akka.util
      .Timeout(Some(Duration("30 days"))
      .collect { case d: FiniteDuration => d }.get)
    val result = (
      bayesianGenotypificationActor ?
        calculateProbabilityScenarioPed
      )
      .mapTo[(Double, Map[String, MarkerLRDetail])]
    result
  }
}
