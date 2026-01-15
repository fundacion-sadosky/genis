package controllers

import models.configdata.{CrimeRow, CrimeType, CrimeTypeRow}
import services.CrimeTypeService
import play.api.mvc._
import play.api.libs.json._
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Controller para gestionar tipos de crimen y crímenes.
 * Expone endpoints REST para operaciones CRUD.
 */
@Singleton
class CrimeTypeController @Inject() (
  val controllerComponents: ControllerComponents,
  crimeTypeService: CrimeTypeService
)(implicit ec: ExecutionContext) extends BaseController {

  /**
   * GET /api/crimeTypes
   * Lista todos los tipos de crimen con sus crímenes asociados
   * Devuelve un Map[id -> CrimeType] para compatibilidad con frontend legacy
   */
  def list(): Action[AnyContent] = Action.async {
    crimeTypeService.listAsMap().map { crimeTypesMap =>
      Ok(Json.toJson(crimeTypesMap))
    }.recover {
      case e: Exception =>
        InternalServerError(Json.obj("error" -> e.getMessage))
    }
  }

  /**
   * GET /api/crimeTypes/:id
   * Obtiene un tipo de crimen por ID
   */
  def findById(id: String): Action[AnyContent] = Action.async {
    crimeTypeService.findById(id).map {
      case Some(crimeType) => Ok(Json.toJson(crimeType))
      case None => NotFound(Json.obj("error" -> s"CrimeType con id '$id' no encontrado"))
    }.recover {
      case e: Exception =>
        InternalServerError(Json.obj("error" -> e.getMessage))
    }
  }

  /**
   * POST /api/crimeTypes
   * Crea un nuevo tipo de crimen
   * Body: { "id": "...", "name": "...", "description": "..." }
   */
  def createCrimeType(): Action[JsValue] = Action.async(parse.json) { request =>
    val json = request.body
    val id = (json \ "id").as[String]
    val name = (json \ "name").as[String]
    val description = (json \ "description").asOpt[String]
    
    crimeTypeService.createCrimeType(id, name, description).map { created =>
      Created(Json.obj(
        "id" -> created.id,
        "name" -> created.name,
        "description" -> created.description
      ))
    }.recover {
      case e: Exception =>
        BadRequest(Json.obj("error" -> e.getMessage))
    }
  }

  /**
   * POST /api/crimeTypes/:crimeTypeId/crimes
   * Crea un nuevo crimen asociado a un tipo
   * Body: { "id": "...", "name": "...", "description": "..." }
   */
  def createCrime(crimeTypeId: String): Action[JsValue] = Action.async(parse.json) { request =>
    val json = request.body
    val id = (json \ "id").as[String]
    val name = (json \ "name").as[String]
    val description = (json \ "description").asOpt[String]
    
    crimeTypeService.createCrime(id, crimeTypeId, name, description).map { created =>
      Created(Json.obj(
        "id" -> created.id,
        "crimeTypeId" -> created.crimeTypeId,
        "name" -> created.name,
        "description" -> created.description
      ))
    }.recover {
      case e: Exception =>
        BadRequest(Json.obj("error" -> e.getMessage))
    }
  }

  /**
   * GET /api/crimeTypes/:crimeTypeId/crimes
   * Obtiene los crímenes de un tipo específico
   */
  def getCrimesByType(crimeTypeId: String): Action[AnyContent] = Action.async {
    crimeTypeService.getCrimesByType(crimeTypeId).map { crimes =>
      Ok(Json.toJson(crimes))
    }.recover {
      case e: Exception =>
        InternalServerError(Json.obj("error" -> e.getMessage))
    }
  }

  /**
   * DELETE /api/crimeTypes/:id
   * Elimina un tipo de crimen
   */
  def deleteCrimeType(id: String): Action[AnyContent] = Action.async {
    crimeTypeService.deleteCrimeType(id).map { deleted =>
      if (deleted) {
        Ok(Json.obj("message" -> s"CrimeType '$id' eliminado"))
      } else {
        NotFound(Json.obj("error" -> s"CrimeType con id '$id' no encontrado"))
      }
    }.recover {
      case e: Exception =>
        InternalServerError(Json.obj("error" -> e.getMessage))
    }
  }

  /**
   * DELETE /api/crimes/:id
   * Elimina un crimen
   */
  def deleteCrime(id: String): Action[AnyContent] = Action.async {
    crimeTypeService.deleteCrime(id).map { deleted =>
      if (deleted) {
        Ok(Json.obj("message" -> s"Crime '$id' eliminado"))
      } else {
        NotFound(Json.obj("error" -> s"Crime con id '$id' no encontrado"))
      }
    }.recover {
      case e: Exception =>
        InternalServerError(Json.obj("error" -> e.getMessage))
    }
  }
}
