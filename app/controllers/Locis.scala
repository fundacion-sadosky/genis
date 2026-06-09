package controllers

import javax.inject.{Inject, Singleton}
import kits.{FullLocus, Locus, LocusLink, LocusService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc.{Action, BodyParsers, Controller, MultipartFormData, Result}

import scala.concurrent.Future

@Singleton
class Locis @Inject() (locusService: LocusService) extends Controller {

  def add = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[FullLocus]

    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      fullLocus => {
          locusService.add(fullLocus) map {
            case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
            case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
          }
      })
  }
  def update = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[FullLocus]

    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      locus => {
        locusService.update(locus) map {
          case Right(()) => Ok(Json.toJson(locus.locus.id)).withHeaders("X-CREATED-ID" -> locus.locus.id)
          case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
        }
      })
  }
  def listFull = Action.async {
    locusService.listFull map { locus =>
      Ok(Json.toJson(locus))
    }
  }

  def list = Action.async {
    locusService.list map { locus =>
      Ok(Json.toJson(locus))
    }
  }

  def delete(id: String) = Action.async {
    locusService.delete(id) map {
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
          case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
        }
  }

  def ranges = Action.async {
    Future.successful(locusService.locusRangeMap) map { locusRanges =>
      Ok(Json.toJson(locusRanges))
    }
  }

  def export = Action.async {
    locusService.listFull map { loci =>
      val json = Json.toJson(loci)
      Ok(json).as("application/json").withHeaders("Content-Disposition" -> "attachment; filename=locus.json")
    }
  }

  def importLocus:  Action[MultipartFormData[play.api.libs.Files.TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    request.body.file("file").map { file =>
      // Guardar archivo temporalmente
      val path = new java.io.File("/tmp/" + file.filename)
      file.ref.moveTo(path, replace = true)

      // Leer contenido del archivo
      val source = scala.io.Source.fromFile(path, "UTF-8")
      val jsonString = try source.mkString finally source.close()

      // Parsear JSON a lista de locus
      Json.parse(jsonString).validate[List[FullLocus]] match {
        case JsSuccess(importedLocus, _) =>
          // Proceso de importación: eliminar categorías existentes y añadir las nuevas
          processImportLocus(importedLocus)
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
  private def processImportLocus(importedLocus: List[FullLocus]): Future[Result] = {

    // 1. Obtener los Kits existentes
    val existingLocusFuture = locusService.list()

    existingLocusFuture.flatMap { existingLocus =>
      // 2. Eliminar Locus existentes
      val deleteFutures = existingLocus.map(_.id).map { locusId =>
        locusService.delete(locusId)
      }

      Future.sequence(deleteFutures).flatMap { deleteResults =>
        // Verificar errores de eliminación
        val deleteErrors = deleteResults.collect { case Left(error) => error }

        if (deleteErrors.nonEmpty) {
          Future.successful(InternalServerError(Json.obj(
            "status" -> "error",
            "message" -> "Error al eliminar Locus existentes",
            "details" -> deleteErrors
          )))
        } else {
          // 3. Agregar nuevas categorías
          val addFutures = importedLocus.map { locusRow =>
            val fullLocus = FullLocus(
              locus = locusRow.locus,
              alias = locusRow.alias,
              links = locusRow.links.map { link =>
                LocusLink(
                  locus = link.locus,
                  factor = link.factor,
                  distance = link.distance
                )
              }
            )
            locusService.add(fullLocus)
          }

          Future.sequence(addFutures).map { addResults =>
            // Verificar errores de adición
            val addErrors = addResults.collect { case Left(error) => error }

            if (addErrors.nonEmpty) {
              InternalServerError(Json.obj(
                "status" -> "error",
                "message" -> "Error al importar locus",
                "details" -> addErrors
              ))
            } else {
              // Importación exitosa
              Ok(Json.obj(
                "status" -> "success",
                "message" -> "Importación de locus exitosa",
                "count" -> importedLocus.size
              ))
            }
          }
        }
      }
    }
  }


}
