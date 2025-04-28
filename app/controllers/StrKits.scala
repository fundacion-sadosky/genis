package controllers

import javax.inject.Inject
import javax.inject.Singleton

import configdata.Group
import kits.{FullStrKit, StrKitService, StrKit}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.Future

@Singleton
class StrKits @Inject() (strKitService: StrKitService) extends Controller {

  def list = Action.async {
    strKitService.list map { kits => Ok(Json.toJson(kits)) }
  }

  def listFull = Action.async {
    strKitService.listFull map { kits => Ok(Json.toJson(kits)) }
  }

  def get(id: String) = Action.async {
    strKitService.get(id) map { kit =>
      kit match {
        case Some(kit) => Ok(Json.toJson(kit))
        case None => NoContent
      }
    }
  }

  def getFull(id: String) = Action.async {
    strKitService.getFull(id) map { kit =>
      kit match {
        case Some(kit) => Ok(Json.toJson(kit))
        case None => NoContent
      }
    }
  }

  def findLociByKit(idKit: String) = Action.async { request =>
    strKitService.findLociByKit(idKit) map { loci =>
      Ok(Json.toJson(loci))
    }
  }

  def add = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[FullStrKit]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      kit => {
        strKitService.add(kit) map {
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
          case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
        }
      })
  }

  def update = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[FullStrKit]
    input.fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      kit => strKitService.update(kit).map(result => result match {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
        case Left(error) => BadRequest(Json.toJson(error))
      })
    )
  }

  def exportKits = Action.async {
    strKitService.listFull map { kits =>
      val json = Json.toJson(kits)
      Ok(json).as("application/json").withHeaders("Content-Disposition" -> "attachment; filename=kits.json")
    }
  }
  
  def delete(id: String) = Action.async {
    strKitService.delete(id) map {
      case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
      case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
    }
  }

}
