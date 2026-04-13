package scenarios

import matching.{MongoId, NewMatchingResult}
import probability.LRResult
import types.SampleCode

import scala.concurrent.Future

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
