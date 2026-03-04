package controllers

import play.api.mvc.{AbstractController, AnyContent, Action, ControllerComponents}
import play.api.libs.json.Json
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import services.LaboratoryService
import com.mongodb.client.{MongoClients, MongoClient, MongoDatabase}
import play.api.Configuration
import scala.jdk.CollectionConverters._
import java.sql.{Connection, DriverManager}


@Singleton
class TestController @Inject()(
    cc: ControllerComponents,
    config: Configuration,
    labService: LaboratoryService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {


    // deberia ir en un Service la conexion?

    // Configuración de PostgreSQL desde application.conf
    private val mongoUri = config.get[String]("mongodb.uri")
    private val dbName = config.get[String]("mongodb.database")

    private lazy val mongoClient: MongoClient = MongoClients.create(mongoUri)
    private lazy val database: MongoDatabase = mongoClient.getDatabase(dbName)

    // Configuración de PostgreSQL desde application.conf
    private val jdbcUrl = config.get[String]("slick.dbs.default.db.url")
    private val username = config.get[String]("slick.dbs.default.db.user")
    private val password = config.get[String]("slick.dbs.default.db.password")


    def hardcoded(): Action[AnyContent] = Action {
        Ok(Json.obj(
            "message" -> "hardcoded response",
            "ok" -> true
        ))
    }

    def echo(text: String): Action[AnyContent] = Action {
        Ok(Json.obj(
            "texto" -> text
        ))
    }

    def findMongoCollections(filter: Option[String]): Action[AnyContent] = Action {
        try {
            val collections = database.listCollectionNames().iterator().asScala.toSeq
            val filteredCollections = filter match {
                case Some(f) => collections.filter(_.contains(f))
                case None => collections
            }
            Ok(Json.obj(
                "database" -> dbName,
                "count" -> filteredCollections.length,
                "filter" -> filter.getOrElse("none"),
                "collections" -> filteredCollections
            ))

        } catch {
            case e: Exception =>
                InternalServerError(Json.obj("message" -> e.getMessage))
        }
    }

    def findPostgresTables(filter: Option[String]): Action[AnyContent] = Action {

        var connection: Connection = null


        try {
            // Intentar establecer conexión
            connection = DriverManager.getConnection(jdbcUrl, username, password)
            val metadata = connection.getMetaData


            val resultSet = metadata.getTables(null, null, "%", Array("TABLE"))
            var tables = List.empty[String]
            while (resultSet.next()) {
                tables = tables :+ resultSet.getString("TABLE_NAME")
            }

            val filteredTables = filter match {
                case Some(f) => tables.filter(_.toLowerCase.contains(f.toLowerCase))
                case None    => tables
            }

            resultSet.close()

            Ok(Json.obj(
                "status" -> "OK",
                "tables" -> Json.obj(
                    "count" -> filteredTables.size,
                    "filter" -> filter.getOrElse("none"),
                    "list" -> filteredTables.sorted
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

//GET     /api/v2/testRoute                       controllers.TestController.hardcoded()
//GET     /api/v2/testEcho                        controllers.TestController.echo(text: String)
//GET     /api/v2/testCollections                 controllers.TestController.findMongoCollections(filter: Option[String])
//GET     /api/v2/testPostgresTables              controllers.TestController.findPostgresTables(filter: Option[String])


