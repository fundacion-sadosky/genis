package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.{JsError, Json}
import profiledata.{ProfileDataAttempt, ProfileDataService}
import types.SampleCode
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProtoProfileDataController @Inject()(
  cc: ControllerComponents,
  @Named("stashed") protoService: ProfileDataService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def isEditable(sampleCode: SampleCode): Action[AnyContent] = Action.async {
    Future.successful(Ok(Json.toJson(true)))
  }

  def create(): Action[AnyContent] = Action.async(parse.anyContent) { request =>
    request.body.asJson.map(_.validate[ProfileDataAttempt]) match {
      case None =>
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Expected JSON body")))
      case Some(jsResult) =>
        jsResult.fold(
          errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
          profileData =>
            protoService.create(profileData).map {
              case Left(error)       => BadRequest(Json.obj("status" -> "KO", "message" -> error))
              case Right(sampleCode) => Ok(Json.obj("sampleCode" -> sampleCode)).withHeaders("X-CREATED-ID" -> sampleCode.text)
            }
        )
    }
  }

  def update(globalCode: SampleCode): Action[AnyContent] = Action.async(parse.anyContent) { request =>
    request.body.asJson.map(_.validate[ProfileDataAttempt]) match {
      case None =>
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Expected JSON body")))
      case Some(jsResult) =>
        jsResult.fold(
          errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
          profileData =>
            protoService.updateProfileData(globalCode, profileData).map(result => Ok(Json.toJson(result)))
        )
    }
  }

  def getResources(imageType: String, id: Long): Action[AnyContent] = Action.async {
    protoService.getResource(imageType, id).map {
      case None        => NotFound(Json.obj("error" -> s"Resource not found: $imageType/$id"))
      case Some(bytes) => Ok(bytes).as("application/octet-stream")
    }
  }

  def getByCode(sampleCode: SampleCode): Action[AnyContent] = Action.async {
    protoService.get(sampleCode).map {
      case None     => NotFound(Json.obj("error" -> s"Profile not found: ${sampleCode.text}"))
      case Some(pd) => Ok(Json.toJson(pd))
    }
  }
}
