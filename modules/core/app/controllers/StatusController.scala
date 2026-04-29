package controllers.core

import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import user.LdapHealthService
import scala.util.{Failure, Success}

/**
 * Status Controller - Migrado a Scala 3 + Play 3
 *
 * Primer módulo migrado según modulesUpgrade guide.
 * Endpoints: /api/v2/status
 */
@Singleton
class StatusController @Inject()(cc: ControllerComponents, ldapHealth: LdapHealthService)
  extends AbstractController(cc) {

  private val logger = Logger(this.getClass)

  /**
   * Health check endpoint
   * GET /api/v2/status
   */
  def index(): Action[AnyContent] = Action {
    Ok(Json.obj(
      "status" -> "OK",
      "version" -> "5.2.0-SNAPSHOT",
      "scala" -> "3.3.1",
      "play" -> "3.0.6",
      "module" -> "core"
    ))
  }

  /**
   * Detailed status with system info
   * GET /api/v2/status/detailed
   */
  def detailed(): Action[AnyContent] = Action {
    val runtime = Runtime.getRuntime
    val maxMemory = runtime.maxMemory() / 1024 / 1024 // MB
    val totalMemory = runtime.totalMemory() / 1024 / 1024 // MB
    val freeMemory = runtime.freeMemory() / 1024 / 1024 // MB

    Ok(Json.obj(
      "status" -> "OK",
      "version" -> "5.2.0-SNAPSHOT",
      "scala" -> "3.3.1",
      "play" -> "3.0.6",
      "java" -> "17",
      "module" -> "core",
      "memory" -> Json.obj(
        "max" -> s"${maxMemory}MB",
        "total" -> s"${totalMemory}MB",
        "free" -> s"${freeMemory}MB",
        "used" -> s"${totalMemory - freeMemory}MB"
      ),
      "timestamp" -> System.currentTimeMillis()
    ))
  }

  def ldapStatus(): Action[AnyContent] = Action {
    ldapHealth.checkStatus() match {
      case Success((status, vendor)) => Ok(Json.obj("ldap" -> status, "vendor" -> vendor))
      case Failure(e)   =>
        logger.error("Error al verificar estado de LDAP", e)
        ServiceUnavailable(Json.obj("ldap" -> "DOWN", "error" -> "Error al verificar estado de LDAP"))
    }
  }
}
