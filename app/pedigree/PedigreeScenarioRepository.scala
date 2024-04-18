package pedigree

import play.api.Play.current
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.{JSONCollection, _}
import scenarios.ScenarioStatus
import types.MongoId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import play.modules.reactivemongo.json._
import reactivemongo.api.Cursor
import reactivemongo.bson.{BSONArray, BSONDocument, BSONDouble, BSONObjectID}

import scala.concurrent.duration._

abstract class PedigreeScenarioRepository {
  def create(pedigreeScenario: PedigreeScenario): Future[Either[String,MongoId]]
  def update(pedigreeScenario: PedigreeScenario): Future[Either[String,MongoId]]
  def get(id: MongoId): Future[Option[PedigreeScenario]]
  def getByPedigree(pedigreeId: Long): Future[Seq[PedigreeScenario]]
  def changeStatus(id: MongoId, status: ScenarioStatus.Value): Future[Either[String,MongoId]]
  def deleteAll(pedigreeId: Long): Future[Either[String,Long]]
  def countByProfile(globalCode: String): Future[Int]
}

class MongoPedigreeScenarioRepository extends PedigreeScenarioRepository {
  private def pedigreeScenarios = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("pedigreeScenarios")), Duration(10, SECONDS))
  private def toBSONDoc(genogram:scala.Seq[Individual])= {
    genogram.map(individual => {
      var result = BSONDocument("alias"-> individual.alias.text,
        "sex"-> individual.sex.toString,
        "unknown" -> individual.unknown)

      if(individual.idFather.isDefined){
        result = result.++("idFather"->individual.idFather.get.text)
      }
      if(individual.idMother.isDefined){
        result = result.++("idMother"->individual.idMother.get.text)
      }
      if(individual.globalCode.isDefined){
        result = result.++("globalCode"->individual.globalCode.get.text)
      }
      if(individual.isReference.isDefined){
        result = result.++("isReference"->individual.isReference.get)
      }
      result
    })
  }
  private def convertToBSON(scenario: PedigreeScenario) = {
    var scenarioBSON = BSONDocument("_id"->BSONObjectID(scenario._id.id),
      "pedigreeId"->scenario.pedigreeId.toString,
      "name"->scenario.name,
      "description"->scenario.description,
      "genogram"->toBSONDoc(scenario.genogram),
      "status"->scenario.status.toString,
      "frequencyTable"->scenario.frequencyTable,
      "isProcessing"-> scenario.isProcessing
    )
    if(scenario.lr.isDefined){
      scenarioBSON = scenarioBSON.++("lr"->scenario.lr.get)
    }
    if(scenario.validationComment.isDefined){
      scenarioBSON = scenarioBSON.++("validationComment"->scenario.validationComment.get)
    }
    if(scenario.matchId.isDefined){
      scenarioBSON = scenarioBSON.++("matchId"->scenario.matchId.get)
    }
    if(scenario.mutationModelId.isDefined){
      scenarioBSON = scenarioBSON.++("mutationModelId"->scenario.mutationModelId.get.toString)
    }
    scenarioBSON
  }
  override def create(scenario: PedigreeScenario): Future[Either[String,MongoId]] = {

    pedigreeScenarios.insert(convertToBSON(scenario)) map { result =>
      Right(scenario._id)
    } recover {
      case error => Left(error.getMessage)
    }
  }

  override def update(scenario: PedigreeScenario): Future[Either[String, MongoId]] = {
    val query = Json.obj("_id" -> scenario._id)

    val modifier = Json.obj("$set" -> convertToBSON(scenario))

    pedigreeScenarios.update(query, modifier, upsert = true).map {
      case result if result.ok => Right(scenario._id)
      case error => Left(error.errmsg.getOrElse("Error"))
    }
  }

  override def getByPedigree(pedigreeId: Long): Future[Seq[PedigreeScenario]] = {
    val query = Json.obj("pedigreeId" -> pedigreeId.toString,
                         "status" -> Json.obj("$ne" -> ScenarioStatus.Deleted.toString))

    pedigreeScenarios
      .find(query)
      .cursor[PedigreeScenario]()
      .collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[PedigreeScenario]]())
  }

  override def changeStatus(id: MongoId, status: ScenarioStatus.Value): Future[Either[String,MongoId]] = {
    val set: JsObject = Json.obj("$set" -> Json.obj("status" -> status))
    val query = Json.obj("_id" -> id)

    pedigreeScenarios.update(query, set).map {
      case result if result.ok => Right(id)
      case error => Left(error.errmsg.getOrElse("Error"))
    }
  }

  override def get(id: MongoId): Future[Option[PedigreeScenario]] = {
    val query = Json.obj("_id" -> id)

    pedigreeScenarios
      .find(query)
      .one[PedigreeScenario]
  }

  override def deleteAll(pedigreeId: Long): Future[Either[String, Long]] = {
    val query = Json.obj("pedigreeId" -> pedigreeId.toString)

    pedigreeScenarios.remove(query).map { result =>
      Right(pedigreeId)
    } recover {
      case error => Left(error.getMessage)
    }
  }

  override def countByProfile(globalCode: String): Future[Int] = {
    val query = Json.obj("genogram" -> Json.obj("$elemMatch" -> Json.obj("globalCode" -> globalCode)),
    "status" -> Json.obj("$in" -> Seq(ScenarioStatus.Pending.toString)))
    pedigreeScenarios.find(query)
      .cursor[PedigreeScenario]()
      .collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[PedigreeScenario]]()).map(_.size)
  }

}