package controllers

import javax.inject.{Inject, Singleton}

import pedigree.PedigreeMatchingParameterService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.Future

@Singleton
class PedigreeMatchingParameterController @Inject() (
  pedigreeMatchingParameterService: PedigreeMatchingParameterService
) extends Controller {

  def get() = Action.async {
    pedigreeMatchingParameterService.getMaxMendelianExclusions.map {
      value => Ok(Json.obj("maxMendelianExclusions" -> value))
    }
  }

  def update() = Action.async(BodyParsers.parse.json) { request =>
    (request.body \ "maxMendelianExclusions").asOpt[Int] match {
      case Some(value) if value >= 0 =>
        pedigreeMatchingParameterService.setMaxMendelianExclusions(value).map {
          updated => Ok(Json.obj("maxMendelianExclusions" -> updated))
        }
      case _ =>
        Future.successful(
          BadRequest(Json.obj("message" -> "maxMendelianExclusions debe ser un entero mayor o igual a 0"))
        )
    }
  }

}