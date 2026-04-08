package controllers.core

import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import configdata.PostgresHealthService

import scala.util.{Failure, Success}

@Singleton
class PostgresController @Inject()(
  cc: ControllerComponents,
  postgresHealth: PostgresHealthService
) extends AbstractController(cc):

  private val logger = Logger(this.getClass)

  def status(): Action[AnyContent] = Action {
    postgresHealth.checkStatus() match
      case Success((status, info)) =>
        Ok(Json.obj("postgres" -> status, "info" -> info))
      case Failure(e) =>
        logger.error("Error al verificar estado de PostgreSQL", e)
        ServiceUnavailable(Json.obj("postgres" -> "DOWN", "error" -> "Error al verificar estado de PostgreSQL"))
  }
