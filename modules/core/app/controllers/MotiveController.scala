package controllers

import javax.inject.{Inject, Singleton}
import motive.{Motive, MotiveService}
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MotiveController @Inject()(
    motiveService: MotiveService,
    cc: ControllerComponents
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def getMotives(motiveType: Long, abm: Boolean): Action[AnyContent] = Action.async {
    motiveService.getMotives(motiveType, abm).map {
      case Nil     => NotFound(Json.obj("message" -> "No existe"))
      case motives => Ok(Json.toJson(motives))
    }
  }

  def getMotivesTypes: Action[AnyContent] = Action.async {
    motiveService.getMotivesTypes().map {
      case Nil     => NotFound(Json.obj("message" -> "No existe"))
      case motives => Ok(Json.toJson(motives))
    }
  }

  def deleteMotiveById(id: Long): Action[AnyContent] = Action.async {
    motiveService.deleteMotiveById(id).map {
      case Left(msg) => InternalServerError(Json.obj("message" -> msg))
      case Right(()) => Ok
    }
  }

  def insert: Action[play.api.mvc.AnyContent] = Action.async(parse.anyContent) { request =>
    request.body.asJson.map(_.validate[Motive]) match {
      case None => Future.successful(BadRequest(Json.obj("message" -> "Expected JSON body")))
      case Some(jsResult) =>
        jsResult.fold(
          errors => Future.successful(BadRequest(JsError.toJson(errors))),
          row =>
            motiveService.insert(row).map {
              case Left(msg) => InternalServerError(Json.obj("message" -> msg))
              case Right(id) => Ok(Json.obj("id" -> id))
            }
        )
    }
  }

  def update: Action[play.api.mvc.AnyContent] = Action.async(parse.anyContent) { request =>
    request.body.asJson.map(_.validate[Motive]) match {
      case None => Future.successful(BadRequest(Json.obj("message" -> "Expected JSON body")))
      case Some(jsResult) =>
        jsResult.fold(
          errors => Future.successful(BadRequest(JsError.toJson(errors))),
          row =>
            motiveService.update(row).map {
              case Left(msg) => InternalServerError(Json.obj("message" -> msg))
              case Right(_)  => Ok
            }
        )
    }
  }
}
