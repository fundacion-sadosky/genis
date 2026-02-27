package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents, Action}
import play.api.libs.json.{Json, JsError}
import scala.concurrent.{ExecutionContext, Future}
import services.GeneticistService
import services.UserService
import types.Geneticist

@Singleton
class GeneticistsController @Inject() (
    cc: ControllerComponents,
    genService: GeneticistService,
    userService: UserService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def allGeneticist(lab: String) = Action.async {
    genService.getAll(lab) map { gens =>
      Ok(Json.toJson(gens))
    }
  }

  def getGeneticistUsers = Action.async {
    userService.findUserAssignable map { gens =>
      Ok(Json.toJson(gens.sortBy(user => user.firstName + " " + user.lastName)))
    }
  }

  def addGeneticist = Action.async(parse.json) { request =>
    request.body.validate[Geneticist].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      gen => genService.add(gen).map(result => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString()))
        .recover {
          case e: Exception => BadRequest(Json.obj("error" -> e.getMessage))
        }
    )
  }

  def updateGeneticist = Action.async(parse.json) { request =>
    request.body.validate[Geneticist].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      gen => genService.update(gen).map(result => Ok(Json.toJson(result)))
    )
  }

  def getGeneticist(geneticistId: Long) = Action.async {
    genService.get(geneticistId) map {
      case Some(gen) => Ok(Json.toJson(gen))
      case None      => NoContent
    }
  }
}
