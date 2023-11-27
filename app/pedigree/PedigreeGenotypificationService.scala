package pedigree

import javax.inject.{Inject, Singleton}
import akka.actor.{ActorRef, ActorSystem}
import connections.SendRequestActor
import kits.AnalysisType
import pedigree.BayesianNetwork.{FrequencyTable, Linkage}
import probability.CalculationTypeService
import profile.{Profile, ProfileRepository}
import play.api.i18n.Messages
import types.SampleCode
import java.security.SecureRandom

import matching._
import java.util.{Calendar, Date}

import javax.inject.{Inject, Named, Singleton}
import akka.pattern.ask
import org.apache.commons.codec.binary.Base64
import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import audit.PEOSignerActor
import connections.ProfileTransfer

import scala.concurrent.Await
import models.Tables.ExternalProfileDataRow
import configdata.{CategoryConfiguration, CategoryRepository, CategoryService}
import inbox._
import kits.AnalysisType
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSBody, _}
import profile.{Profile, ProfileService}
import profiledata.{DeletedMotive, ProfileData, ProfileDataService}
import types.{AlphanumericId, Permission, SampleCode}
import user.{RoleService, UserService}
import connections.MatchSuperiorInstance
import util.FutureUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.async.Async.{async, await}
import play.api.i18n.Messages

import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS}
import scala.util.Try
import inbox.DiscardInfoInbox
import inbox.HitInfoInbox
import inbox.DeleteProfileInfo
import trace.{MatchInfo, MatchTypeInfo, Trace, TraceService}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait PedigreeGenotypificationService {
  def generateGenotypificationAndFindMatches(pedigreeId: Long): Future[Either[String, Long]]

  def saveGenotypification(pedigree: PedigreeGenogram, profiles: Array[Profile],
                           frequencyTable: BayesianNetwork.FrequencyTable, analysisType: AnalysisType, linkage: Linkage,
                           mutationModel: Option[MutationModel]): Future[Either[String, Long]]

  def calculateProbability(c:CalculateProbabilityScenarioPed):Double

  def calculateProbabilityActor(calculateProbabilityScenarioPed:CalculateProbabilityScenarioPed):Future[Double]
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
  mutationService: MutationService = null,
  mutationRepository: MutationRepository = null
) extends PedigreeGenotypificationService {

  val logger = Logger(this.getClass())
  implicit val executionContext = akkaSystem
    .dispatchers
    .lookup("play.akka.actor.pedigree-context")
  val bayesianGenotypificationActor: ActorRef = akkaSystem
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
            case Right(id) => pedigreeSparkMatcher
              .findMatchesInBackGround(pedigreeId)
            case Left(error) => ()
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
      ).flatMap(
        result => {
          if(result.isRight) {
            val markers = profileRepository.
              getProfilesMarkers(profiles)
//            markers.foreach(
//              marker => {
//                logger.info(s"Marker: ${marker}")
//              }
//            )
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
          } else {
            logger.info("--- SAVE GENO END / Secondary branch ---")
            Future.successful(Left(result.left.get))
          }
      }
    )
  }
  def saveGenotypificationActor(
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
    implicit val timeout = akka.util
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

