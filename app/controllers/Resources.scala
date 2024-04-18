package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import services.CacheService
import services.TemporaryAssetKey
import play.api.libs.json.JsString
import java.nio.file.Files

@Singleton
class Resources @Inject() (cache: CacheService) extends Controller {

  def uploadImage = Action(parse.multipartFormData) { request =>

    request.body.dataParts.get("filesId") match {
      case Some(parameterList) => {
        val value = parameterList.head
        val optionFile = request.body.file("file")
        optionFile.map { picture =>

          val imageFile = picture.ref
          val mimeType = Option(Files.probeContentType(imageFile.file.toPath)).getOrElse("")

          if ( mimeType.contains("image") ) {
            cache.get(TemporaryAssetKey(value)).map { imageList =>
              cache.set(TemporaryAssetKey(value), imageList :+ imageFile)
              Ok(imageFile.file.getAbsolutePath)
            }.getOrElse {
              BadRequest(Json.obj("status" -> "KO", "message" -> "Missing File from cache"))
            }
          } else {
            BadRequest(Json.obj("status" -> "KO", "message" -> "Bad File Type"))
          }

        }.getOrElse {
          BadRequest(Json.obj("status" -> "KO", "message" -> "Missing File"))
        }
      }
      case None => BadRequest(Json.obj("status" -> "KO", "message" -> "Missing Parameters"))
    }
  }
  def uploadFile = Action(parse.multipartFormData) { request =>

    request.body.dataParts.get("filesId") match {
      case Some(parameterList) => {
        val value = parameterList.head
        val optionFile = request.body.file("file")
        optionFile.map { file =>

          val fileRef = file.ref

            cache.get(TemporaryAssetKey(value)).map { fileList =>
              cache.set(TemporaryAssetKey(value), fileList :+ fileRef)
              Ok(fileRef.file.getAbsolutePath)
            }.getOrElse {
              BadRequest(Json.obj("status" -> "KO", "message" -> "Missing File from cache"))
            }

        }.getOrElse {
          BadRequest(Json.obj("status" -> "KO", "message" -> "Missing File"))
        }
      }
      case None => BadRequest(Json.obj("status" -> "KO", "message" -> "Missing Parameters"))
    }
  }
  def getFilesId = Action {
    val uuid = java.util.UUID.randomUUID.toString
    val lista = Nil
    cache.set(TemporaryAssetKey(uuid), lista)
    Ok(Json.obj("filesId" -> uuid))
  }

  def temporary(resourceName: String) = Action {
    Ok.sendFile(new java.io.File("/tmp/" + resourceName))
  }
}
