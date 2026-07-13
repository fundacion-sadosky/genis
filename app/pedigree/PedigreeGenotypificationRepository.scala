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
  def get(pedigreeId: Long): Future[Option[PedigreeGenotypification]]
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

  // Lectura directa via ReactiveMongo (JSON), en vez de leer esta misma
  // coleccion a traves de un RDD de Spark (MongoSpark + doc.toJson()):
  // para documentos con matrices grandes (CPTs de cientos de filas, ver
  // modelo mutacional Stepwise) esa lectura via Spark devolvia el array
  // "matrix" vacio, aun cuando el documento en Mongo estaba completo
  // (verificado leyendo el mismo documento directamente y via el mismo
  // pipeline de agregacion sin Spark). Como esta consulta trae un unico
  // documento por _id, no hay ninguna ventaja en usar Spark para leerla.
  override def get(pedigreeId: Long): Future[Option[PedigreeGenotypification]] = {
    val query = Json.obj("_id" -> pedigreeId.toString)
    pedigreeGenotypificationCollection.find(query).one[PedigreeGenotypification]
  }

}
