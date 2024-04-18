package controllers

import scala.concurrent.Future

import org.postgresql.util.PSQLException

import javax.inject.Inject
import javax.inject.Singleton
import laboratories.Geneticist
import laboratories.GeneticistService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsError
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.BodyParsers
import play.api.mvc.Controller
import user.UserService

@Singleton
class Geneticists @Inject() (genService: GeneticistService, userService: UserService) extends Controller {

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

  def addGeneticist = Action.async(BodyParsers.parse.json) { request =>
    val gen = request.body.validate[Geneticist]
    gen.fold(errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      gen => genService.add(gen).map(result => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString()))
        .recover {
          case psql: PSQLException => {
            psql.getSQLState match {
              case "23505" => BadRequest(Json.obj("error" -> "Nombre ya utilizado en el Laboratio"))
              case _       => BadRequest(Json.obj("error" -> "Error inesperado en la base de datos"))
            }
          }
        })
  }

  def updateGeneticist = Action.async(BodyParsers.parse.json) { request =>
    val gen = request.body.validate[Geneticist]

    gen.fold(errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      gen => genService.update(gen).map(result => Ok(Json.toJson(result))))
  }

  def getGeneticist(geneticistId: Long) = Action.async {
    genService.get(geneticistId) map { geneticist =>
      geneticist match {
        case Some(gen) => Ok(Json.toJson(gen))
        case None      => NoContent
      }
    }
  }

}