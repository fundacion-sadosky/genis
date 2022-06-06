package controllers

import javax.inject.{Inject, Singleton}

import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, BodyParsers, Controller}
import trace.{TraceSearch,TraceSearchPedigree, TraceService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

@Singleton
class Traces @Inject() (traceService: TraceService) extends Controller {

  def search = Action.async(BodyParsers.parse.json) { request =>
    val search = request.body.validate[TraceSearch]

    search.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        traceService.search(input) map { traces =>
          Ok(Json.toJson(traces))
        }
      })

  }

  def count = Action.async(BodyParsers.parse.json) { request =>
    val search = request.body.validate[TraceSearch]

    search.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        traceService.count(input) map { size =>
          Ok("").withHeaders("X-TRACE-LENGTH" -> size.toString)
        }
      })

  }

  def searchPedigree = Action.async(BodyParsers.parse.json) { request =>
    val search = request.body.validate[TraceSearchPedigree]

    search.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        traceService.searchPedigree(input) map { traces =>
          Ok(Json.toJson(traces))
        }
      })

  }

  def countPedigree = Action.async(BodyParsers.parse.json) { request =>
    val search = request.body.validate[TraceSearchPedigree]

    search.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        traceService.countPedigree(input) map { size =>
          Ok("").withHeaders("X-TRACE-LENGTH" -> size.toString)
        }
      })

  }

  def getFullDescription(id: Long) = Action.async { request =>
    traceService.getFullDescription(id) map { description =>
      Ok(Json.toJson(description))
    }
  }

}
