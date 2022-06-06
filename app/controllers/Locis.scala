package controllers

import javax.inject.{Inject, Singleton}

import kits.{FullLocus, Locus, LocusService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.Future

@Singleton
class Locis @Inject() (locusService: LocusService) extends Controller {

  def add = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[FullLocus]

    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      fullLocus => {
          locusService.add(fullLocus) map {
            case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
            case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
          }
      })
  }
  def update = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[FullLocus]

    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      locus => {
        locusService.update(locus) map {
          case Right(()) => Ok(Json.toJson(locus.locus.id)).withHeaders("X-CREATED-ID" -> locus.locus.id)
          case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
        }
      })
  }
  def listFull = Action.async {
    locusService.listFull map { locus =>
      Ok(Json.toJson(locus))
    }
  }

  def list = Action.async {
    locusService.list map { locus =>
      Ok(Json.toJson(locus))
    }
  }

  def delete(id: String) = Action.async {
    locusService.delete(id) map {
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
          case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
        }
  }

  def ranges = Action.async {
    Future.successful(locusService.locusRangeMap) map { locusRanges =>
      Ok(Json.toJson(locusRanges))
    }
  }

}
