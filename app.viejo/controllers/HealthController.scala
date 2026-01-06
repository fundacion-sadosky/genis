package controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._

/**
 * Controlador de Salud del Sistema
 * Monitorea el estado de la aplicación y sus dependencias
 */
@Singleton
class HealthController @Inject()(
    cc: ControllerComponents,
    db: Database
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * Endpoint de salud general
   * GET /api/health
   */
  def health(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(Json.obj(
      "status" -> "UP",
      "timestamp" -> System.currentTimeMillis(),
      "version" -> "6.0.0.develop",
      "application" -> "GENis",
      "environment" -> "development"
    )))
  }

  /**
   * Verifica la conexión a la base de datos
   * GET /api/health/db
   */
  def healthDb(): Action[AnyContent] = Action.async { implicit request =>
    val query = sql"SELECT 1".as[Int]
    
    db.run(query).map { _ =>
      Ok(Json.obj(
        "status" -> "UP",
        "database" -> "PostgreSQL",
        "message" -> "Conexión a base de datos exitosa"
      ))
    }.recover {
      case e: Exception =>
        ServiceUnavailable(Json.obj(
          "status" -> "DOWN",
          "database" -> "PostgreSQL",
          "error" -> e.getMessage
        ))
    }
  }
}
