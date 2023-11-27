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

  def calculateProbability(c: CalculateProbabilityScenarioPed): Double

  def calculateProbabilityActor(calculateProbabilityScenarioPed: CalculateProbabilityScenarioPed): Future[Double]
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
              profiles <- profileRepository.findByCodes(codes)
              analysisType <- calculationTypeService
                .getAnalysisTypeByCalculation(BayesianNetwork.name)
              frequencyTable <- bayesianNetworkService
                .getFrequencyTable(frequencyTableName)
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
                  mutationService
                    .getAllPossibleAllelesByLocus()
                    .flatMap(
                      n => {
                        bayesianNetworkService
                          .getGenotypification(
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
                            genotypification => {
                              val newGenotypification = genotypification
                                .map(
                                  plainCPT => PlainCPT2(
                                    plainCPT.header,
                                    plainCPT.matrix.toArray
                                  )
                                )
                              val pedigreeGenotypification =
                                PedigreeGenotypification(
                                  pedigree._id,
                                  newGenotypification,
                                  pedigree.boundary,
                                  pedigree.frequencyTable.get,
                                  unknowns
                                )
                              logger.info("--- SAVE GENO END / Main branch ---")
                              pedigreeGenotypificationRepository
                                .upsertGenotypification(
                                  pedigreeGenotypification
                                )
                            }
                          }
                      }
                    )
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
  ):Double = {
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
  ):Future[Double] = {
    implicit val timeout: Timeout = akka.util
      .Timeout(Some(Duration("30 days"))
      .collect { case d: FiniteDuration => d }.get)
    val result = (
      bayesianGenotypificationActor ?
        calculateProbabilityScenarioPed
      )
      .mapTo[Double]
    result
  }
}
