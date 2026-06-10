package controllers

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.*
import configdata.{CategoryService, FullCategory, Group}
import profiledata.{ProfileData, ProfileDataService}
import types.SampleCode

import scala.concurrent.{ExecutionContext, Future}

class ProfileDataController @Inject()(
  cc: ControllerComponents,
  profileDataService: ProfileDataService,
  categoryService: CategoryService
)(using ec: ExecutionContext) extends AbstractController(cc):

  def findByCodes(globalCode: String): Action[AnyContent] = Action.async {
    val codes = globalCode.split(",").map(_.trim).filter(_.nonEmpty).map(SampleCode(_)).toList
    profileDataService.findByCodes(codes).map(list => Ok(Json.toJson(list)))
  }

  def getByCode(globalCode: SampleCode): Action[AnyContent] = Action.async {
    profileDataService.findByCode(globalCode).map {
      case None     => NotFound
      case Some(pd) => Ok(Json.toJson(pd))
    }
  }

  // ProfileData.findByCode (singular) — usado por pdg-profile-data-info-popover en la lista de coincidencias
  def findByCodeSingle(globalCode: SampleCode): Action[AnyContent] = Action.async {
    profileDataService.findByCode(globalCode).map {
      case None     => NotFound
      case Some(pd) => Ok(Json.toJson(pd))
    }
  }

  def isProfileReplicated(internalCode: String): Action[AnyContent] = Action.async {
    profileDataService.isProfileReplicated(internalCode).map(r => Ok(Json.toJson(r)))
  }

  def countProfiles(): Action[AnyContent] = Action.async {
    profileDataService.countProfiles().map(n => Ok(Json.toJson(n)))
  }

  def findByCodeWithAssociations(globalCode: SampleCode): Action[AnyContent] = Action.async {
    profileDataService.findByCode(globalCode).flatMap {
      case None => Future.successful(Ok(Json.obj()))
      case Some(profile) =>
        for {
          categoryOpt <- categoryService.getCategory(profile.category)
          groups      <- categoryService.listGroups
        } yield {
          val result = for {
            category <- categoryOpt
            group    <- groups.find(_.id == category.group)
          } yield Ok(Json.obj(
            "profileData" -> profile,
            "group"       -> group,
            "category"    -> category
          ))
          result.getOrElse(Ok(Json.obj()))
        }
    }
  }
