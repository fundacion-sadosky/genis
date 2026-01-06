package pedigree

import javax.inject.{Inject, Singleton}
import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher
import inbox.{NotificationService, PedigreeLRInfo}
import kits.{AnalysisType, LocusService}
import matching.{AleleRange, NewMatchingResult}
import pedigree.BayesianNetwork.{FrequencyTable, Linkage, MutationModelData}
import play.api.libs.json.Json
import probability.CalculationTypeService
import profile.{Profile, ProfileRepository}
import stats.{PopulationBaseFrequency, PopulationBaseFrequencyService, PopulationSampleFrequency}
import user.UserService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


trait BayesianNetworkService {
  def getGenotypification(
    pedigree: PedigreeGenogram,
    profiles: Array[Profile],
    frequencyTable: FrequencyTable,
    analysisType: AnalysisType,
    linkage: Linkage,
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String,List[Double]] = Map.empty): Future[Array[PlainCPT]]
  def calculateProbability(scenario: PedigreeScenario): Future[Double]
  def getFrequencyTable(name: String): Future[(String, FrequencyTable)]
  def getFrequencyTables(names: Seq[String]): Future[Map[String, FrequencyTable]]
  def getLinkage(): Future[Linkage]
}

@Singleton
class BayesianNetworkServiceImpl @Inject() (
   akkaSystem: ActorSystem,
   profileRepository: ProfileRepository,
   populationBaseFrequencyService: PopulationBaseFrequencyService,
   calculationTypeService: CalculationTypeService,
   pedigreeScenarioService: PedigreeScenarioService,
   notificationService: NotificationService,
   pedigreeService: PedigreeService,
   locusService: LocusService = null,
   mutationService: MutationService = null,
   mutationRepository: MutationRepository = null,
   pedigreeGenotypificationService: PedigreeGenotypificationService = null,
   userService: UserService = null
) extends BayesianNetworkService {

  implicit val executionContext: MessageDispatcher = akkaSystem
    .dispatchers
    .lookup("play.akka.actor.pedigree-context")
  // con este nombre se configura en el application.conf
  val calculation = "pedigree"
  override def calculateProbability(scenario: PedigreeScenario):
    Future[Double] = {
    val codes = scenario.genogram.flatMap(_.globalCode).toList

    val lrFuture = (
      for {
        _ <- pedigreeScenarioService.updateScenario(
          getScenarioWithProcessing(scenario, processing = true)
        )
        profiles <- profileRepository.findByCodes(codes)
        analysisType <- calculationTypeService
          .getAnalysisTypeByCalculation(calculation)
        frequencyTable <- getFrequencyTable(scenario.frequencyTable)
        linkage <- getLinkage()
        mutationModel <- if (scenario.mutationModelId.isDefined) {
          mutationRepository.getMutationModel(scenario.mutationModelId)
        } else {
          Future.successful(None)
        }
        mutationModelData <- mutationService
          .getMutationModelData(
            mutationModel,
            profileRepository.getProfilesMarkers(profiles.toArray)
          )
        n <- mutationService.getAllPossibleAllelesByLocus()
        listLocus <- locusService.list()
      } yield {
        val locusRangesDefault: NewMatchingResult.AlleleMatchRange =
          listLocus
            .map(
              locus =>
                locus.id -> AleleRange(
                  locus.minAlleleValue.getOrElse(0),
                  locus.maxAlleleValue.getOrElse(99)
                )
            )
            .toMap
        val mutationModelType: Option[Long] = mutationModel map (_.mutationType)
        pedigreeGenotypificationService
          .calculateProbabilityActor(
            CalculateProbabilityScenarioPed(
              profiles.toArray,
              scenario.genogram.toArray,
              frequencyTable._2,
              analysisType,
              linkage,
              verbose = false,
              mutationModelType,
              mutationModelData,
              n,
              locusRangesDefault
            )
          )
      }
    )
    .flatMap(identity)

    lrFuture
      .foreach {
        lr =>
          pedigreeScenarioService
            .updateScenario(getScenarioWithLR(scenario, lr))
          pedigreeService
            .getPedigree(scenario.pedigreeId)
            .foreach {
              pedigree =>
                notificationService
                  .push(
                    pedigree.get.pedigreeMetaData.assignee,
                    PedigreeLRInfo(
                      scenario.pedigreeId,
                      pedigree.get.pedigreeMetaData.courtCaseId,
                      scenario.name
                    )
                  )
                userService
                  .sendNotifToAllSuperUsers(
                    PedigreeLRInfo(
                      scenario.pedigreeId,
                      pedigree.get.pedigreeMetaData.courtCaseId,
                      scenario.name
                    ),
                    Seq(pedigree.get.pedigreeMetaData.assignee)
                  )
            }
      }

    lrFuture.onFailure {
      case _ => pedigreeScenarioService
        .updateScenario(
          getScenarioWithProcessing(
            scenario,
            processing = false
          )
        )
    }
    lrFuture
  }

  private def getScenarioWithProcessing(
    scenario: PedigreeScenario,
    processing: Boolean
  ): PedigreeScenario = {
    scenario.copy(isProcessing = processing)
  }

  private def getScenarioWithLR(
    scenario: PedigreeScenario,
    lr: Double
  ): PedigreeScenario = {
    scenario.copy(
      lr = Some(lr.toString),
      isProcessing = false
    )
  }

  override def getGenotypification(
    pedigree: PedigreeGenogram,
    profiles: Array[Profile],
    frequencyTable: FrequencyTable,
    analysisType: AnalysisType,
    linkage: Linkage,
    mutationModelType: Option[Long],
    mutationModelData: Option[List[MutationModelData]],
    n: Map[String,List[Double]] = Map.empty
  ): Future[Array[PlainCPT]] = {
    Future {
      val normalizedFrequencyTable = BayesianNetwork
        .getNormalizedFrequencyTable(frequencyTable)
      BayesianNetwork
        .getGenotypification(
          profiles,
          pedigree.genogram.toArray,
          normalizedFrequencyTable,
          analysisType,
          linkage,
          None,
          verbose = true,
          locusService.locusRangeMap(),
          mutationModelType,
          mutationModelData,
          n
        )
    }
  }

//  def createGenotypification(
//    genogram: Seq[Individual],
//    frequencyTable: String
//  ): Future[Map[Nothing, Nothing]] = {
//    Future.successful(Map.empty)
//  }

  override def getFrequencyTable(name: String):
    Future[(String, FrequencyTable)] = {
    populationBaseFrequencyService
      .getByName(name)
      .map {
        baseOpt =>
          val mapAlleleToFrq = (e:(String, Seq[PopulationSampleFrequency])) =>
            e match {
              case (marker, bf) =>
                marker -> bf
                  .map(
                    av => av.allele -> av.frequency.toDouble
                  )
                  .toMap
            }
          val baseFreqMap = baseOpt
            .get
            .base
            .groupBy(_.marker)
            .map(mapAlleleToFrq)
          (name, baseFreqMap)
    }
  }

  override def getFrequencyTables(
    names: Seq[String]
  ): Future[Map[String, FrequencyTable]] = {
    Future
      .sequence(
        names.map { name => getFrequencyTable(name)}
      )
      .map(seq => seq.toMap)
  }

  override def getLinkage(): Future[Linkage] = {
    Future.successful(Map.empty)
  }

  /*def findExclusions(pedigree: PedigreeGenogram): Future[Map[SampleCode, Map[Profile.Marker, Boolean]]] = {
    val codes = pedigree.genogram.flatMap(ind => ind.globalCode.map(_ -> ind.alias.text)).toMap

    for {
      profiles <- profileRepository.findByCodes(codes.keys.toList)
      analysisType <- calculationTypeService.getAnalysisTypeByCalculation(calculation)
      frequencyTable <- getFrequencyTable(pedigree.frequencyTable.get)
    } yield {
      val genotypifications = profiles.map(p => codes(p.globalCode) -> BayesianNetwork.getQueryProfileAlleles(p, analysisType, frequencyTable._2)).toMap

      pedigree.genogram.flatMap { individual =>
        individual.globalCode.map { gc =>
          val markers = frequencyTable._2.keys
          gc -> markers.map { marker =>
            val alleles = genotypifications.get(individual.alias.text).flatMap(_.get(marker))
            val allelesM = individual.idMother.flatMap(m => genotypifications.get(m.text).flatMap(_.get(marker)))
            val allelesP = individual.idFather.flatMap(m => genotypifications.get(m.text).flatMap(_.get(marker)))
            marker -> BayesianNetwork.allelesMutated(alleles, allelesM, allelesP)
          }.toMap
        }
      }.toMap

    }

  }*/

}
