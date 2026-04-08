package controllers.core

import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import profile.MongoHealthService

import scala.util.{Failure, Success}

@Singleton
class MongoController @Inject()(
  cc: ControllerComponents,
  mongoHealth: MongoHealthService
) extends AbstractController(cc):

  private val logger = Logger(this.getClass)

  def status(): Action[AnyContent] = Action {
    mongoHealth.checkStatus() match
      case Success((status, dbName)) =>
        Ok(Json.obj("mongo" -> status, "database" -> dbName))
      case Failure(e) =>
        logger.error("Error al verificar estado de MongoDB", e)
        ServiceUnavailable(Json.obj("mongo" -> "DOWN", "error" -> "Error al verificar estado de MongoDB"))
  }
