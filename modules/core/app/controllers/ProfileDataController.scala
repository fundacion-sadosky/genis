package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.Json
import profiledata._
import types.SampleCode
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProfileDataController @Inject()(
  cc: ControllerComponents,
  profileDataService: ProfileDataService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // --- read ---

  def getByGlobalCode(globalCode: String): Action[AnyContent] = Action.async {
    profileDataService.get(SampleCode(globalCode)).map {
      case Some(pd) => Ok(Json.toJson(pd))
      case None     => NotFound(Json.obj("error" -> s"Profile not found: $globalCode"))
    }
  }

  def findByCode(globalCode: String): Action[AnyContent] = Action.async {
    profileDataService.findByCode(SampleCode(globalCode)).map {
      case Some(pd) => Ok(Json.toJson(pd))
      case None     => NotFound(Json.obj("error" -> s"Profile not found: $globalCode"))
    }
  }

  def isDeleted(globalCode: String): Action[AnyContent] = Action.async {
    // TODO: isDeleted is not exposed in ProfileDataService — delegates to repository directly.
    // Consider adding it to the service trait when the service layer is reviewed.
    Future.successful(NotImplemented)
  }

  def getResource(resourceType: String, id: Long): Action[AnyContent] = Action.async {
    profileDataService.getResource(resourceType, id).map {
      case Some(bytes) => Ok(bytes)
      case None        => NotFound(Json.obj("error" -> s"Resource not found: $resourceType/$id"))
    }
  }

  def getDesktopProfiles(): Action[AnyContent] = Action.async {
    profileDataService.getDesktopProfiles().map { codes =>
      Ok(Json.toJson(codes))
    }
  }

  def getDeleteMotive(globalCode: String): Action[AnyContent] = Action.async {
    profileDataService.getDeleteMotive(SampleCode(globalCode)).map {
      case Some(motive) => Ok(Json.toJson(motive))
      case None         => NotFound(Json.obj("error" -> s"No delete motive for: $globalCode"))
    }
  }

  // --- write ---

  // TODO: create requires CacheService, TraceService, NotificationService.
  def create(): Action[AnyContent] = Action.async {
    Future.successful(NotImplemented)
  }

  // TODO: updateProfileData requires CacheService, TraceService.
  def updateProfileData(globalCode: String): Action[AnyContent] = Action.async {
    Future.successful(NotImplemented)
  }

  // TODO: deleteProfile requires PedigreeService, ScenarioRepository, MatchingService.
  def deleteProfile(globalCode: String): Action[AnyContent] = Action.async {
    Future.successful(NotImplemented)
  }

  // TODO: removeProfile is implemented in service but requires validation review.
  def removeProfile(globalCode: String): Action[AnyContent] = Action.async {
    Future.successful(NotImplemented)
  }
}