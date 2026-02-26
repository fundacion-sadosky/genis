package controllers

import javax.inject.{Inject, Singleton}
import disclaimer.{Disclaimer, DisclaimerService}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import scala.concurrent.ExecutionContext

@Singleton
class DisclaimerController @Inject()(
    disclaimerService: DisclaimerService,
    cc: ControllerComponents
)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with Logging {

  def getDisclaimer = Action.async { implicit request =>
    disclaimerService.get().map {
      case Disclaimer(Some(disclaimer)) => Ok(Json.toJson(Disclaimer(Some(disclaimer))))
      case Disclaimer(None) => NotFound(Json.obj("message" -> "No existe en disclaimer"))
    }
  }
}
