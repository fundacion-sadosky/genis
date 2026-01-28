package controllers

import javax.inject.{Inject, Singleton}

import disclaimer.{Disclaimer, DisclaimerService}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

@Singleton
class DisclaimerController @Inject()(disclaimerService : DisclaimerService) extends Controller {
  val logger: Logger = Logger(this.getClass())

  def getDisclaimer = Action.async {
    request => {
      disclaimerService.get().map{
        case Disclaimer(Some(disclaimer)) => Ok(Json.toJson(Disclaimer(Some(disclaimer))))
        case Disclaimer(None) => NotFound(Json.obj("message" -> "No existe en disclaimer"))
      }
    }
  }

}
