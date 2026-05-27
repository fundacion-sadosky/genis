package controllers

import javax.inject.{Inject, Singleton}
import pedigree.{MutationModelFull, MutationService}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MutationController @Inject() (
  mutationService: MutationService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext) extends AbstractController(cc):

  def getAllMutationModels(): Action[AnyContent] = Action.async {
    mutationService.getAllMutationModels().map {
      case Nil    => NotFound(Json.obj("message" -> "No existe"))
      case models => Ok(Json.toJson(models))
    }
  }

  def getAllMutationDefaultParameters(): Action[AnyContent] = Action.async {
    mutationService.getAllMutationDefaultParameters().map {
      case Nil    => NotFound(Json.obj("message" -> "No existe"))
      case models => Ok(Json.toJson(models))
    }
  }

  def getMutationModelsTypes(): Action[AnyContent] = Action.async {
    mutationService.getAllMutationModelType().map {
      case Nil => NotFound(Json.obj("message" -> "No existe"))
      case mmt => Ok(Json.toJson(mmt))
    }
  }

  def getActiveMutationModels(): Action[AnyContent] = Action.async {
    mutationService.getActiveMutationModels().map {
      case Nil    => NotFound(Json.obj("message" -> "No existe"))
      case models => Ok(Json.toJson(models))
    }
  }

  def getMutatitionModelParameters(idMutationModel: Long): Action[AnyContent] = Action.async {
    mutationService.getMutatitionModelParameters(idMutationModel).map { list =>
      Ok(Json.toJson(list))
    }
  }

  def getMutationModel(idMutationModel: Long): Action[AnyContent] = Action.async {
    mutationService.getMutationModel(Some(idMutationModel)).map {
      case None       => NotFound
      case Some(full) => Ok(Json.toJson(full))
    }
  }

  def deleteMutationModelById(id: Long): Action[AnyContent] = Action.async {
    mutationService.deleteMutationModelById(id).map {
      case Left(msg) => NotFound(Json.obj("message" -> msg))
      case Right(()) => Ok
    }
  }

  def insert: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[MutationModelFull].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      row =>
        mutationService.insertMutationModel(row).map {
          case Left(msg) => NotFound(Json.obj("message" -> msg))
          case Right(id) => Ok(Json.obj("id" -> id))
        }
    )
  }

  def generateMatrix: Action[JsValue] = Action.async(parse.json(1024 * 1024 * 16)) { request =>
    request.body.validate[MutationModelFull].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      row => mutationService.generateKis(row.header).map(_ => Ok)
    )
  }

  def update: Action[JsValue] = Action.async(parse.json(1024 * 1024 * 16)) { request =>
    request.body.validate[MutationModelFull].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      row =>
        mutationService.updateMutationModel(row).map {
          case Left(msg) => NotFound(Json.obj("message" -> msg))
          case Right(_)  => Ok
        }
    )
  }
