package controllers

import jakarta.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import trace.{TraceSearch, TraceSearchPedigree, TraceService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Traces @Inject() (
  val controllerComponents: ControllerComponents,
  traceService: TraceService
)(implicit ec: ExecutionContext) extends BaseController {

  def search: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[TraceSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input  => traceService.search(input).map(traces => Ok(Json.toJson(traces)))
    )
  }

  def count: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[TraceSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input  => traceService.count(input).map(size => Ok("").withHeaders("X-TRACE-LENGTH" -> size.toString))
    )
  }

  def searchPedigree: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[TraceSearchPedigree].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input  => traceService.searchPedigree(input).map(traces => Ok(Json.toJson(traces)))
    )
  }

  def countPedigree: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[TraceSearchPedigree].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input  => traceService.countPedigree(input).map(size => Ok("").withHeaders("X-TRACE-LENGTH" -> size.toString))
    )
  }

  def getFullDescription(id: Long): Action[AnyContent] = Action.async {
    traceService.getFullDescription(id).map(description => Ok(Json.toJson(description)))
  }
}
