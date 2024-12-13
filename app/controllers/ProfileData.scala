package controllers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, MINUTES, SECONDS}
import javax.inject.Inject
import javax.inject.Singleton
import models.Tables
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{Format, JsError, JsPath, JsValue, Json, Reads, Writes, __}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, BodyParsers, Controller, ResponseHeader, Result, Results}
import profile.ProfileService
import profile.Profile
import profiledata._
import configdata.CategoryService
import matching.MatchingService
import connections.InterconnectionService
import play.api.data.validation.ValidationError
import types._

@Singleton
class ProfileData @Inject() (
  profiledataService: ProfileDataService,
  profileService: ProfileService,
  categoryService: CategoryService,
  matchingService: MatchingService,
  interconnectionService: InterconnectionService
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

  def modifyCategory(
    globalCode: SampleCode,
    replicate: Boolean
  ): Action[JsValue] = Action
    .async(BodyParsers.parse.json) {
      request =>
        val profileDataJson = request.body.validate[ProfileDataAttempt]
        val errorWhileReadingBody = (error: Seq[(JsPath, Seq[ValidationError])]) =>
          Future
            .successful(
              BadRequest(Json
                .obj(
                  "status" -> "KO",
                  "message" -> JsError.toFlatJson(error)
                )
              )
            )
        val getProfileId = (updateResult: Option[String]) => {
          updateResult
            match {
              case None => Right(globalCode)
              case Some(error) => Left(error)
          }
        }
        val getProfile = (profileIdOrError: Either[String, SampleCode]) => {
          profileIdOrError
            match {
              case Left(error) => Future
                .successful(Left(error))
              case Right(profileId) =>
                profileService
                  .get(profileId)
                  .map {
                    case None => Left(Messages("error.E0101"))
                    case Some(profile) => Right(profile)
                  }
            }
        }
        val copyAndModifyProfile = (profileData: ProfileDataAttempt) =>
          (profileOrError: Either[String, Profile]) => {
            profileOrError
              .right
              .map( _.copy(categoryId = profileData.category))
          }
        val updateProfile = (newProfileOrError: Either[String, Profile]) => {
          newProfileOrError match {
            case Left(error) => Future
              .successful(Left(error))
            case Right(prof) =>
              try {
                profileService
                  .updateProfile(prof)
                  .map(_ => Right(prof))
              } catch {
                case e: Exception => Future
                  .successful(Left(Messages("error.E0132")))
              }
          }
        }
        val launchFindMatches = (profileData: ProfileDataAttempt) =>
          (profileOpt: Option[Profile]) => {
            profileOpt match {
              case None => Left(Messages("error.E0101"))
              case Some(p) =>
                matchingService
                  .findMatches(
                    p.globalCode,
                    categoryService
                      .getCategory(profileData.category)
                      .flatMap(
                        cat => categoryService
                          .getCategoryTypeFromFullCategory(cat)
                      )
                  )
                Right(p)
            }
          }
        val uploadModifiedProfile:
          PartialFunction[Either[String, Profile], Unit] = {
          case findMatchesResult =>
            findMatchesResult.right.map {
              p =>
                profiledataService
                  .get(globalCode)
                  .map {
                    case Some(pdata) =>
                      interconnectionService
                        .uploadProfileToSuperiorInstance(p, pdata)
                  }
            }
        }
        val findMatchestoJson =
          (findMatchesResult: Either[String, Profile]) => {
            findMatchesResult match {
              case Left(error) => Json
                .obj(
                  "status" -> "error",
                  "message" -> error
                )
              case Right(p) => Json
                .obj(
                  "status" -> "OK",
                  "message" -> Messages("success.S0602", p.globalCode.text)
                )
            }
          }
        val updateCategoryInProfile = (profileData: ProfileDataAttempt) => {
          val matchAndUpload = (profileOrError: Either[String, Profile]) => {
            profileOrError match {
              case Left(error) => Future
                .successful(
                  Json.arr(
                    Json.obj( "status" -> "error", "message" -> error )
                  )
                )
              case Right(prof) =>
                val futFindMatches = profileService
                  .get(globalCode)
                  .map(launchFindMatches(profileData))
                val findMatchesResult = futFindMatches
                  .map(findMatchestoJson)
                if (replicate) {
                  futFindMatches
                    .onSuccess(uploadModifiedProfile)
                }
                findMatchesResult
                  .map(
                    result =>
                      Json
                        .arr(
                          Json.obj(
                            "status" -> "OK",
                            "message" -> Messages("success.S0100", prof.globalCode.text)
                          ),
                          result
                        )
                  )
            }
          }
          profiledataService
            .updateProfileCategoryData(globalCode, profileData)
            .map(getProfileId)
            .flatMap(getProfile)
            .map(copyAndModifyProfile(profileData))
            .flatMap(updateProfile)
            .flatMap(matchAndUpload)
            .map(Ok(_))
        }
        profileDataJson
          .fold(
            errorWhileReadingBody,
            updateCategoryInProfile
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

  def findByCodes(
    globalCodes: List[SampleCode]
  ): Action[AnyContent] = Action.async { request =>
    profiledataService
      .findByCodes(globalCodes)
      .map(
        l => Ok(Json.toJson(l))
      )
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
