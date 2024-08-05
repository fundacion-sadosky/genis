package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc._
import types.SampleCode
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import profiledata.ProfileDataService
import profiledata.ProfileDataAttempt
import play.api.libs.iteratee.Enumerator
import javax.inject.Named

class ProtoProfileData @Inject() (@Named("stashed") protoProfiledataService: ProfileDataService) extends Controller with JsonActions {

  def isEditable(sampleCode: SampleCode) = Action.async { request =>
    Future(Ok(Json.toJson(true)))
  }

  def create = Action.async(BodyParsers.parse.json) { request =>
    val profileDataJson = request.body.validate[ProfileDataAttempt]

    profileDataJson.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
      },
      profileData => {
        protoProfiledataService.create(profileData).map {
          case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> error))
          case Right(sampleCode) => Ok(Json.obj("sampleCode" -> sampleCode)).withHeaders("X-CREATED-ID" -> sampleCode.text)
        }
      })
  }

  def update(globalCode: SampleCode) = Action.async(BodyParsers.parse.json) { request =>
    val profileDataJson = request.body.validate[ProfileDataAttempt]

    profileDataJson.fold(errors => { Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))) },
      profileData => protoProfiledataService.updateProfileData
        
      (globalCode, profileData) map { result => Ok(Json.toJson(result)) })
  }

  def getResources(imageType: String, id: Long) = Action.async { request =>
    protoProfiledataService.getResource(imageType, id) map { image =>
      image match {
        case None => Results.NotFound
        case Some(bytes) =>
          val fileContent: Enumerator[Array[Byte]] = Enumerator(bytes)
          Result(
            header = ResponseHeader(200),
            body = fileContent)
      }
    }
  }

  def getByCode(sampleCode: SampleCode) = Action.async { request =>
    protoProfiledataService.get(sampleCode) map { result =>
      result.map { profileData => Ok(Json.toJson(profileData)) }.getOrElse {
        NotFound
      }
    }
  }

}