package controllers.core

import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext
import java.sql.{Connection, ResultSet}

/**
 * PostgreSQL Controller - Endpoint para verificar conectividad con PostgreSQL
 *
 * Endpoints básicos de diagnóstico de conexión a PostgreSQL. Usa el `Database`
 * singleton inyectado (mismo pool/configuración que el resto de la app), en vez
 * de abrir conexiones propias con credenciales tomadas de la Configuration.
 */
@Singleton
class PostgresController @Inject()(
  cc: ControllerComponents,
  db: slick.jdbc.JdbcBackend.Database
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val logger: Logger = Logger(this.getClass)

  /**
   * Verifica la conectividad con PostgreSQL
   * GET /api/v2/postgres/ping
   */
  def ping(): Action[AnyContent] = Action {
    var connection: Connection = null
    try {
      connection = db.source.createConnection()
      val isValid = connection.isValid(5) // timeout de 5 segundos

      if (isValid) {
        val metadata = connection.getMetaData

        Ok(Json.obj(
          "status" -> "OK",
          "message" -> "PostgreSQL connection successful",
          "database" -> metadata.getDatabaseProductName,
          "version" -> metadata.getDatabaseProductVersion
        ))
      } else {
        ServiceUnavailable(Json.obj(
          "status" -> "ERROR",
          "message" -> "PostgreSQL connection is not valid"
        ))
      }
    } catch {
      case e: Exception =>
        logger.error(e.getMessage, e)
        InternalServerError(Json.obj(
          "status" -> "ERROR",
          "message" -> "No se pudo conectar a PostgreSQL"
        ))
    } finally {
      if (connection != null && !connection.isClosed) {
        connection.close()
      }
    }
  }

  /**
   * Obtiene información detallada de la base de datos
   * GET /api/v2/postgres/info
   */
  def info(): Action[AnyContent] = Action {
    var connection: Connection = null
    try {
      connection = db.source.createConnection()
      val metadata = connection.getMetaData

      // Obtener lista de tablas
      val resultSet: ResultSet = metadata.getTables(null, null, "%", Array("TABLE"))
      var tables = List.empty[String]
      try {
        while (resultSet.next()) {
          tables = tables :+ resultSet.getString("TABLE_NAME")
        }
      } finally {
        resultSet.close()
      }

      Ok(Json.obj(
        "status" -> "OK",
        "database" -> Json.obj(
          "product" -> metadata.getDatabaseProductName,
          "version" -> metadata.getDatabaseProductVersion,
          "driver" -> metadata.getDriverName,
          "driverVersion" -> metadata.getDriverVersion
        ),
        "connection" -> Json.obj(
          "catalog" -> connection.getCatalog,
          "schema" -> connection.getSchema
        ),
        "tables" -> Json.obj(
          "count" -> tables.size,
          "list" -> tables.sorted
        )
      ))
    } catch {
      case e: Exception =>
        logger.error(e.getMessage, e)
        InternalServerError(Json.obj(
          "status" -> "ERROR",
          "message" -> "No se pudo obtener información de PostgreSQL"
        ))
    } finally {
      if (connection != null && !connection.isClosed) {
        connection.close()
      }
    }
  }
}
