package controllers

import play.api.mvc.{AbstractController, AnyContent, Action, ControllerComponents}
import play.api.libs.json.Json
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import services.LaboratoryService



@Singleton
class TestFerController @Inject()(
    cc: ControllerComponents,
    labService: LaboratoryService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

 def hardcoded(): Action[AnyContent] = Action {
    Ok(Json.obj(
      "message" -> "hardcoded response", 
      "ok" -> true 
    ))
 }

 def echo(text:String): Action[AnyContent] = Action {
  Ok(Json.obj({
    "texto" -> text
  }))
 }
}


