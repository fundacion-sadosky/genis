package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.{Json, OFormat}
import kits._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StrKitsController @Inject()(
    cc: ControllerComponents,
    strKitService: StrKitService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def get(id: String): Action[AnyContent] = Action.async { implicit request =>
    strKitService.get(id).map {
      case Some(kit) => Ok(Json.toJson(kit))
      case None      => NotFound(Json.obj("error" -> "Kit not found"))
    }
  }

  def getFull(id: String): Action[AnyContent] = Action.async { implicit request =>
    strKitService.getFull(id).map {
      case Some(kit) => Ok(Json.toJson(kit))
      case None      => NotFound(Json.obj("error" -> "Kit not found"))
    }
  }

  def list(): Action[AnyContent] = Action.async { implicit request =>
    strKitService.list().map { kits => Ok(Json.toJson(kits)) }
  }

  def listFull(): Action[AnyContent] = Action.async { implicit request =>
    strKitService.listFull().map { kits => Ok(Json.toJson(kits)) }
  }

  def add(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(NotImplemented)
  }

  def update(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(NotImplemented)
  }

  def delete(id: String): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(NotImplemented)
  }

  def importKits(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(NotImplemented)
  }

  def exportKits(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(NotImplemented)
  }
}
