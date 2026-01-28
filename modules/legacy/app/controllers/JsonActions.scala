package controllers

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsError
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.Action
import play.api.mvc.BodyParsers
import play.api.mvc.Results
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results.Ok
import views.html.defaultpages.badRequest
import org.postgresql.util.PSQLException
import types.DataAccessException
import play.api.Logger

trait JsonActions {

  def JsonAction[O](action: () => Future[Option[O]])(implicit writer: Writes[O]) = Action.async { request =>
    val result = action()
    result map { outOpt =>
      outOpt.map { output =>
        Ok(Json.toJson(output))
      } getOrElse {
        Results.NotFound
      }
    }
  }

  def JsonAction[I, O](action: I => Future[Option[O]])(implicit reader: Reads[I], writer: Writes[O]) = Action.async(BodyParsers.parse.json) { request =>

    val input = request.body.validate[I]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        val result = action(input)
        result map { outOpt =>
          outOpt.map { output =>
            Ok(Json.toJson(output))
          } getOrElse {
            Results.NotFound
          }
        }
      })
  }

  def JsonAction[S, E](action: () => Future[Either[E, S]])(implicit successWriter: Writes[S], errorWriter: Writes[E]) = Action.async(BodyParsers.parse.json) { request =>
    val result = action()
    result map { outEither =>
      outEither.fold(
        error => BadRequest(Json.toJson(error)),
        success => Ok(Json.toJson(success)))
    }
  }

  def JsonAction[I, S, E](action: I => Future[Either[E, S]])(implicit reader: Reads[I], successWriter: Writes[S], errorWriter: Writes[E]) = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[I]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        val result = action(input)
        result map { outEither =>
          outEither.fold(
            error => BadRequest(Json.toJson(error)),
            success => Ok(Json.toJson(success)))
        }
      })
  }
}

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
