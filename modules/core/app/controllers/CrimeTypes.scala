package controllers

import play.api.mvc.{BaseController, ControllerComponents}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import configdata.CrimeTypeService

@Singleton
class CrimeTypes @Inject() (
  val controllerComponents: ControllerComponents,
  crimeTypeService: CrimeTypeService
)(implicit ec: ExecutionContext) extends BaseController {

  def list = Action.async {
    crimeTypeService.list().map { crimeTypes =>
      Ok(Json.toJson(crimeTypes))
    }
  }
}
