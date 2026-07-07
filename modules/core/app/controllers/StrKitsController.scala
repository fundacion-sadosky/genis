package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.{Json, JsError, OFormat}
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
    request.body.asJson.map(_.validate[FullStrKit]) match
      case None => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Expected JSON body")))
      case Some(result) => result.fold(
        errors => Future.successful(BadRequest(JsError.toJson(errors))),
        kit => strKitService.add(kit).map {
          case Right(id)   => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
          case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
        }
      )
  }

  def update(): Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson.map(_.validate[FullStrKit]) match
      case None => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Expected JSON body")))
      case Some(result) => result.fold(
        errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
        kit => strKitService.update(kit).map {
          case Right(id)   => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
          case Left(error) => BadRequest(Json.toJson(error))
        }
      )
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

  def findLociByKit(kitId: String): Action[AnyContent] = Action.async { implicit request =>
    strKitService.findLociByKit(kitId).map { loci => Ok(Json.toJson(loci)) }
  }
}
