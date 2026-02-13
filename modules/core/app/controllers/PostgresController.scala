package controllers.core

import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.Configuration

import scala.concurrent.ExecutionContext
import java.sql.{Connection, DriverManager}

/**
 * PostgreSQL Controller - Endpoint para verificar conectividad con PostgreSQL
 * 
 * Endpoints básicos para verificar conexión a la base de datos PostgreSQL legacy
 */
@Singleton
class PostgresController @Inject()(
  cc: ControllerComponents,
  config: Configuration
)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  // Configuración de PostgreSQL desde application.conf
  private val jdbcUrl = config.get[String]("slick.dbs.default.db.url")
  private val username = config.get[String]("slick.dbs.default.db.user")
  private val password = config.get[String]("slick.dbs.default.db.password")
  
  /**
   * Verifica la conectividad con PostgreSQL
   * GET /api/v2/postgres/ping
   */
  def ping(): Action[AnyContent] = Action {
    var connection: Connection = null
    try {
      // Intentar establecer conexión
      connection = DriverManager.getConnection(jdbcUrl, username, password)
      
      // Verificar que la conexión está activa
      val isValid = connection.isValid(5) // timeout de 5 segundos
      
      if (isValid) {
        val metadata = connection.getMetaData
        
        Ok(Json.obj(
          "status" -> "OK",
          "message" -> "PostgreSQL connection successful",
          "database" -> metadata.getDatabaseProductName,
          "version" -> metadata.getDatabaseProductVersion,
          "url" -> jdbcUrl.replaceAll(":[^:]+@", ":***@"), // ocultar password en URL
          "user" -> username
        ))
      } else {
        ServiceUnavailable(Json.obj(
          "status" -> "ERROR",
          "message" -> "PostgreSQL connection is not valid"
        ))
      }
    } catch {
      case e: Exception =>
        InternalServerError(Json.obj(
          "status" -> "ERROR",
          "message" -> e.getMessage,
          "type" -> e.getClass.getSimpleName
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
      connection = DriverManager.getConnection(jdbcUrl, username, password)
      val metadata = connection.getMetaData
      
      // Obtener lista de tablas
      val resultSet = metadata.getTables(null, null, "%", Array("TABLE"))
      var tables = List.empty[String]
      while (resultSet.next()) {
        tables = tables :+ resultSet.getString("TABLE_NAME")
      }
      resultSet.close()
      
      Ok(Json.obj(
        "status" -> "OK",
        "database" -> Json.obj(
          "product" -> metadata.getDatabaseProductName,
          "version" -> metadata.getDatabaseProductVersion,
          "driver" -> metadata.getDriverName,
          "driverVersion" -> metadata.getDriverVersion
        ),
        "connection" -> Json.obj(
          "url" -> jdbcUrl.replaceAll(":[^:]+@", ":***@"),
          "user" -> username,
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
        InternalServerError(Json.obj(
          "status" -> "ERROR",
          "message" -> e.getMessage,
          "type" -> e.getClass.getSimpleName
        ))
    } finally {
      if (connection != null && !connection.isClosed) {
        connection.close()
      }
    }
  }
}
