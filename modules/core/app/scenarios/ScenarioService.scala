package scenarios

import javax.inject.{Inject, Named, Singleton}
import matching.{MongoId, NewMatchingResult}
import probability.{CalculationTypeService, FullCalculationScenario, FullHypothesis, LRMixCalculator, LRResult, PValueCalculator}
import stats.PopulationBaseFrequencyService
import types.SampleCode

import scala.concurrent.{ExecutionContext, Future}

trait ScenarioService:
  def getLRMix(scenario: CalculationScenario, allelesRanges: Option[NewMatchingResult.AlleleMatchRange] = None): Future[Option[LRResult]]
  def findExistingMatches(scenario: Scenario): Future[Seq[ScenarioOption]]
  def findMatchesForScenario(userId: String, globalCode: SampleCode, isSuperUser: Boolean): Future[Seq[ScenarioOption]]
  def findMatchesForRestrainedScenario(userId: String, firingCode: SampleCode, matchingCode: SampleCode, isSuperUser: Boolean): Future[Seq[ScenarioOption]]
  def search(userId: String, scenarioSearch: ScenarioSearch, isSuperUser: Boolean): Future[Seq[Scenario]]
  def add(scenario: Scenario): Future[Either[String, String]]
  def delete(userId: String, id: MongoId, isSuperUser: Boolean): Future[Either[String, String]]
  def get(userId: String, id: MongoId, isSuperUser: Boolean): Future[Option[Scenario]]
  def validate(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]]
  def update(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]]
  def getNCorrection(request: NCorrectionRequest): Future[Either[String, NCorrectionResponse]]

@javax.inject.Singleton
class ScenarioServiceStub extends ScenarioService:
  override def getLRMix(scenario: CalculationScenario, allelesRanges: Option[NewMatchingResult.AlleleMatchRange]): Future[Option[LRResult]] =
    Future.successful(None)
  override def findExistingMatches(scenario: Scenario): Future[Seq[ScenarioOption]] =
    Future.successful(Seq.empty)
  override def findMatchesForScenario(userId: String, globalCode: SampleCode, isSuperUser: Boolean): Future[Seq[ScenarioOption]] =
    Future.successful(Seq.empty)
  override def findMatchesForRestrainedScenario(userId: String, firingCode: SampleCode, matchingCode: SampleCode, isSuperUser: Boolean): Future[Seq[ScenarioOption]] =
    Future.successful(Seq.empty)
  override def search(userId: String, scenarioSearch: ScenarioSearch, isSuperUser: Boolean): Future[Seq[Scenario]] =
    Future.successful(Seq.empty)
  override def add(scenario: Scenario): Future[Either[String, String]] =
    Future.successful(Right(scenario._id.id))
  override def delete(userId: String, id: MongoId, isSuperUser: Boolean): Future[Either[String, String]] =
    Future.successful(Right(id.id))
  override def get(userId: String, id: MongoId, isSuperUser: Boolean): Future[Option[Scenario]] =
    Future.successful(None)
  override def validate(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]] =
    Future.successful(Right(scenario._id.id))
  override def update(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]] =
    Future.successful(Right(scenario._id.id))
  override def getNCorrection(request: NCorrectionRequest): Future[Either[String, NCorrectionResponse]] =
    Future.successful(Left("Not implemented"))

@Singleton
class ScenarioServiceImpl @Inject()(
  populationBaseFrequencyService: PopulationBaseFrequencyService,
  calculationTypeService: CalculationTypeService,
  @Named("lrmix-context") lrmixEc: ExecutionContext
)(using ec: ExecutionContext) extends ScenarioServiceStub:

  override def getLRMix(
    scenario: CalculationScenario,
    allelesRanges: Option[NewMatchingResult.AlleleMatchRange] = None
  ): Future[Option[LRResult]] =
    val frequencyTableFuture = populationBaseFrequencyService
      .getByName(scenario.stats.frequencyTable)
      .map(maybeFrequencyTable => PValueCalculator.parseFrequencyTable(maybeFrequencyTable.get))
    val fullScenarioFuture = convertFullScenario(scenario)
    for
      frequencyTable <- frequencyTableFuture
      fullScenario   <- fullScenarioFuture
      lr             <- LRMixCalculator.calculateLRMix(fullScenario, frequencyTable, allelesRanges)(using lrmixEc)
    yield Some(lr)

  private def convertFullScenario(scenario: CalculationScenario): Future[FullCalculationScenario] =
    val calculation    = LRMixCalculator.name
    val profiles       = scenario.profiles.getOrElse(Nil)
    val sampleFut      = calculationTypeService.filterCodes(List(scenario.sample), calculation, profiles).map(_.head)
    val selectedPFut   = calculationTypeService.filterCodes(scenario.prosecutor.selected, calculation, profiles)
    val unselectedPFut = calculationTypeService.filterCodes(scenario.prosecutor.unselected, calculation, profiles)
    val selectedDFut   = calculationTypeService.filterCodes(scenario.defense.selected, calculation, profiles)
    val unselectedDFut = calculationTypeService.filterCodes(scenario.defense.unselected, calculation, profiles)
    for
      sample      <- sampleFut
      selectedP   <- selectedPFut
      unselectedP <- unselectedPFut
      selectedD   <- selectedDFut
      unselectedD <- unselectedDFut
    yield FullCalculationScenario(
      sample,
      FullHypothesis(selectedP.toArray, unselectedP.toArray, scenario.prosecutor.unknowns, scenario.prosecutor.dropOut),
      FullHypothesis(selectedD.toArray, unselectedD.toArray, scenario.defense.unknowns, scenario.defense.dropOut),
      scenario.stats
    )
