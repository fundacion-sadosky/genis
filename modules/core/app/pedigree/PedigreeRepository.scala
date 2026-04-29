package pedigree

import com.mongodb.client.model.{Filters, ReplaceOptions, Updates}
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import play.api.libs.json.Json
import scenarios.ScenarioStatus

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

// ---------------------------------------------------------------------------
// MongoPedigreeRepository
// ---------------------------------------------------------------------------

@jakarta.inject.Singleton
class MongoPedigreeRepository @jakarta.inject.Inject() (
  database: MongoDatabase
)(implicit ec: ExecutionContext) extends PedigreeRepository:

  private def col: MongoCollection[Document] = database.getCollection("pedigrees")

  private def parseGenogram(doc: Document): PedigreeGenogram =
    Json.parse(doc.toJson()).as[PedigreeGenogram]

  override def addGenogram(genogram: PedigreeGenogram): Future[Either[String, Long]] = Future {
    val doc  = Document.parse(Json.toJson(genogram)(PedigreeGenogram.pedigreeWrites).toString())
    val opts = new ReplaceOptions().upsert(true)
    col.replaceOne(Filters.eq("_id", genogram._id.toString), doc, opts)
    Right(genogram._id)
  }.recover { case e => Left(e.getMessage) }

  override def get(id: Long): Future[Option[PedigreeGenogram]] = Future {
    Option(col.find(Filters.eq("_id", id.toString)).first()).map(parseGenogram)
  }

  override def getActivePedigreesByCaseType(caseType: String): Future[Seq[PedigreeGenogram]] = Future {
    col
      .find(Filters.and(Filters.eq("caseType", caseType), Filters.eq("status", PedigreeStatus.Active.toString)))
      .into(new java.util.ArrayList[Document]()).asScala.toSeq
      .map(parseGenogram)
  }

  override def changeStatus(courtCaseId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]] = Future {
    val filter = Filters.eq("_id", courtCaseId.toString)
    val update =
      if status == PedigreeStatus.UnderConstruction then
        Updates.combine(Updates.set("status", status.toString), Updates.set("processed", false))
      else
        Updates.set("status", status.toString)
    col.updateOne(filter, update)
    Right(courtCaseId)
  }.recover { case e => Left(e.getMessage) }

  override def findByProfile(profile: String): Future[Seq[Long]] = Future {
    col
      .find(Filters.elemMatch("genogram", Filters.eq("globalCode", profile)))
      .into(new java.util.ArrayList[Document]()).asScala.toSeq
      .flatMap(doc => Option(doc.getString("_id")).map(_.toLong))
  }

  override def setProcessed(pedigreeId: Long): Future[Either[String, Long]] = Future {
    col.updateOne(Filters.eq("_id", pedigreeId.toString), Updates.set("processed", true))
    Right(pedigreeId)
  }.recover { case e => Left(e.getMessage) }

  override def getUnprocessed(): Future[Seq[Long]] = Future {
    col
      .find(Filters.and(Filters.eq("processed", false), Filters.eq("status", PedigreeStatus.Active.toString)))
      .into(new java.util.ArrayList[Document]()).asScala.toSeq
      .flatMap(doc => Option(doc.getString("_id")).map(_.toLong))
  }

  override def deleteFisicalPedigree(pedigreeId: Long): Future[Either[String, Long]] = Future {
    col.findOneAndDelete(Filters.eq("_id", pedigreeId.toString))
    Right(pedigreeId)
  }.recover { case e => Left(e.getMessage) }

  override def countByProfile(globalCode: String): Future[Int] = Future {
    val filter = Filters.and(
      Filters.elemMatch("genogram", Filters.eq("globalCode", globalCode)),
      Filters.in("status", PedigreeStatus.Active.toString, PedigreeStatus.UnderConstruction.toString)
    )
    col.find(filter).into(new java.util.ArrayList[Document]()).size()
  }

  override def countByProfileIdPedigrees(globalCode: String, idsPedigrees: Seq[String]): Future[Int] =
    if idsPedigrees.isEmpty then Future.successful(0)
    else Future {
      val filter = Filters.and(
        Filters.elemMatch("genogram", Filters.eq("globalCode", globalCode)),
        Filters.in("_id", idsPedigrees*),
        Filters.in("status", PedigreeStatus.Active.toString, PedigreeStatus.UnderConstruction.toString)
      )
      col.find(filter).into(new java.util.ArrayList[Document]()).size()
    }

  override def getPedigreeByCourtCaseId(courtCaseId: Long): Future[List[PedigreeGenogram]] = Future {
    col
      .find(Filters.eq("idCourtCase", courtCaseId.toString))
      .into(new java.util.ArrayList[Document]()).asScala.toList
      .map(parseGenogram)
  }
