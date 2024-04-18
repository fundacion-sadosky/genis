package controllers

import play.api.mvc.Action
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.Controller
import javax.inject.Singleton
import play.api.mvc.Action
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import configdata.CrimeTypeService

@Singleton
class CrimeTypes @Inject() (crimeTypeService: CrimeTypeService) extends Controller {

  def list = Action.async {
    crimeTypeService.list map { crimeTypes =>
      Ok(Json.toJson(crimeTypes))
    }
  }
}
