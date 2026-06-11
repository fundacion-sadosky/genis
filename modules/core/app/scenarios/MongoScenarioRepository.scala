package scenarios

import com.mongodb.client.model.{Filters, ReplaceOptions, Sorts}
import com.mongodb.client.{MongoCollection, MongoDatabase}
import matching.MongoId
import org.bson.Document
import org.bson.types.ObjectId
import play.api.libs.json.Json
import types.SampleCode

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

@Singleton
class MongoScenarioRepository @Inject()(
  database: MongoDatabase
)(using ec: ExecutionContext) extends ScenarioRepository:

  private def scenarios: MongoCollection[Document] = database.getCollection("scenarios")

  private def docToScenario(doc: Document): Scenario =
    Json.parse(doc.toJson()).as[Scenario]

  private def scenarioToDoc(scenario: Scenario): Document =
    Document.parse(Json.toJson(scenario).toString())

  override def get(userId: String, id: MongoId, isSuperUser: Boolean): Future[Option[Scenario]] = Future {
    val filter =
      if isSuperUser then Filters.eq("_id", new ObjectId(id.id))
      else Filters.and(Filters.eq("_id", new ObjectId(id.id)), Filters.eq("geneticist", userId))
    Option(scenarios.find(filter).first()).map(docToScenario)
  }

  override def get(userId: String, search: ScenarioSearch, isSuperUser: Boolean): Future[Seq[Scenario]] = Future {
    val filters = scala.collection.mutable.Buffer[org.bson.conversions.Bson](
      Filters.eq("calculationScenario.sample", search.profile.text),
      Filters.ne("state", ScenarioStatus.Deleted.toString)
    )
    if !isSuperUser then filters += Filters.eq("geneticist", userId)
    search.name.foreach(n => filters += Filters.regex("name", s".*$n.*", "i"))
    search.hourFrom.foreach(d => filters += Filters.gte("date", d))
    search.hourUntil.foreach(d => filters += Filters.lte("date", d))
    search.state match
      case Some(true)  => filters += Filters.eq("state", ScenarioStatus.Validated.toString)
      case Some(false) => filters += Filters.eq("state", ScenarioStatus.Pending.toString)
      case None        => ()

    val sort = search.sortField match
      case "name"  => if search.ascending then Sorts.ascending("name")  else Sorts.descending("name")
      case "state" => if search.ascending then Sorts.ascending("state") else Sorts.descending("state")
      case _       => if search.ascending then Sorts.ascending("date")  else Sorts.descending("date")

    scenarios.find(Filters.and(filters.toSeq*)).sort(sort)
      .into(new java.util.ArrayList[Document]()).asScala.map(docToScenario).toSeq
  }

  override def add(scenario: Scenario): Future[Either[String, String]] = Future {
    try
      scenarios.insertOne(scenarioToDoc(scenario))
      Right(scenario._id.id)
    catch case e: Exception => Left(e.getMessage)
  }

  override def delete(userId: String, id: MongoId, isSuperUser: Boolean): Future[Either[String, String]] = Future {
    val filter =
      if isSuperUser then Filters.eq("_id", new ObjectId(id.id))
      else Filters.and(Filters.eq("_id", new ObjectId(id.id)), Filters.eq("geneticist", userId))
    val update = new Document("$set", new Document("state", ScenarioStatus.Deleted.toString))
    val result = scenarios.updateOne(filter, update)
    if result.getMatchedCount == 0 then Left("error.E0645")
    else Right(id.id)
  }

  override def validate(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]] = Future {
    val updated = scenario.copy(state = ScenarioStatus.Validated)
    val filter =
      if isSuperUser then Filters.eq("_id", new ObjectId(scenario._id.id))
      else Filters.and(Filters.eq("_id", new ObjectId(scenario._id.id)), Filters.eq("geneticist", userId))
    val result = scenarios.replaceOne(filter, scenarioToDoc(updated), ReplaceOptions().upsert(true))
    if result.getMatchedCount == 0 && result.getUpsertedId == null then Left("error.E0645")
    else Right(scenario._id.id)
  }

  override def update(userId: String, scenario: Scenario, isSuperUser: Boolean): Future[Either[String, String]] = Future {
    val filter =
      if isSuperUser then Filters.eq("_id", new ObjectId(scenario._id.id))
      else Filters.and(Filters.eq("_id", new ObjectId(scenario._id.id)), Filters.eq("geneticist", userId))
    val result = scenarios.replaceOne(filter, scenarioToDoc(scenario))
    if result.getMatchedCount == 0 then Left("error.E0645")
    else Right(scenario._id.id)
  }

  override def getByProfile(sampleCode: SampleCode): Future[Seq[Scenario]] = Future {
    scenarios.find(Filters.eq("calculationScenario.sample", sampleCode.text))
      .into(new java.util.ArrayList[Document]()).asScala.map(docToScenario).toSeq
  }

  override def getByMatch(firingCode: SampleCode, matchingCode: SampleCode, userId: String, isSuperUser: Boolean): Future[Seq[Scenario]] = Future {
    val baseFilter = Filters.and(
      Filters.eq("calculationScenario.sample", firingCode.text),
      Filters.ne("state", ScenarioStatus.Deleted.toString)
    )
    val filter =
      if isSuperUser then baseFilter
      else Filters.and(baseFilter, Filters.eq("geneticist", userId))
    scenarios.find(filter).into(new java.util.ArrayList[Document]()).asScala.map(docToScenario).toSeq
  }
