package controllers


import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import disclaimer.{DisclaimerService, DisclaimerServiceImpl, DisclaimerRepositoryImpl}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DisclaimerController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  // Instanciar el repositorio y pasar al servicio (simulando inyección real)
  private val disclaimerService: DisclaimerService = new DisclaimerServiceImpl(new DisclaimerRepositoryImpl())

  def getDisclaimer(): Action[AnyContent] = Action.async { implicit request =>
    disclaimerService.get().map { disclaimer =>
      Ok(Json.toJson(disclaimer))
    }
  }
}
