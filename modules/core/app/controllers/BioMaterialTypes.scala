package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import configdata.BioMaterialTypeService
import scala.concurrent.{ExecutionContext, Future}
import types.DataAccessException
import play.api.Logger

@Singleton
class BioMaterialTypes @Inject() (
  val controllerComponents: ControllerComponents,
  bioMaterialTypeService: BioMaterialTypeService
)(implicit ec: ExecutionContext) extends BaseController {

  private val logger: Logger = Logger(this.getClass())

  def list: Action[AnyContent] = Action.async {
    bioMaterialTypeService.list().map { bioMaterials =>
      Ok(Json.toJson(bioMaterials))
    }
  }

  def insert: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[configdata.BioMaterialType].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> "Invalid JSON", "details" -> errors.toString))),
      bmt => bioMaterialTypeService.insert(bmt).map { result =>
        Ok(Json.toJson(result))
      }.recover {
        case dae: DataAccessException =>
          logger.error(dae.msg, dae.error)
          BadRequest(Json.obj("error" -> dae.msg))
      }
    )
  }

  def update: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[configdata.BioMaterialType].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> "Invalid JSON", "details" -> errors.toString))),
      bmt => bioMaterialTypeService.update(bmt).map { result =>
        Ok(Json.toJson(result))
      }.recover {
        case dae: DataAccessException =>
          logger.error(dae.msg, dae.error)
          BadRequest(Json.obj("error" -> dae.msg))
      }
    )
  }

  def remove(bmtId: String): Action[AnyContent] = Action.async {
    bioMaterialTypeService.delete(bmtId)
      .map { res => Ok(Json.obj("deletes" -> res)) }
      .recover {
        case dae: DataAccessException =>
          logger.error(dae.msg, dae.error)
          BadRequest(Json.obj("error" -> dae.msg))
      }
  }
}
