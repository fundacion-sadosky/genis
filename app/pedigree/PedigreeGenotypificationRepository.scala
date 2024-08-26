package pedigree

import scala.concurrent.duration.{Duration, _}
import play.api.Play.current
import play.api.libs.json.Json
import play.modules.reactivemongo.json.collection.{JSONCollection, _}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import play.modules.reactivemongo.json._

abstract class PedigreeGenotypificationRepository {
  def upsertGenotypification(pedigreeGenotypification: PedigreeGenotypification): Future[Either[String, Long]]
  def doesntHaveGenotification(pedigreeId: Long) : Future[Boolean]
}

class MongoPedigreeGenotypificationRepository extends PedigreeGenotypificationRepository  {
  private def pedigreeGenotypificationCollection = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("pedigreeGenotypification")), Duration(10, SECONDS))

  override def upsertGenotypification(pedigreeGenotypification: PedigreeGenotypification): Future[Either[String, Long]] = {
    val query = Json.obj("_id" -> pedigreeGenotypification._id.toString)
    pedigreeGenotypificationCollection.update(query, pedigreeGenotypification, upsert = true) map {
      case result if result.ok => Right(pedigreeGenotypification._id)
      case error => Left(error.errmsg.getOrElse("Error"))
    }
  }

  override def doesntHaveGenotification(pedigreeId: Long) : Future[Boolean] = {
    val query = Json.obj("_id" -> pedigreeId.toString)

    pedigreeGenotypificationCollection.count(Some(query)).map { n => n == 0 }

  }

}
