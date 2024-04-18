package controllers

import javax.inject.Singleton
import javax.inject.Inject
import configdata.CountryService
import play.api.mvc.Controller
import play.api.mvc.Action
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.BodyParsers
import scala.concurrent.Future
import play.api.libs.json.JsError
import laboratories.LaboratoryService
import laboratories.Laboratory

@Singleton
class Laboratories @Inject() (labService: LaboratoryService, countryService: CountryService) extends Controller {

  def list = Action.async {
    labService.list map { labs =>
      Ok(Json.toJson(labs))
    }
  }

  def listDescriptive = Action.async {
    labService.listDescriptive map { labs =>
      Ok(Json.toJson(labs))
    }
  }

  def listCountries = Action.async {
    countryService.listCountries map {
      countries => Ok(Json.toJson(countries))
    }
  }

  def listProvinces(country: String) = Action.async {
    countryService.listProvinces(country) map {
      provs => Ok(Json.toJson(provs))
    }
  }

  def addLab = Action.async(BodyParsers.parse.json) { request =>
    val lab = request.body.validate[Laboratory]

    lab.fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      laboratory => labService.add(laboratory).map(result => result match {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
        case Left(error) => BadRequest(Json.toJson(error))
      })
    )

  }

  def getLaboratory(laboratoryId: String) = Action.async {
    labService.get(laboratoryId) map { laboratory =>
      laboratory match {
        case Some(lab) => Ok(Json.toJson(lab))
        case None => NoContent
      }
    }
  }

  def updateLab = Action.async(BodyParsers.parse.json) { request =>
    val lab = request.body.validate[Laboratory]

    lab.fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      laboratory => labService.update(laboratory).map(result => result match {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
        case Left(error) => BadRequest(Json.toJson(error))
      })
    )
  }
}