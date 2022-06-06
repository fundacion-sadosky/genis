package controllers

import java.io.FileInputStream
import java.nio.file.Files
import scala.io.BufferedSource
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import javax.inject.Singleton
import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import play.api.libs.json.Format
import play.api.libs.json.JsError
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.__
import play.api.libs.json._
import play.api.libs.functional.syntax._
import probability.ProbabilityModel
import stats.PopulationBaseFrequencyService
import stats.PopBaseFreqResult
import stats.Fmins
import play.api.mvc.BodyParsers

@Singleton
class PopulationBaseFreq @Inject() (populationBaseFrequencyService: PopulationBaseFrequencyService) extends Controller {

  def setBaseAsDefault(name: String) = Action.async {
    populationBaseFrequencyService.setAsDefault(name) map {
      result => Ok(Json.toJson(result))
    }
  }

  def getAllBaseNames = Action.async {
    populationBaseFrequencyService.getAllNames map {
      names => Ok(Json.toJson(names))
    }
  }

  def toggleStateBase(name: String) = Action.async { request =>
    populationBaseFrequencyService.toggleStateBase(name) map { data =>
      Ok(Json.toJson(data))
    }
  }

  def getByName(name: String) = Action.async { request =>
    populationBaseFrequencyService.getByNamePV(name) map { data =>
      Ok(Json.toJson(data))
    }
  }

  def insertFmin(id: String) = Action.async(BodyParsers.parse.json) { request =>

    val fmins = request.body.validate[Fmins]

    fmins.fold(err => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(err)))),

      fmin => populationBaseFrequencyService.insertFmin(id, fmin) map { r => Ok(Json.toJson(r)) })
  }

  def uploadPopulationFile = Action.async(parse.multipartFormData) { request =>
    val name: Option[String] = request.body.dataParts.get("baseName") match {
      case Some(parameterList) => Some(parameterList.head)
      case _                   => None
    }

    val model: Option[String] = request.body.dataParts.get("baseModel") match {
      case Some(parameterList) => Some(parameterList.head)
      case _                   => None
    }

    val theta: Option[Double] = request.body.dataParts.get("baseTheta") match {
      case Some(parameterList) => Some(parameterList.head.toDouble)
      case _                   => None
    }

    if (name.isEmpty || theta.isEmpty) {
      Future.successful(BadRequest(Json.obj("status" -> "KO",
        "message" -> ("Missing Parameters: " + (if (name.isEmpty) " name " else "") + (if (theta.isEmpty) " theta " else "")))))
    } else {
      val optionFile = request.body.file("file")
      optionFile.map { csvFile =>

        val fileType = Files.probeContentType(csvFile.ref.file.toPath)

        if (fileType == "text/plain") {

          populationBaseFrequencyService.parseFile(name.get, theta.get, ProbabilityModel.withName(model.get), csvFile.ref.file) map {
            r => Ok(Json.toJson(r)).withHeaders("X-CREATED-ID" -> name.get)
          }

        } else {
          Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Bad File Type")))
        }
      }.getOrElse {
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Missing File")))
      }
    }
  }

  def getAllBasesCharacteristics = Action.async {
    populationBaseFrequencyService.getAllNames map {
      names =>
        val map = names.map(bd => bd.name -> bd).toMap
        Ok(Json.toJson(map))
    }
  }

}
