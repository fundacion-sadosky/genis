package controllers

import javax.inject.{Inject, Singleton}

import disclaimer.{Disclaimer, DisclaimerService}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DisclaimerController @Inject()(
  cc: ControllerComponents,
  disclaimerService: DisclaimerService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def getDisclaimer = Action.async { _ =>
    disclaimerService.get().map {
      case Disclaimer(Some(disclaimer)) => Ok(Json.toJson(Disclaimer(Some(disclaimer))))
      case Disclaimer(None) => NotFound(Json.obj("message" -> "No existe en disclaimer"))
    }
  }
}

