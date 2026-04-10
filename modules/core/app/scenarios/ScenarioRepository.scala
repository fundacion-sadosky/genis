package scenarios

import matching.MongoId
import types.SampleCode

import scala.concurrent.Future

trait ScenarioRepository:
  def get(userId: String, id: MongoId, isSuperUser: Boolean): Future[Option[Scenario]]
  def get(userId: String, scenarioSearch: ScenarioSearch, isSuperUser: Boolean): Future[Seq[Scenario]]
  def add(scenario: Scenario): Future[Either[String, String]]
  def delete(userId: String, id: MongoId, isSuperUser: Boolean): Future[Either[String, String]]
  def validate(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]]
  def update(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]]
  def getByProfile(sampleCode: SampleCode): Future[Seq[Scenario]]
  def getByMatch(firingCode: SampleCode, matchingCode: SampleCode, userId: String, isSuperUser: Boolean): Future[Seq[Scenario]]

@javax.inject.Singleton
class ScenarioRepositoryStub extends ScenarioRepository:
  override def get(userId: String, id: MongoId, isSuperUser: Boolean): Future[Option[Scenario]] =
    Future.successful(None)
  override def get(userId: String, scenarioSearch: ScenarioSearch, isSuperUser: Boolean): Future[Seq[Scenario]] =
    Future.successful(Seq.empty)
  override def add(scenario: Scenario): Future[Either[String, String]] =
    Future.successful(Right(scenario._id.id))
  override def delete(userId: String, id: MongoId, isSuperUser: Boolean): Future[Either[String, String]] =
    Future.successful(Right(id.id))
  override def validate(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]] =
    Future.successful(Right(scenario._id.id))
  override def update(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]] =
    Future.successful(Right(scenario._id.id))
  override def getByProfile(sampleCode: SampleCode): Future[Seq[Scenario]] =
    Future.successful(Seq.empty)
  override def getByMatch(firingCode: SampleCode, matchingCode: SampleCode, userId: String, isSuperUser: Boolean): Future[Seq[Scenario]] =
    Future.successful(Seq.empty)
