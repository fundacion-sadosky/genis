package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import types.DataAccessException

trait JsonSimpleActions {
  def JsonSimpleAction[I, O](action: I => Future[O])(implicit reader: Reads[I], writer: Writes[O]) = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[I]
    input fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        val result = action(input)
        result map { output =>
          Ok(Json.toJson(output))
        }
      })
  }
}

trait JsonSqlActions {
  def JsonSqlAction[I, S](action: I => Future[S])(implicit reader: Reads[I], successWriter: Writes[S]) = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[I]
    input.fold(err => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(err)))),
      parsed => action(parsed)
        .map { x => Ok(Json.obj("status" -> "OK", "Id" -> x)) }
        .recover {
          case dae: DataAccessException => {
            BadRequest(Json.obj("error" -> dae.msg))
          }
        })
  }
}
