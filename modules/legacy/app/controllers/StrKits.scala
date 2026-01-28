package controllers

import javax.inject.Inject
import javax.inject.Singleton
import configdata.Group
import kits.{FullStrKit, StrKit, StrKitService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{Action, BodyParsers, Controller, MultipartFormData, Result}
import types.AlphanumericId

import scala.concurrent.Future
import models.Tables.StrkitRow


@Singleton
class StrKits @Inject() (strKitService: StrKitService) extends Controller {

  def list = Action.async {
    strKitService.list map { kits => Ok(Json.toJson(kits)) }
  }

  def listFull = Action.async {
    strKitService.listFull map { kits => Ok(Json.toJson(kits)) }
  }

  def get(id: String) = Action.async {
    strKitService.get(id) map { kit =>
      kit match {
        case Some(kit) => Ok(Json.toJson(kit))
        case None => NoContent
      }
    }
  }

  def getFull(id: String) = Action.async {
    strKitService.getFull(id) map { kit =>
      kit match {
        case Some(kit) => Ok(Json.toJson(kit))
        case None => NoContent
      }
    }
  }

  def findLociByKit(idKit: String) = Action.async { request =>
    strKitService.findLociByKit(idKit) map { loci =>
      Ok(Json.toJson(loci))
    }
  }

  def add = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[FullStrKit]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      kit => {
        strKitService.add(kit) map {
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
          case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
        }
      })
  }

  def update = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[FullStrKit]
    input.fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      kit => strKitService.update(kit).map(result => result match {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
        case Left(error) => BadRequest(Json.toJson(error))
      })
    )
  }

  def exportKits = Action.async {
    strKitService.listFull map { kits =>
      val json = Json.toJson(kits)
      Ok(json).as("application/json").withHeaders("Content-Disposition" -> "attachment; filename=kits.json")
    }
  }
  
  def importKits:  Action[MultipartFormData[play.api.libs.Files.TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    request.body.file("file").map { file =>
      // Guardar archivo temporalmente
      val path = new java.io.File("/tmp/" + file.filename)
      file.ref.moveTo(path, replace = true)

      // Leer contenido del archivo
      val source = scala.io.Source.fromFile(path, "UTF-8")
      val jsonString = try source.mkString finally source.close()

      // Parsear JSON a lista de 
      Json.parse(jsonString).validate[List[FullStrKit]] match {
        case JsSuccess(importedKits, _) =>
          // Proceso de importación: eliminar categorías existentes y añadir las nuevas
          processImportKits(importedKits)
        case JsError(errors) =>
          Future.successful(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> "Error en el formato JSON",
            "details" -> JsError.toFlatJson(errors)
          )))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj(
        "status" -> "error",
        "message" -> "No se encontró ningún archivo"
      )))
    }
  }

  // Método auxiliar para procesar la importación de kits
  private def processImportKits(importedKits: List[FullStrKit]): Future[Result] = {

    // 1. Obtener los Kits existentes
    val existingKitsFuture = strKitService.list()

    existingKitsFuture.flatMap { existingKits =>
      // 2. Eliminar kits existentes
      val deleteFutures = existingKits.map(_.id).map { kitId =>
        strKitService.delete(kitId)
      }

      Future.sequence(deleteFutures).flatMap { deleteResults =>
        // Verificar errores de eliminación
        val deleteErrors = deleteResults.collect { case Left(error) => error }

        if (deleteErrors.nonEmpty) {
          Future.successful(InternalServerError(Json.obj(
            "status" -> "error",
            "message" -> "Error al eliminar kits existentes",
            "details" -> deleteErrors
          )))
        } else {
          // 3. Agregar nuevos kits
          val addFutures = importedKits.map { kitRow =>
            // Convertir kitRow a kit

            val kit = FullStrKit(
              kitRow.id,
              kitRow.name,
              kitRow.`type`,
              kitRow.locy_quantity,
              kitRow.representative_parameter,
              kitRow.alias,
              kitRow.locus.map { locusRow =>
                kits.NewStrKitLocus(
                  locusRow.locus,
                  locusRow.fluorophore,
                  locusRow.order
                )
              }
            )
            strKitService.add(kit)
          }

          Future.sequence(addFutures).map { addResults =>
            // Verificar errores de adición
            val addErrors = addResults.collect { case Left(error) => error }

            if (addErrors.nonEmpty) {
              InternalServerError(Json.obj(
                "status" -> "error",
                "message" -> "Error al importar kits",
                "details" -> addErrors
              ))
            } else {
              // Importación exitosa
              Ok(Json.obj(
                "status" -> "success",
                "message" -> "Importación de kits exitosa",
                "count" -> importedKits.size
              ))
            }
          }
        }
      }
    }
  }
  
  def delete(id: String) = Action.async {
    strKitService.delete(id) map {
      case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
      case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
    }
  }

}
