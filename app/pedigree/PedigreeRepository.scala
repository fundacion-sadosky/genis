package pedigree

import matching.MatchResult
import play.api.Play.current
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.json.collection.{JSONCollection, _}
import reactivemongo.bson.{BSONDocument, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import play.modules.reactivemongo.json._
import reactivemongo.api.Cursor

import scala.concurrent.duration._

abstract class PedigreeRepository {
  def addGenogram(genogram: PedigreeGenogram): Future[Either[String, Long]]
  def get(pedigreeId: Long): Future[Option[PedigreeGenogram]]
  def changeStatus(courtCaseId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]]
  def findByProfile(profile: String): Future[Seq[Long]]
  def setProcessed(pedigreeId: Long): Future[Either[String, Long]]
  def getUnprocessed(): Future[Seq[Long]]
  def deleteFisicalPedigree(pedigreeId : Long) : Future[Either[String, Long]]
  def countByProfile(globalCode: String): Future[Int]
  def countByProfileIdPedigrees(globalCode: String,idsPedigrees: Seq[String]): Future[Int]
  def getActivePedigreesByCaseType(caseType: String): Future[Seq[PedigreeGenogram]]

}

class MongoPedigreeRepository extends PedigreeRepository {
  private def pedigrees = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("pedigrees")), Duration(10, SECONDS))

  override def addGenogram(genogram: PedigreeGenogram): Future[Either[String, Long]] = {
    val query = Json.obj("_id" -> genogram._id.toString)
    pedigrees.update(query, genogram, upsert = true) map {
      case result if result.ok => Right(genogram._id)
      case error => Left(error.errmsg.getOrElse("Error"))
    }
  }

  override def get(pedigreeId: Long): Future[Option[PedigreeGenogram]] = {
    val query = Json.obj("_id" -> pedigreeId.toString)
    pedigrees.find(query).one[PedigreeGenogram]
  }
  override def getActivePedigreesByCaseType(caseType: String): Future[Seq[PedigreeGenogram]] = {
    val query = Json.obj("caseType" -> caseType,"status" -> PedigreeStatus.Active.toString)
    pedigrees.find(query).cursor[PedigreeGenogram]().collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[PedigreeGenogram]]())
  }

  override def changeStatus(courtCaseId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]] = {
    val set: JsObject = if (status == PedigreeStatus.UnderConstruction) {
      Json.obj("$set" -> Json.obj("status" -> status.toString, "processed" -> false))
    } else {
      Json.obj("$set" -> Json.obj("status" -> status.toString))
    }

    pedigrees.update(Json.obj("_id" -> courtCaseId.toString), set) map {
      case result if result.ok => Right(courtCaseId)
      case error => Left(error.errmsg.getOrElse("Error"))
    }
  }

  override def findByProfile(profile: String): Future[Seq[Long]] = {
    val query = Json.obj("genogram.globalCode" -> profile)
    pedigrees.find(query).cursor[PedigreeGenogram]().collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[PedigreeGenogram]]()) map {
      pedigrees => pedigrees.map(_._id)
    }
  }

  override def setProcessed(pedigreeId: Long): Future[Either[String, Long]] = {
    val set: JsObject = Json.obj("$set" -> Json.obj("processed" -> true))
    pedigrees.update(Json.obj("_id" -> pedigreeId.toString), set) map {
      case result if result.ok => Right(pedigreeId)
      case error => Left(error.errmsg.getOrElse("Error"))
    }
  }

  override def getUnprocessed() : Future[Seq[Long]] = {
    val query = Json.obj("processed" -> false, "status" -> PedigreeStatus.Active.toString)
    val projection = Json.obj("_id" -> 1)

    val cursor = pedigrees
      .find(query, projection)
      .cursor[BSONDocument]()

    cursor.collect[List](Int.MaxValue, Cursor.FailOnError[List[BSONDocument]]()) map { list =>
      list map {
        element => element.getAs[String]("_id").get.toLong
      }
    }
  }

  override def deleteFisicalPedigree(pedigreeId : Long) : Future[Either[String, Long]] = Future {
    val query = Json.obj("_id" -> pedigreeId.toString)
    pedigrees.findAndRemove(query)
    Right(pedigreeId)
  }

  override def countByProfileIdPedigrees(globalCode: String,idsPedigrees: Seq[String]): Future[Int] = {
    if(idsPedigrees.isEmpty){
      Future.successful(0)
    }else{
    val query = Json.obj("genogram" -> Json.obj("$elemMatch" -> Json.obj("globalCode" -> globalCode)),"_id" -> Json.obj("$in" -> idsPedigrees),
      "status" ->  Json.obj("$in" -> Seq(PedigreeStatus.Active.toString,
        PedigreeStatus.UnderConstruction.toString)))
    pedigrees.find(query)
      .cursor[PedigreeGenogram]()
      .collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[PedigreeGenogram]]()).map(_.size)
    }
  }
  override def countByProfile(globalCode: String): Future[Int] = {
    val query = Json.obj("genogram" -> Json.obj("$elemMatch" -> Json.obj("globalCode" -> globalCode)),
      "status" ->  Json.obj("$in" -> Seq(PedigreeStatus.Active.toString,
        PedigreeStatus.UnderConstruction.toString)))
    pedigrees.find(query)
      .cursor[PedigreeGenogram]()
      .collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[PedigreeGenogram]]()).map(_.size)
  }
}