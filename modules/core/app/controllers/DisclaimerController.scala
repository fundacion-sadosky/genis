package controllers.core

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.api.libs.json.Json
import disclaimer.{Disclaimer, DisclaimerService}
import scala.concurrent.ExecutionContext

@Singleton
class DisclaimerController @Inject()(
  cc: ControllerComponents,
  disclaimerService: DisclaimerService
)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def getDisclaimer: Action[AnyContent] = Action.async { _ =>
    disclaimerService.get().map {
      case Disclaimer(Some(disclaimer)) => Ok(Json.toJson(Disclaimer(Some(disclaimer))))
      case Disclaimer(None) => NotFound(Json.obj("message" -> "No existe en disclaimer"))
    }
  }
}
