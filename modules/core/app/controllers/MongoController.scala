package controllers.core

import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Json, JsArray, JsObject}
import play.api.Configuration
import com.mongodb.client.{MongoClients, MongoClient, MongoDatabase, MongoCollection}
import org.bson.Document

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

/**
 * MongoDB Controller - Endpoint simple para probar FerretDB
 * 
 * Endpoints básicos para verificar conectividad con FerretDB
 * Usa el driver Java de MongoDB (compatible con Scala 3)
 */
@Singleton
class MongoController @Inject()(
  cc: ControllerComponents,
  config: Configuration
)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  // Configuración de MongoDB desde application.conf
  private val mongoUri = config.get[String]("mongodb.uri")
  private val dbName = config.get[String]("mongodb.database")
  
  // Cliente MongoDB (lazy para evitar conexión inmediata)
  private lazy val mongoClient: MongoClient = MongoClients.create(mongoUri)
  private lazy val database: MongoDatabase = mongoClient.getDatabase(dbName)
  
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
        InternalServerError(Json.obj(
          "status" -> "ERROR",
          "message" -> e.getMessage
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
        InternalServerError(Json.obj(
          "status" -> "ERROR",
          "collection" -> collectionName,
          "message" -> e.getMessage
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
        InternalServerError(Json.obj(
          "status" -> "ERROR",
          "message" -> e.getMessage
        ))
    }
  }
}
