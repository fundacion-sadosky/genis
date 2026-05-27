package pedigree

import com.mongodb.client.model.{Filters, ReplaceOptions}
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import play.api.Logger
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

// ---------------------------------------------------------------------------
// PedigreeGenotypificationRepository
// ---------------------------------------------------------------------------

trait PedigreeGenotypificationRepository:
  def upsertGenotypification(pedigreeGenotypification: PedigreeGenotypification): Future[Either[String, Long]]
  def doesntHaveGenotification(pedigreeId: Long): Future[Boolean]
  def get(pedigreeId: Long): Future[Option[PedigreeGenotypification]]

@jakarta.inject.Singleton
class MongoPedigreeGenotypificationRepository @jakarta.inject.Inject() (
  database: MongoDatabase
)(implicit ec: ExecutionContext) extends PedigreeGenotypificationRepository:

  private val logger: Logger = Logger(this.getClass)

  private def col: MongoCollection[Document] = database.getCollection("pedigreeGenotypification")

  override def upsertGenotypification(pg: PedigreeGenotypification): Future[Either[String, Long]] = Future {
    try
      val doc     = Document.parse(Json.toJson(pg)(PedigreeGenotypification.pedigreeGenotypificationFormat).toString())
      val filter  = Filters.eq("_id", pg._id.toString)
      val options = new ReplaceOptions().upsert(true)
      col.replaceOne(filter, doc, options)
      Right(pg._id)
    catch case e: Exception => logger.error(s"upsertGenotypification failed for pedigree=${pg._id}", e); Left("error.E0630")
  }

  override def doesntHaveGenotification(pedigreeId: Long): Future[Boolean] = Future {
    col.countDocuments(Filters.eq("_id", pedigreeId.toString)) == 0L
  }

  override def get(pedigreeId: Long): Future[Option[PedigreeGenotypification]] = Future {
    Option(col.find(Filters.eq("_id", pedigreeId.toString)).first())
      .map(doc => Json.parse(doc.toJson()).as[PedigreeGenotypification])
  }
