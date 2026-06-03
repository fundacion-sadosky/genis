package controllers.core

import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{Json, JsArray, JsObject}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import com.mongodb.client.{MongoClients, MongoClient, MongoDatabase, MongoCollection}
import org.bson.Document

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

/**
 * MongoDB Controller - Endpoint simple para probar FerretDB
 *
 * Endpoints básicos para verificar conectividad con FerretDB
 * Usa el driver Java de MongoDB (compatible con Scala 3)
 *
 * TODO (seguimiento): extraer el acceso a MongoDB a un servicio inyectable
 * (provider + binding Guice) en vez de crear el MongoClient en el controller,
 * para cumplir la regla de DI (no instanciar infraestructura en controllers).
 */
@Singleton
class MongoController @Inject()(
  cc: ControllerComponents,
  config: Configuration,
  lifecycle: ApplicationLifecycle
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val logger: Logger = Logger(this.getClass)

  // Configuración de MongoDB desde application.conf
  private val mongoUri = config.get[String]("mongodb.uri")
  private val dbName = config.get[String]("mongodb.database")

  // Cliente MongoDB (lazy para evitar conexión inmediata)
  private lazy val mongoClient: MongoClient = MongoClients.create(mongoUri)
  private lazy val database: MongoDatabase = mongoClient.getDatabase(dbName)

  // Cierre coordinado del cliente al apagar la aplicación.
  lifecycle.addStopHook { () =>
    Future.successful(mongoClient.close())
  }

  /**
   * Lista todas las colecciones en la base de datos
   * GET /api/v2/mongo/collections
   */
  def listCollections(): Action[AnyContent] = Action {
    try {
      val collections = database.listCollectionNames().asScala.toSeq
      
      Ok(Json.obj(
        "status" -> "OK",
        "database" -> dbName,
        "collections" -> collections,
        "count" -> collections.size
      ))
    } catch {
      case e: Exception =>
        logger.error(e.getMessage, e)
        InternalServerError(Json.obj(
          "status" -> "ERROR",
          "message" -> "No se pudo listar las colecciones de MongoDB"
        ))
    }
  }

  /**
   * Cuenta documentos en una colección específica
   * GET /api/v2/mongo/count/:collection
   */
  def countDocuments(collectionName: String): Action[AnyContent] = Action {
    try {
      val collection: MongoCollection[Document] = database.getCollection(collectionName)
      val count = collection.countDocuments()

      Ok(Json.obj(
        "status" -> "OK",
        "database" -> dbName,
        "collection" -> collectionName,
        "documentCount" -> count
      ))
    } catch {
      case e: Exception =>
        logger.error(e.getMessage, e)
        InternalServerError(Json.obj(
          "status" -> "ERROR",
          "collection" -> collectionName,
          "message" -> "No se pudo contar los documentos de la colección"
        ))
    }
  }
  
  /**
   * Resumen general de la BD
   * GET /api/v2/mongo/stats
   */
  def stats(): Action[AnyContent] = Action {
    try {
      val collectionNames = database.listCollectionNames().asScala.toSeq
      
      // Obtener count de cada colección
      val collectionsInfo = collectionNames.map { name =>
        val collection = database.getCollection(name)
        val count = collection.countDocuments()
        Json.obj("name" -> name, "documents" -> count)
      }
      
      val totalDocs = collectionsInfo.map(obj => (obj \ "documents").as[Long]).sum
      
      Ok(Json.obj(
        "status" -> "OK",
        "database" -> dbName,
        "totalCollections" -> collectionNames.size,
        "totalDocuments" -> totalDocs,
        "collections" -> JsArray(collectionsInfo)
      ))
    } catch {
      case e: Exception =>
        logger.error(e.getMessage, e)
        InternalServerError(Json.obj(
          "status" -> "ERROR",
          "message" -> "No se pudo obtener el resumen de MongoDB"
        ))
    }
  }
}
