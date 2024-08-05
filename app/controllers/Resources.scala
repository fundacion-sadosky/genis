package controllers

import play.api.libs.Files.TemporaryFile

import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.{Action, AnyContent, Controller, MultipartFormData, Result}
import services.CacheService
import services.TemporaryAssetKey
import play.api.libs.json.JsString

import java.nio.file.Files

@Singleton
class Resources @Inject() (cache: CacheService) extends Controller {

  private def badRequestWithMessage(message:String): Result = {
    BadRequest(
      Json.obj( "status" -> "KO", "message" -> message )
    )
  }

  def uploadImage: Action[MultipartFormData[TemporaryFile]] = Action(parse.multipartFormData) {
    request =>
      val filesIds: Option[Seq[String]] = request.body.dataParts.get("filesId")
      filesIds
        .toRight("Missing Parameters").right
        .flatMap(
          parameterList => {
            request
              .body
              .file(key = "file")
              .map(picture => (parameterList.head, picture.ref))
              .toRight("Missing File");
          }
        ).right
        .flatMap {
          case (value, imageFile) =>
            Option(Files.probeContentType(imageFile.file.toPath))
              .flatMap( mimeType => if (mimeType.contains("image")) Some((value, imageFile)) else None)
              .toRight("Bad File Type")
        }.right
        .flatMap {
          case (value, imageFile) =>
            cache
              .get(TemporaryAssetKey(value))
              .map(imageList=>(value, imageList, imageFile))
              .toRight("Missing File from cache")
        }
        .fold(
          badRequestWithMessage,
          {
            case (value, imageList, imageFile) =>
              cache.set(TemporaryAssetKey(value), imageList :+ imageFile)
              Ok(imageFile.file.getAbsolutePath)
          }
        )
  }

  def uploadFile: Action[MultipartFormData[TemporaryFile]] = Action(parse.multipartFormData) {
    request =>
      val filesIds: Option[Seq[String]] = request
        .body
        .dataParts
        .get("filesId")
      filesIds
        .toRight("Missing Parameters")
        .right.flatMap(
          parameterList => {
            request
              .body
              .file("file")
              .map(file => (parameterList.head, file.ref))
              .toRight("Missing File")
          }
        )
        .right.flatMap {
          case (value, fileRef) =>
            cache
              .get(TemporaryAssetKey(value))
              .map(fileList => (value, fileList, fileRef))
              .toRight("Missing File from cache")
        }
        .fold(
          badRequestWithMessage,
          {
            case (value, fileList, fileRef) =>
              cache.set(TemporaryAssetKey(value), fileList :+ fileRef)
              Ok(fileRef.file.getAbsolutePath)
          }
        )
  }

  def getFilesId: Action[AnyContent] = Action {
    val uuid = java.util.UUID.randomUUID.toString
    val lista = Nil
    cache.set(TemporaryAssetKey(uuid), lista)
    Ok(Json.obj("filesId" -> uuid))
  }

  def temporary(resourceName: String): Action[AnyContent] = Action {
    Ok.sendFile(new java.io.File("/tmp/" + resourceName))
  }
}
