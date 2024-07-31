package controllers

import scala.concurrent.Future
import scala.concurrent.duration.MINUTES
import scala.concurrent.duration.SECONDS
import javax.inject.Inject
import javax.inject.Singleton
import models.Tables
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Format
import play.api.libs.json.JsError
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.__
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, BodyParsers, Controller, ResponseHeader, Result, Results}
import profile.ProfileService
import profile.Profile
import profiledata._
import configdata.CategoryService
import types._

@Singleton
class ProfileData @Inject() (
  profiledataService: ProfileDataService,
  profileService: ProfileService,
  categoryService: CategoryService
) extends Controller {

  def update(globalCode: SampleCode): Action[JsValue] = Action.async(BodyParsers.parse.json) {
    request =>
      val profileDataJson = request.body.validate[ProfileDataAttempt]
      profileDataJson
        .fold(
          errors => {
            Future
              .successful(
                BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))
              )
          },
          profileData =>
            profiledataService
              .updateProfileData(globalCode, profileData) map { result => Ok(Json.toJson(result)) }
        )
  }

  def isReadOnly(globalCode: SampleCode): Action[AnyContent] =
    Action.async {
      request => Future.successful(Ok(Json.obj("data" -> true)));
    }

  def modifyCategory(globalCode: SampleCode): Action[JsValue] = Action
    .async(BodyParsers.parse.json) {
      request =>
        val profileDataJson = request.body.validate[ProfileDataAttempt]
        profileDataJson
          .fold(
            errors => {
              Future
                .successful(
                  BadRequest(Json
                    .obj(
                      "status" -> "KO",
                      "message" -> JsError.toFlatJson(errors)
                    )
                  )
                )
            },
            profileData =>
              profiledataService
                .updateProfileCategoryData(globalCode, profileData)
                .map {
                  case None => Right(globalCode)
                  case Some(error) => Left(error)
                }
                .flatMap {
                  case Left(error) => Future.successful(Left(error))
                  case Right(code) =>
                    val x = profileService
                      .get(code)
                      .map {
                        case None => Left(Messages("error.E0101"))
                        case Some(profile) => Right(profile)
                      }
                    x
                }
                .map {
                  x => x.right.map(
                    _.copy(categoryId = profileData.category)
                  )
                }
                .flatMap {
                  case Left(error) => Future.successful(Left(error))
                  case Right(profile) =>
                    try {
                      profileService
                        .updateProfile(profile)
                        .map(_ => Right(profile))
                    } catch {
                      case e: Exception => Future.successful(Left(Messages("error.E0132")))
                    }
                }
                .map {
                  case Left(error) => Json.obj(
                    "status" -> "error",
                    "message" -> error
                  )
                  case Right(_) => Json.obj(
                    "status" -> "OK",
                    "message" -> Messages("success.S0100")
                  )
                }
                .map {
                  result => Ok(result)
                }
          )
    }

  def getByCode(sampleCode: SampleCode) = Action.async { request =>
    profiledataService.get(sampleCode) map { result =>
      result.map { profileData => Ok(Json.toJson(profileData)) }.getOrElse {
        NotFound
      }
    }
  }

  def deleteProfile(profileId: SampleCode) = Action.async(BodyParsers.parse.json) { request =>
    val motive = request.body.validate[DeletedMotive]
    val userId = request.headers.get("X-USER").get

    motive.fold(errors =>
      { Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))) },
      motive => profiledataService.deleteProfile(profileId, motive, userId) map { result =>
        result.fold(error => BadRequest(Json.obj("status" -> "KO", "message" -> error)),
          sampleCode => {
            Ok(Json.toJson(sampleCode))

          })
      })
  }

  def getDeleteMotive(sampleCode: SampleCode) = Action.async {
    profiledataService.getDeleteMotive(sampleCode).map { opt =>
      opt.fold(Results.NoContent)(mot => Ok(Json.toJson(mot)))
    }
  }

  def isEditable(sampleCode: SampleCode) = Action.async { request =>
    profiledataService.isEditable(sampleCode: SampleCode) map {
      result => Ok(Json.toJson(result))
    }
  }

  def create = Action.async(BodyParsers.parse.json) { request =>
    val profileDataJson = request.body.validate[ProfileDataAttempt]

    profileDataJson.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
      },
      profileData => {
        profiledataService.create(profileData).map {
          case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> error))
          case Right(sampleCode) => Ok(Json.obj("sampleCode" -> sampleCode)).withHeaders("X-CREATED-ID" -> sampleCode.text)
        }
      })
  }

  def get(id: Long) = Action.async(BodyParsers.parse.json) { request =>
    profiledataService.get(id) map {
      profileData =>
        Ok(Json.obj(
          "profile" -> profileData._1,
          "category" -> profileData._2))
    }
  }

  def getResources(imageType: String, id: Long) = Action.async { request =>
    profiledataService.getResource(imageType, id) map {
      case None => Results.NotFound
      case Some(bytes) =>
        val fileContent: Enumerator[Array[Byte]] = Enumerator(bytes)
        Result(
          header = ResponseHeader(200),
          body = fileContent)
    }
  }

  def findByCode(globalCode: SampleCode) = Action.async { request =>
    profiledataService.findByCode(globalCode) map { result =>
      result.map { profileData => Ok(Json.toJson(profileData)) }.getOrElse {
        NotFound
      }
    }
  }

  def findByCodes(globalCodes: List[SampleCode]) = Action.async { request =>
    profiledataService.findByCodes(globalCodes) map (l => Ok(Json.toJson(l)))
  }

  def findByCodeWithAssociations(globalCode: SampleCode) = Action.async { request =>
    profiledataService.findByCodeWithAssociations(globalCode) map {
      case Some((profileData, group, category)) =>
        Ok(Json.obj(
          "profileData" -> profileData,
          "group" -> group,
          "category" -> category))
      case None => BadRequest
    }
  }

}
