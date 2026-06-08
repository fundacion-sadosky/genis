package pedigree

import inbox.{NotificationService, PedigreeLRInfo}
import kits.{AnalysisType, LocusService}
import matching.{AleleRange, NewMatchingResult}
import play.api.Logger
import probability.CalculationTypeService
import profile.{Profile, ProfileRepository}
import services.UserService
import stats.{PopulationSampleFrequency, PopulationBaseFrequencyService}

import scala.concurrent.{ExecutionContext, Future}

// ---------------------------------------------------------------------------
// BayesianNetworkService
// ---------------------------------------------------------------------------

trait BayesianNetworkService:
  def getGenotypification(
    pedigree: PedigreeGenogram,
    profiles: Array[profile.Profile],
    frequencyTable: BayesianNetwork.FrequencyTable,
    analysisType: AnalysisType,
    linkage: BayesianNetwork.Linkage,
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String, List[Double]] = Map.empty
  ): Future[Array[PlainCPT]]

  def calculateProbability(scenario: PedigreeScenario): Future[Double]
  def getFrequencyTable(name: String): Future[(String, BayesianNetwork.FrequencyTable)]
  def getFrequencyTables(names: Seq[String]): Future[Map[String, BayesianNetwork.FrequencyTable]]
  def getLinkage(): Future[BayesianNetwork.Linkage]

@jakarta.inject.Singleton
class BayesianNetworkServiceStub extends BayesianNetworkService:
  override def getGenotypification(
    pedigree: PedigreeGenogram,
    profiles: Array[profile.Profile],
    frequencyTable: BayesianNetwork.FrequencyTable,
    analysisType: AnalysisType,
    linkage: BayesianNetwork.Linkage,
    mutationModelType: Option[Long],
    mutationModelData: Option[List[MutationModelData]],
    n: Map[String, List[Double]]
  ): Future[Array[PlainCPT]] = Future.successful(Array.empty)

  override def calculateProbability(scenario: PedigreeScenario): Future[Double] =
    Future.successful(0.0)

  override def getFrequencyTable(name: String): Future[(String, BayesianNetwork.FrequencyTable)] =
    Future.successful((name, Map.empty))

  override def getFrequencyTables(names: Seq[String]): Future[Map[String, BayesianNetwork.FrequencyTable]] =
    Future.successful(Map.empty)

  override def getLinkage(): Future[BayesianNetwork.Linkage] =
    Future.successful(Map.empty)

// ---------------------------------------------------------------------------
// BayesianNetworkServiceImpl
// ---------------------------------------------------------------------------

