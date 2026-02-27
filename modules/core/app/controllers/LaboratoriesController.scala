package controllers

import javax.inject.Singleton
import javax.inject.Inject
import play.api.mvc.{AbstractController, ControllerComponents, Action}
import play.api.libs.json.{Json, JsError}
import scala.concurrent.{ExecutionContext, Future}
import services.LaboratoryService
import services.CountryService
import types.Laboratory

@Singleton
class LaboratoriesController @Inject() (
    cc: ControllerComponents,
    labService: LaboratoryService,
    countryService: CountryService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def list = Action.async {
    labService.list() map { labs =>
      Ok(Json.toJson(labs))
    }
  }

  def listDescriptive = Action.async {
    labService.listDescriptive() map { labs =>
      Ok(Json.toJson(labs))
    }
  }

  def listCountries = Action.async {
    countryService.listCountries map { countries =>
      Ok(Json.toJson(countries))
    }
  }

  def listProvinces(country: String) = Action.async {
    countryService.listProvinces(country) map { provs =>
      Ok(Json.toJson(provs))
    }
  }

  def addLab = Action.async(parse.json) { request =>
    request.body.validate[Laboratory].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      laboratory => labService.add(laboratory).map {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
        case Left(error) => BadRequest(Json.toJson(error))
      }
    )
  }

  def getLaboratory(laboratoryId: String) = Action.async {
    labService.get(laboratoryId) map {
      case Some(lab) => Ok(Json.toJson(lab))
      case None => NoContent
    }
  }

  def updateLab = Action.async(parse.json) { request =>
    request.body.validate[Laboratory].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      laboratory => labService.update(laboratory).map {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
        case Left(error) => BadRequest(Json.toJson(error))
      }
    )
  }
}
