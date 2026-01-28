package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import kits.AnalysisTypeService
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

@Singleton
class AnalysisTypes @Inject() (service: AnalysisTypeService) extends Controller {

  def list = Action.async {
    service.list map { kits => Ok(Json.toJson(kits)) }
  }

}
