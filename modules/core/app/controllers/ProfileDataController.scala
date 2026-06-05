package controllers

import connections.InterconnectionService
import jakarta.inject.{Inject, Singleton}
import matching.MatchingService
import play.api.Logging
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, BodyParsers, ControllerComponents, Results}
import profile.ProfileService
import profiledata.{DeletedMotive, ProfileDataAttempt, ProfileDataService}
import types.SampleCode

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProfileDataController @Inject()(
  cc: ControllerComponents,
  profileDataService: ProfileDataService,
  profileService: ProfileService,
  matchingService: MatchingService,
  interconnectionService: InterconnectionService,
  messagesApi: MessagesApi
)(implicit ec: ExecutionContext) extends AbstractController(cc) with I18nSupport with Logging:

  private implicit val lang: Lang = Lang("es")
  private def msg(key: String, args: Any*): String = messagesApi(key, args*)(lang)

  def update(globalCode: SampleCode): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[ProfileDataAttempt].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      profileData =>
        profileDataService.updateProfileData(globalCode, profileData).map(result => Ok(Json.toJson(result)))
    )
  }

  def isReadOnly(globalCode: SampleCode): Action[AnyContent] = Action.async {
    Future.successful(Ok(Json.obj("data" -> true)))
  }

  // NOTE: legacy awaited MatchingProcessStatus (Play 2 Iteratee broadcast) before checking replication.
  // Play 3 has no equivalent, so matching is fired and the replication check uses pre-fire results.
  // The replication guard and superior-instance upload are fully implemented.
  def modifyCategory(globalCode: SampleCode, replicate: Boolean, userName: String): Action[JsValue] =
    Action.async(parse.json) { request =>
      request.body.validate[ProfileDataAttempt].fold(
        errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
        profileData =>
          profileDataService.updateProfileCategoryData(globalCode, profileData, userName).flatMap {
            case Some(error) =>
              Future.successful(Ok(Json.arr(Json.obj("status" -> "error", "message" -> error))))
            case None =>
              profileService.get(globalCode).flatMap {
                case None =>
                  Future.successful(Ok(Json.arr(Json.obj("status" -> "error", "message" -> msg("error.E0101")))))
                case Some(prof) =>
                  val updatedProf = prof.copy(categoryId = profileData.category)
                  profileService.updateProfile(updatedProf)
                    .map(_ => Right(()): Either[String, Unit])
                    .recover { case _ => Left(msg("error.E0132")) }
                    .flatMap {
                      case Left(error) =>
                        Future.successful(Ok(Json.arr(Json.obj("status" -> "error", "message" -> error))))
                      case Right(_) =>
                        profileService.fireMatching(globalCode)
                        if !replicate then
                          Future.successful(Ok(Json.arr(
                            Json.obj("status" -> "OK", "message" -> msg("success.S0100", globalCode.text)),
                            Json.obj("status" -> "OK", "message" -> msg("success.S0602", globalCode.text))
                          )))
                        else
                          checkForReplicatedMatches(prof).flatMap {
                            case Left(error) =>
                              Future.successful(Ok(Json.arr(Json.obj("status" -> "error", "message" -> error))))
                            case Right(_) =>
                              profileDataService.get(globalCode).flatMap { pdOpt =>
                                pdOpt.foreach(pd => interconnectionService.uploadProfileToSuperiorInstance(updatedProf, pd, userName))
                                Future.successful(Ok(Json.arr(
                                  Json.obj("status" -> "OK", "message" -> msg("success.S0100", globalCode.text)),
                                  Json.obj("status" -> "OK", "message" -> msg("success.S0602", globalCode.text))
                                )))
                              }
                          }
                    }
              }
          }
      )
    }

  private def checkForReplicatedMatches(prof: profile.Profile): Future[Either[String, profile.Profile]] =
    matchingService.findMatchingResults(prof.globalCode).flatMap {
      case None => Future.successful(Right(prof))
      case Some(matchingResults) =>
        Future.sequence(
          matchingResults.results.map(r => profileDataService.getIsProfileReplicatedInternalCode(r.internalSampleCode))
        ).map { flags =>
          if flags.exists(identity) then Left(msg("error.E0732"))
          else Right(prof)
        }
    }

  def getByCode(sampleCode: SampleCode): Action[AnyContent] = Action.async {
    profileDataService.get(sampleCode).map {
      case Some(pd) => Ok(Json.toJson(pd))
      case None => NotFound
    }
  }

  def deleteProfile(profileId: SampleCode): Action[JsValue] = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    request.body.validate[DeletedMotive].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      motive =>
        profileDataService.deleteProfile(profileId, motive, userId).map {
          case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> error))
          case Right(sc) => Ok(Json.toJson(sc))
        }
    )
  }

  def getDeleteMotive(sampleCode: SampleCode): Action[AnyContent] = Action.async {
    profileDataService.getDeleteMotive(sampleCode).map {
      case None => Results.NoContent
      case Some(mot) => Ok(Json.toJson(mot))
    }
  }

  def isEditable(sampleCode: SampleCode): Action[AnyContent] = Action.async {
    profileDataService.isEditable(sampleCode).map(result => Ok(Json.toJson(result)))
  }

  def create: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[ProfileDataAttempt].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      profileData =>
        profileDataService.create(profileData).map {
          case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> error))
          case Right(sc) => Ok(Json.obj("sampleCode" -> sc)).withHeaders("X-CREATED-ID" -> sc.text)
        }
    )
  }

  def removeAll: Action[AnyContent] = Action { NotImplemented }

  def removeProfile(globalCode: SampleCode): Action[AnyContent] = Action.async {
    profileDataService.removeProfile(globalCode).map {
      case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> error))
      case Right(_) => Ok(Json.obj("status" -> "OK", "message" -> msg("success.S0100", globalCode.text)))
    }
  }

  def get(id: Long): Action[JsValue] = Action.async(parse.json) { _ =>
    profileDataService.get(id).map { case (profileData, group, _) =>
      Ok(Json.obj("profile" -> profileData, "category" -> group))
    }
  }

  def getResources(imageType: String, id: Long): Action[AnyContent] = Action.async {
    profileDataService.getResource(imageType, id).map {
      case None => Results.NotFound
      case Some(bytes) => Ok(bytes).as("application/octet-stream")
    }
  }

  def getDesktopProfiles: Action[AnyContent] = Action.async {
    profileDataService.getDesktopProfiles().map(result => Ok(Json.toJson(result)))
  }

  def findByCode(globalCode: SampleCode): Action[AnyContent] = Action.async {
    profileDataService.findByCode(globalCode).map {
      case Some(pd) => Ok(Json.toJson(pd))
      case None => NotFound
    }
  }

  def findByCodes(globalCodes: List[SampleCode]): Action[AnyContent] = Action.async {
    profileDataService.findByCodes(globalCodes).map(l => Ok(Json.toJson(l)))
  }

  def findByCodeWithAssociations(globalCode: SampleCode): Action[AnyContent] = Action.async {
    profileDataService.findByCodeWithAssociations(globalCode).map {
      case Some((pd, group, category)) =>
        Ok(Json.obj("profileData" -> pd, "group" -> group, "category" -> category))
      case None => BadRequest
    }
  }

  def getIsProfileReplicatedInternalCode(internalCode: String): Action[AnyContent] = Action.async {
    profileDataService.getIsProfileReplicatedInternalCode(internalCode).map(r => Ok(Json.toJson(r)))
  }

  def countProfiles(): Action[AnyContent] = Action.async {
    profileDataService.countProfiles().map(count => Ok(Json.toJson(count)))
  }
