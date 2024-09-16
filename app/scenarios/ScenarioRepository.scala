package scenarios

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import types.{MongoId, SampleCode}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import play.modules.reactivemongo.json._
import reactivemongo.api.{Cursor, DefaultDB, FailoverStrategy, MongoConnection}
import play.api.i18n.{Messages, MessagesApi}

import javax.inject.Inject
import scala.concurrent.duration._

abstract class ScenarioRepository {
  def get(userId: String, id: MongoId, isSuperUser: Boolean) : Future[Option[Scenario]]
  def get(userId: String, scenarioSearch: ScenarioSearch, isSuperUser: Boolean) : Future[Seq[Scenario]]
  def add(scenario: Scenario) : Future[Either[String, String]]
  def delete(userId: String, id: MongoId, isSuperUser: Boolean): Future[Either[String, String]]
  def validate(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]]
  def update(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]]
  def getByProfile(sampleCode: SampleCode): Future[Seq[Scenario]]
  def getByMatch(firingCode: SampleCode, matchingCode: SampleCode, userId: String, isSuperUser: Boolean): Future[Seq[Scenario]]
}

class MongoScenarioRepository  @Inject() (messagesApi: MessagesApi) extends ScenarioRepository  {
  private def scenarios = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("scenarios")), Duration(10, SECONDS))
  //private def scenarios = play.modules.reactivemongo.ReactiveMongoPlugin.db.collection[JSONCollection]("scenarios")

  override def get(userId: String, scenarioSearch: ScenarioSearch, isSuperUser: Boolean): Future[Seq[Scenario]] = {
    val state = if (scenarioSearch.state.isDefined && scenarioSearch.state.get) {
        Json.obj("state" -> ScenarioStatus.Validated)
      } else if (scenarioSearch.state.isDefined && !scenarioSearch.state.get) {
      Json.obj("state" -> ScenarioStatus.Pending)
    } else {
      Json.obj()
    }

    val query = Json.obj(
      "$and" -> Json.arr(
        Json.obj("calculationScenario.sample" -> scenarioSearch.profile),
        if (isSuperUser) Json.obj() else Json.obj("geneticist" -> userId),
        Json.obj("state" -> Json.obj("$ne" -> ScenarioStatus.Deleted)),
        if (scenarioSearch.name.isDefined) Json.obj("name" -> Json.obj("$regex" -> (".*" + scenarioSearch.name.get + ".*"), "$options" -> "i")) else Json.obj(),
        if (scenarioSearch.hourFrom.isDefined) Json.obj("date" -> Json.obj("$gte" -> Json.obj("$date" -> scenarioSearch.hourFrom.get))) else Json.obj(),
        if (scenarioSearch.hourUntil.isDefined) Json.obj("date" -> Json.obj("$lte" -> Json.obj("$date" -> scenarioSearch.hourUntil.get))) else Json.obj(),
        state
      )
    )

    val options = if (scenarioSearch.sortField == "date") Json.obj("date" -> (if (scenarioSearch.ascending) 1 else -1)) else if (scenarioSearch.sortField == "name") Json.obj("name" -> (if (scenarioSearch.ascending) 1 else -1)) else Json.obj("state" -> (if (scenarioSearch.ascending) 1 else -1))

    scenarios
        .find(query)
        .sort(options)
        .cursor[Scenario]()
        .collect[List](Int.MaxValue, Cursor.FailOnError[List[Scenario]]())
  }

  override def add(scenario: Scenario) : Future[Either[String, String]] = {
    scenarios.insert(scenario) map { result =>
      Right(scenario._id.id)
    } recover {
      case error => Left(error.getMessage)
    }
  }

  override def delete(userId: String, id: MongoId, isSuperUser: Boolean): Future[Either[String, String]] = {
    implicit val messages: Messages = messagesApi.preferred(Seq.empty)
    val set: JsObject = Json.obj("$set" -> Json.obj("state" -> ScenarioStatus.Deleted))

    val query = if (isSuperUser){
      Json.obj("_id" -> id)
    }else{
      Json.obj("_id" -> id, "geneticist" -> userId)
    }

    scenarios.update(query, set).map {
      case result if result.ok && result.nModified == 0 => Left(Messages("error.E0645"))
      case result if result.ok => Right(id.id)
      case error => Left(error.errmsg.getOrElse("Error"))
    }
  }

  override def get(userId: String, id: MongoId, isSuperUser: Boolean): Future[Option[Scenario]] = {
    val query = if (isSuperUser){
      Json.obj("_id" -> id)
    }else{
      Json.obj("_id" -> id, "geneticist" -> userId)
    }
    scenarios.find(query).one[Scenario]
  }

  override def validate(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]] = {
    val scenarioToInsertOrUpdate = Scenario(
      scenario._id,
      scenario.name,
      ScenarioStatus.Validated,
      scenario.geneticist,
      scenario.calculationScenario,
      scenario.date,
      scenario.isRestricted,
      scenario.result,
      scenario.description)
    implicit val messages: Messages = messagesApi.preferred(Seq.empty)
    val query = if (isSuperUser){
      Json.obj("_id" -> scenario._id)
    }else{
      Json.obj("_id" -> scenario._id, "geneticist" -> userId)
    }

    scenarios.update(query, scenarioToInsertOrUpdate, upsert = true).map {//
      //      case result if result.ok && result.upserted.isEmpty => Left(Messages("error.E0645"))
      case result if result.ok && result.nModified == 0 && result.upserted.isEmpty=> Left(Messages("error.E0645"))
      case result if result.ok => Right(scenario._id.id)
      case error => Left(error.errmsg.getOrElse("Error"))
    }
  }

  override def update(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]] = {
    implicit val messages: Messages = messagesApi.preferred(Seq.empty)
    val query = if (isSuperUser){
      Json.obj("_id" -> scenario._id)
    }else{
      Json.obj("_id" -> scenario._id, "geneticist" -> userId)
    }

    scenarios.update(query, scenario).map {
      case result if result.ok && result.nModified == 0 => Left(Messages("error.E0645"))
      case result if result.ok => Right(scenario._id.id)
      case error => Left(error.errmsg.getOrElse("Error"))
    }
  }

  private def queryCombination(sample: SampleCode, operator: String, profile: SampleCode) = {
    val statusPending = Json.obj("state" -> ScenarioStatus.Pending)
    val sampleQuery = Json.obj("calculationScenario.sample" -> sample)
    val profileQuery = Json.obj(
      "$or" -> Json.arr(
        Json.obj("calculationScenario.prosecutor.selected" -> profile),
        Json.obj("calculationScenario.prosecutor.unselected" -> profile),
        Json.obj("calculationScenario.defense.selected" -> profile),
        Json.obj("calculationScenario.defense.unselected" -> profile)
      )
    )

    Json.obj(
      "$and" -> Json.arr(
        statusPending,
        Json.obj(operator -> Json.arr(sampleQuery, profileQuery))
      )
    )
  }

  override def getByProfile(sampleCode: SampleCode): Future[Seq[Scenario]] = {
    val query = queryCombination(sampleCode, "$or", sampleCode)

    scenarios
      .find(query)
      .cursor[Scenario]()
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[Scenario]]())
  }

  override def getByMatch(firingCode: SampleCode, matchingCode: SampleCode, userId: String, isSuperUser: Boolean): Future[Seq[Scenario]] = {
    val query = getQueryByMatch(firingCode, matchingCode, userId, isSuperUser)

    scenarios
      .find(query)
      .cursor[Scenario]()
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[Scenario]]())
  }

    private def getQueryByMatch(firingCode: SampleCode, matchingCode: SampleCode, userId: String, isSuperUser: Boolean): JsObject = {
      if (isSuperUser){
        Json.obj(
          "$or" -> Json.arr(
              queryCombination(firingCode, "$and", matchingCode),
              queryCombination(matchingCode, "$and", firingCode)
            ))
      } else{
        Json.obj(
          "$and" -> Json.arr(
            Json.obj("geneticist" -> userId),
            Json.obj("$or" -> Json.arr(
              queryCombination(firingCode, "$and", matchingCode),
              queryCombination(matchingCode, "$and", firingCode)
            ))))
      }
    }
}