package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.{JsError, Json}
import kits._

import java.nio.file.Files
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LocusController @Inject()(
  cc: ControllerComponents,
  locusService: LocusService
)(implicit ec: ExecutionContext) extends AbstractController(cc):

  def add: Action[FullLocus] = Action.async(parse.json[FullLocus]) { request =>
    locusService.add(request.body).map {
      case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
      case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
    }
  }

  def update: Action[FullLocus] = Action.async(parse.json[FullLocus]) { request =>
    locusService.update(request.body).map {
      case Right(()) => Ok(Json.toJson(request.body.locus.id)).withHeaders("X-CREATED-ID" -> request.body.locus.id)
      case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
    }
  }

  def listFull: Action[AnyContent] = Action.async {
    locusService.listFull().map(loci => Ok(Json.toJson(loci)))
  }

  def list: Action[AnyContent] = Action.async {
    locusService.list().map(loci => Ok(Json.toJson(loci)))
  }

  def delete(id: String): Action[AnyContent] = Action.async {
    locusService.delete(id).map {
      case Right(deletedId) => Ok(Json.toJson(deletedId)).withHeaders("X-CREATED-ID" -> deletedId)
      case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
    }
  }

  def ranges: Action[AnyContent] = Action.async {
    locusService.locusRangeMap().map(rangeMap => Ok(Json.toJson(rangeMap)))
  }

  def exportLocus: Action[AnyContent] = Action.async {
    locusService.listFull().map { loci =>
      Ok(Json.toJson(loci)).as("application/json")
        .withHeaders("Content-Disposition" -> "attachment; filename=locus.json")
    }
  }

  def importLocus: Action[MultipartFormData[play.api.libs.Files.TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    request.body.file("file").map { file =>
      val jsonString = Files.readString(file.ref.path)

      Json.parse(jsonString).validate[List[FullLocus]].fold(
        errors => Future.successful(BadRequest(Json.obj(
          "status" -> "error",
          "message" -> "Error en el formato JSON",
          "details" -> JsError.toJson(errors)
        ))),
        importedLocus => processImportLocus(importedLocus)
      )
    }.getOrElse {
      Future.successful(BadRequest(Json.obj(
        "status" -> "error",
        "message" -> "No se encontró ningún archivo"
      )))
    }
  }

  private def processImportLocus(importedLocus: List[FullLocus]): Future[Result] =
    locusService.list().flatMap { existingLocus =>
      val deleteFutures = existingLocus.map(_.id).map(locusService.delete)

      Future.sequence(deleteFutures).flatMap { deleteResults =>
        val deleteErrors = deleteResults.collect { case Left(error) => error }

        if deleteErrors.nonEmpty then
          Future.successful(InternalServerError(Json.obj(
            "status" -> "error",
            "message" -> "Error al eliminar Locus existentes",
            "details" -> deleteErrors
          )))
        else
          val addFutures = importedLocus.map(locusService.add)

          Future.sequence(addFutures).map { addResults =>
            val addErrors = addResults.collect { case Left(error) => error }

            if addErrors.nonEmpty then
              InternalServerError(Json.obj(
                "status" -> "error",
                "message" -> "Error al importar locus",
                "details" -> addErrors
              ))
            else
              Ok(Json.obj(
                "status" -> "success",
                "message" -> "Importación de locus exitosa",
                "count" -> importedLocus.size
              ))
          }
      }
    }
