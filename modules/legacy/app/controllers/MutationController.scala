package controllers
import javax.inject.{Inject, Singleton}

import motive.Motive
import pedigree._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.Future
import scala.util.{Left, Right}

@Singleton
class MutationController @Inject()(mutationService: MutationService) extends Controller {
  def getAllMutationModels() = Action.async {
    request => {
      mutationService.getAllMutationModels().map {
        case Nil => NotFound(Json.obj("message" -> "No existe"))
        case models => Ok(Json.toJson(models))
      }
    }
  }
  def getAllMutationDefaultParameters() = Action.async {
    request => {
      mutationService.getAllMutationDefaultParameters().map {
        case Nil => NotFound(Json.obj("message" -> "No existe"))
        case models => Ok(Json.toJson(models))
      }
    }
  }
  def getMutationModelsTypes() = Action.async {
    request => {
      mutationService.getAllMutationModelType().map {
        case Nil => NotFound(Json.obj("message" -> "No existe"))
        case mmt => Ok(Json.toJson(mmt))
      }
    }
  }
  def getActiveMutationModels() = Action.async {
    request => {
      mutationService.getActiveMutationModels().map {
        case Nil => NotFound(Json.obj("message" -> "No existe"))
        case models => Ok(Json.toJson(models))
      }
    }
  }
  def getMutatitionModelParameters(idMutationModel: Long) = Action.async {
    request => {
      mutationService.getMutatitionModelParameters(idMutationModel).map {
        case list => Ok(Json.toJson(list))
      }
    }
  }
  def getMutationModel(idMutationModel: Long) = Action.async {
    request => {
      mutationService.getMutationModel(Some(idMutationModel)).map {
        case None => NotFound
        case list => Ok(Json.toJson(list))
      }
    }
  }
  def deleteMutationModelById(id: Long) = Action.async {
    request => {
      mutationService.deleteMutationModelById(id).map {
        case Left(msg) => NotFound(Json.obj("message" -> msg))
        case Right(()) => Ok
      }
    }
  }
  def insert = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[MutationModelFull]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      row => {
        mutationService.insertMutationModel(row).map {
          case Left(msg) => NotFound(Json.obj("message" -> msg))
          case Right(id) => Ok(Json.obj("id" -> id))
        }
      })
  }

//  def convertMutationModel(parameter:MutationModelFullUpdate):MutationModelFull  = {
//    MutationModelFull(parameter.header,parameter.parameters.map(parameter => {
//      MutationModelParameter(parameter.i1,parameter.i2,parameter.l,parameter.s,parameter.r1,parameter.r2,parameter.r3)
//    }))
//  }
def generateMatrix = Action.async(BodyParsers.parse.json(1024*1024*16)) { request =>
  val input = request.body.validate[MutationModelFull]
  input.fold(
    errors => {
      Future.successful(BadRequest(JsError.toFlatJson(errors)))
    },
    row => {
      mutationService.generateKis(row.header).map {
        case _ => Ok
      }
    })
}
def update = Action.async(BodyParsers.parse.json(1024*1024*16)) { request =>
    val input = request.body.validate[MutationModelFull]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      row => {
        mutationService.updateMutationModel(row).map {
          case Left(msg) => NotFound(Json.obj("message" -> msg))
          case Right(_) => Ok
        }
      })
  }
}
