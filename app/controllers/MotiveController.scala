package controllers

import javax.inject.{Inject, Singleton}

import kits.FullLocus
import motive.{Motive, MotiveService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.Future
import scala.util.{Left, Right}

@Singleton
class MotiveController @Inject()(motiveService: MotiveService) extends Controller {

  def getMotives(motiveType: Long, abm:Boolean) = Action.async {
    request => {
      motiveService.getMotives(motiveType,abm).map {
        case Nil => NotFound(Json.obj("message" -> "No existe"))
        case motive => Ok(Json.toJson(motive))
      }
    }
  }

  def getMotivesTypes = Action.async {
    request => {
      motiveService.getMotivesTypes().map {
        case Nil => NotFound(Json.obj("message" -> "No existe"))
        case motives => Ok(Json.toJson(motives))
      }
    }
  }

  def deleteMotiveById(id: Long) = Action.async {
    request => {
      motiveService.deleteMotiveById(id).map {
        case Left(msg) => NotFound(Json.obj("message" -> msg))
        case Right(()) => Ok
      }
    }
  }

  def insert = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[Motive]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      row => {
        motiveService.insert(row).map {
          case Left(msg) => NotFound(Json.obj("message" -> msg))
          case Right(id) => Ok(Json.obj("id" -> id))
        }
      })
  }

  def update = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[Motive]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      row => {
        motiveService.update(row).map {
          case Left(msg) => NotFound(Json.obj("message" -> msg))
          case Right(_) => Ok
        }
      })
  }

}