@jakarta.inject.Singleton
class BayesianNetworkServiceImpl @jakarta.inject.Inject() (
  profileRepository: ProfileRepository,
  populationBaseFrequencyService: PopulationBaseFrequencyService,
  calculationTypeService: CalculationTypeService,
  pedigreeScenarioServiceProvider: jakarta.inject.Provider[PedigreeScenarioService],
  notificationService: NotificationService,
  pedigreeServiceProvider: jakarta.inject.Provider[PedigreeService],
  locusService: LocusService,
  mutationService: MutationService,
  mutationRepository: MutationRepository,
  pedigreeGenotypificationServiceProvider: jakarta.inject.Provider[PedigreeGenotypificationService],
  userService: UserService
)(using ec: ExecutionContext) extends BayesianNetworkService:

  private val logger: Logger = Logger(this.getClass)

  private def pedigreeScenarioService: PedigreeScenarioService    = pedigreeScenarioServiceProvider.get()
  private def pedigreeService: PedigreeService                    = pedigreeServiceProvider.get()
  private def pedigreeGenotypificationService: PedigreeGenotypificationService = pedigreeGenotypificationServiceProvider.get()

  private val calculation = "pedigree"

  override def getFrequencyTable(name: String): Future[(String, BayesianNetwork.FrequencyTable)] =
    populationBaseFrequencyService.getByName(name).map { baseOpt =>
      val mapAlleleToFrq = (e: (String, Seq[PopulationSampleFrequency])) =>
        e._1 -> e._2.map(av => av.allele -> av.frequency.toDouble).toMap
      val baseFreqMap = baseOpt.get.base.groupBy(_.marker).map(mapAlleleToFrq)
      (name, baseFreqMap)
    }

  override def getFrequencyTables(names: Seq[String]): Future[Map[String, BayesianNetwork.FrequencyTable]] =
    Future.sequence(names.map(getFrequencyTable)).map(_.toMap)

  override def getLinkage(): Future[BayesianNetwork.Linkage] =
    Future.successful(Map.empty)

  override def getGenotypification(
    pedigree: PedigreeGenogram,
    profiles: Array[Profile],
    frequencyTable: BayesianNetwork.FrequencyTable,
    analysisType: AnalysisType,
    linkage: BayesianNetwork.Linkage,
    mutationModelType: Option[Long],
    mutationModelData: Option[List[MutationModelData]],
    n: Map[String, List[Double]]
  ): Future[Array[PlainCPT]] =
    locusService.locusRangeMap().map { locusRangeMap =>
      val normalizedFrequencyTable = BayesianNetwork.getNormalizedFrequencyTable(frequencyTable)
      BayesianNetwork.getGenotypification(
        profiles,
        pedigree.genogram.toArray,
        normalizedFrequencyTable,
        analysisType,
        linkage,
        None,
        verbose = true,
        locusRangeMap,
        mutationModelType,
        mutationModelData,
        n
      )
    }

  override def calculateProbability(scenario: PedigreeScenario): Future[Double] =
    val codes = scenario.genogram.flatMap(_.globalCode).toList
    val lrFuture = (for
      _             <- pedigreeScenarioService.updateScenario(scenario.copy(isProcessing = true))
      profiles      <- profileRepository.findByCodes(codes)
      analysisType  <- calculationTypeService.getAnalysisTypeByCalculation(calculation)
      freqTable     <- getFrequencyTable(scenario.frequencyTable)
      linkage       <- getLinkage()
      mutationModel <-
        if scenario.mutationModelId.isDefined then mutationRepository.getMutationModel(scenario.mutationModelId)
        else Future.successful(None)
      mutModelData  <- mutationService.getMutationModelData(mutationModel, profileRepository.getProfilesMarkers(profiles.toArray))
      n             <- mutationService.getAllPossibleAllelesByLocus()
      listLocus     <- locusService.list()
    yield
      val locusRanges: NewMatchingResult.AlleleMatchRange = listLocus.map { locus =>
        locus.id -> AleleRange(locus.minAlleleValue.getOrElse(0), locus.maxAlleleValue.getOrElse(99))
      }.toMap
      val mutationModelType: Option[Long] = mutationModel.map(_.mutationType)
      pedigreeGenotypificationService.calculateProbabilityActor(
        CalculateProbabilityScenarioPed(
          profiles.toArray,
          scenario.genogram.toArray,
          freqTable._2,
          analysisType,
          linkage,
          verbose = false,
          mutationModelType,
          mutModelData,
          n,
          locusRanges
        )
      )
    ).flatMap(identity)

    lrFuture.foreach { lr =>
      pedigreeScenarioService.updateScenario(scenario.copy(lr = Some(lr.toString), isProcessing = false))
        .onComplete {
          case scala.util.Success(_) =>
            pedigreeService.getPedigree(scenario.pedigreeId).foreach { pedigree =>
              notificationService.push(pedigree.get.pedigreeMetaData.assignee,
                PedigreeLRInfo(scenario.pedigreeId, pedigree.get.pedigreeMetaData.courtCaseId, scenario.name))
              userService.sendNotifToAllSuperUsers(
                PedigreeLRInfo(scenario.pedigreeId, pedigree.get.pedigreeMetaData.courtCaseId, scenario.name),
                Seq(pedigree.get.pedigreeMetaData.assignee))
            }
          case scala.util.Failure(e) =>
            logger.error(s"updateScenario (LR=$lr, isProcessing=false) failed for scenario=${scenario._id.id} — scenario may be stuck in isProcessing=true", e)
        }
    }

    lrFuture.failed.foreach { lrErr =>
      logger.error(s"calculateProbability failed for scenario=${scenario._id.id} pedigree=${scenario.pedigreeId}", lrErr)
      pedigreeScenarioService.updateScenario(scenario.copy(isProcessing = false))
        .failed.foreach { e =>
          logger.error(s"updateScenario (isProcessing=false) after lrFuture failure ALSO failed for scenario=${scenario._id.id} — scenario stuck in isProcessing=true", e)
        }
    }
    lrFuture
