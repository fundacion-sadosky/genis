package controllers

import jakarta.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import profiledata.{ProfileDataAttempt, ProfileDataService}
import types.SampleCode

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProtoProfileDataController @Inject()(
    cc: ControllerComponents,
    // TODO: bind @Named("stashed") ProfileDataService when stash profiledata service is migrated
    protoProfiledataService: ProfileDataService
)(implicit ec: ExecutionContext) extends AbstractController(cc):

  def isEditable(sampleCode: SampleCode): Action[AnyContent] = Action.async { _ =>
    Future.successful(Ok(Json.toJson(true)))
  }

  def create = Action.async(parse.json) { request =>
    request.body.validate[ProfileDataAttempt].fold(
      errors       => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      profileData  =>
        protoProfiledataService.create(profileData).map {
          case Left(error)       => BadRequest(Json.obj("status" -> "KO", "message" -> error))
          case Right(sampleCode) => Ok(Json.obj("sampleCode" -> sampleCode)).withHeaders("X-CREATED-ID" -> sampleCode.text)
        }
    )
  }

  def update(globalCode: SampleCode) = Action.async(parse.json) { request =>
    request.body.validate[ProfileDataAttempt].fold(
      errors       => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      profileData  => protoProfiledataService.updateProfileData(globalCode, profileData).map(result => Ok(Json.toJson(result)))
    )
  }

  def getResources(imageType: String, id: Long): Action[AnyContent] = Action.async { _ =>
    protoProfiledataService.getResource(imageType, id).map {
      case None        => NotFound
      case Some(bytes) => Ok(bytes)
    }
  }

  def getByCode(sampleCode: SampleCode): Action[AnyContent] = Action.async { _ =>
    protoProfiledataService.get(sampleCode).map {
      case Some(profileData) => Ok(Json.toJson(profileData))
      case None              => NotFound
    }
  }
