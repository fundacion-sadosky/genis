package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.Json
import kits.AnalysisTypeService

import scala.concurrent.ExecutionContext

@Singleton
class AnalysisTypesController @Inject()(
  cc: ControllerComponents,
  service: AnalysisTypeService
)(implicit ec: ExecutionContext) extends AbstractController(cc):

  def list: Action[AnyContent] = Action.async {
    service.list().map(types => Ok(Json.toJson(types)))
  }
