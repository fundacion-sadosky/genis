package pedigree

import com.mongodb.client.model.{Filters, ReplaceOptions, Updates}
import com.mongodb.client.{MongoCollection, MongoDatabase}
import matching.MongoId
import org.bson.Document
import org.bson.types.ObjectId
import play.api.libs.json.Json
import scenarios.ScenarioStatus

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

// ---------------------------------------------------------------------------
// PedigreeScenarioRepository trait
// ---------------------------------------------------------------------------

trait PedigreeScenarioRepository:
  def create(pedigreeScenario: PedigreeScenario): Future[Either[String, MongoId]]
  def update(pedigreeScenario: PedigreeScenario): Future[Either[String, MongoId]]
  def get(id: MongoId): Future[Option[PedigreeScenario]]
  def getByPedigree(pedigreeId: Long): Future[Seq[PedigreeScenario]]
  def changeStatus(id: MongoId, status: ScenarioStatus.Value): Future[Either[String, MongoId]]
  def deleteAll(pedigreeId: Long): Future[Either[String, Long]]
  def countByProfile(globalCode: String): Future[Int]

@jakarta.inject.Singleton
class PedigreeScenarioRepositoryStub extends PedigreeScenarioRepository:
  override def create(s: PedigreeScenario): Future[Either[String, MongoId]]                              = Future.successful(Right(s._id))
  override def update(s: PedigreeScenario): Future[Either[String, MongoId]]                              = Future.successful(Right(s._id))
  override def get(id: MongoId): Future[Option[PedigreeScenario]]                                        = Future.successful(None)
  override def getByPedigree(pedigreeId: Long): Future[Seq[PedigreeScenario]]                            = Future.successful(Seq.empty)
  override def changeStatus(id: MongoId, status: ScenarioStatus.Value): Future[Either[String, MongoId]] = Future.successful(Right(id))
  override def deleteAll(pedigreeId: Long): Future[Either[String, Long]]                                 = Future.successful(Right(pedigreeId))
  override def countByProfile(globalCode: String): Future[Int]                                          = Future.successful(0)

// ---------------------------------------------------------------------------
// MongoDB sync driver implementation
// ---------------------------------------------------------------------------

@jakarta.inject.Singleton
class MongoPedigreeScenarioRepository @jakarta.inject.Inject() (
  database: MongoDatabase
)(implicit ec: ExecutionContext) extends PedigreeScenarioRepository:

  private def col: MongoCollection[Document] = database.getCollection("pedigreeScenarios")

  private def parseScenario(doc: Document): PedigreeScenario =
    Json.parse(doc.toJson()).as[PedigreeScenario]

  override def create(scenario: PedigreeScenario): Future[Either[String, MongoId]] = Future {
    val doc = Document.parse(Json.toJson(scenario).toString())
    // ensure _id is stored as ObjectId
    doc.put("_id", new ObjectId(scenario._id.id))
    col.insertOne(doc)
    Right(scenario._id)
  }.recover { case e => Left(e.getMessage) }

  override def update(scenario: PedigreeScenario): Future[Either[String, MongoId]] = Future {
    val doc  = Document.parse(Json.toJson(scenario).toString())
    doc.put("_id", new ObjectId(scenario._id.id))
    val opts = new ReplaceOptions().upsert(true)
    col.replaceOne(Filters.eq("_id", new ObjectId(scenario._id.id)), doc, opts)
    Right(scenario._id)
  }.recover { case e => Left(e.getMessage) }

  override def get(id: MongoId): Future[Option[PedigreeScenario]] = Future {
    Option(col.find(Filters.eq("_id", new ObjectId(id.id))).first()).map(parseScenario)
  }

  override def getByPedigree(pedigreeId: Long): Future[Seq[PedigreeScenario]] = Future {
    val filter = Filters.and(
      Filters.eq("pedigreeId", pedigreeId.toString),
      Filters.ne("status", ScenarioStatus.Deleted.toString)
    )
    col.find(filter).into(new java.util.ArrayList[Document]()).asScala.toSeq.map(parseScenario)
  }

  override def changeStatus(id: MongoId, status: ScenarioStatus.Value): Future[Either[String, MongoId]] = Future {
    col.updateOne(Filters.eq("_id", new ObjectId(id.id)), Updates.set("status", status.toString))
    Right(id)
  }.recover { case e => Left(e.getMessage) }

  override def deleteAll(pedigreeId: Long): Future[Either[String, Long]] = Future {
    col.deleteMany(Filters.eq("pedigreeId", pedigreeId.toString))
    Right(pedigreeId)
  }.recover { case e => Left(e.getMessage) }

  override def countByProfile(globalCode: String): Future[Int] = Future {
    val filter = Filters.and(
      Filters.elemMatch("genogram", Filters.eq("globalCode", globalCode)),
      Filters.in("status", ScenarioStatus.Pending.toString)
    )
    col.find(filter).into(new java.util.ArrayList[Document]()).size()
  }
