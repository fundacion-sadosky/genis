package controllers

import jakarta.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import services.UserService
import trace.{TraceSearch, TraceSearchPedigree, TraceService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Traces @Inject() (
  val controllerComponents: ControllerComponents,
  traceService: TraceService,
  userService: UserService
)(implicit ec: ExecutionContext) extends BaseController {

  private def resolveIsSuperUser(request: play.api.mvc.RequestHeader): Future[Boolean] =
    request.session.get("X-USER") match
      case Some(userId) => userService.isSuperUser(userId)
      case None         => Future.successful(false)

  def search: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[TraceSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input  => resolveIsSuperUser(request).flatMap { isSuperUser =>
        traceService.search(input.copy(isSuperUser = isSuperUser)).map(traces => Ok(Json.toJson(traces)))
      }
    )
  }

  def count: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[TraceSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input  => resolveIsSuperUser(request).flatMap { isSuperUser =>
        traceService.count(input.copy(isSuperUser = isSuperUser)).map(size => Ok("").withHeaders("X-TRACE-LENGTH" -> size.toString))
      }
    )
  }

  def searchPedigree: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[TraceSearchPedigree].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input  => resolveIsSuperUser(request).flatMap { isSuperUser =>
        traceService.searchPedigree(input.copy(isSuperUser = isSuperUser)).map(traces => Ok(Json.toJson(traces)))
      }
    )
  }

  def countPedigree: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[TraceSearchPedigree].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input  => resolveIsSuperUser(request).flatMap { isSuperUser =>
        traceService.countPedigree(input.copy(isSuperUser = isSuperUser)).map(size => Ok("").withHeaders("X-TRACE-LENGTH" -> size.toString))
      }
    )
  }

  def getFullDescription(id: Long): Action[AnyContent] = Action.async {
    traceService.getFullDescription(id).map(description => Ok(Json.toJson(description)))
  }
}
