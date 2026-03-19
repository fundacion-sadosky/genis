package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, JsValue, Json}
import profiledata.ProfileDataModelsJson._
import models.CategoryModelsJson._
import models.FullCategoryJson._
import play.api.mvc.{Action, AnyContent, BodyParsers, BaseController, ControllerComponents}
import search.FullTextSearchService
import configdata.CategoryService
import profiledata.ProfileDataSearch
import types.AlphanumericId
import services.UserService
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SearchProfileDatasController @Inject() (
  val controllerComponents: ControllerComponents,
  fullTextSearchService: FullTextSearchService,
  categoryService: CategoryService,
  userService: UserService
)(implicit ec: ExecutionContext) extends BaseController {

  def search: Action[JsValue] = Action.async(parse.json) { request =>
    val input = request.body.validate[ProfileDataSearch]
    input.fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search => {
        userService.isSuperUser(search.userId).flatMap { isSuperUser =>
          val newSearch = new ProfileDataSearch(
            search.userId,
            isSuperUser,
            search.page,
            search.pageSize,
            search.input,
            search.active,
            search.inactive,
            search.notUploaded,
            search.category
          )
          fullTextSearchService.searchProfileDatas(newSearch).map { profileDatas =>
            Ok(Json.toJson(profileDatas))
          }
        }
      }
    )
  }

  def searchTotal: Action[JsValue] = Action.async(parse.json) { request =>
    val input = request.body.validate[ProfileDataSearch]
    input.fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search => {
        userService.isSuperUser(search.userId).flatMap { isSuperUser =>
          val newSearch = new ProfileDataSearch(
            search.userId,
            isSuperUser,
            search.page,
            search.pageSize,
            search.input,
            search.active,
            search.inactive,
            search.notUploaded,
            search.category
          )
          fullTextSearchService.searchFilterTotalAndTotalProfileDatas(newSearch).map {
            case (length: Int, totalLength: Int) =>
              Ok("")
                .withHeaders(
                  "X-PROFILES-LENGTH" -> length.toString,
                  "X-PROFILES-TOTAL-LENGTH" -> totalLength.toString
                )
          }
        }
      }
    )
  }

  def searchProfilesAssociable(input: String, category: String): Action[AnyContent] = Action.async {
    val associables = categoryService
      .listCategories(AlphanumericId(category))
      .associations
    fullTextSearchService
      .searchProfileDatasWithFilter(input) { profile =>
        associables.exists(_.categoryRelated == profile.category)
      }
      .map { profileDatas => Ok(Json.toJson(profileDatas)) }
  }

  def searchProfilesForPedigree(input: String): Action[AnyContent] = Action.async {
    val categories = categoryService.listCategories
    fullTextSearchService
      .searchProfileDatasWithFilter(input) { profile =>
        val catId = types.AlphanumericId(profile.category)
        categories.get(catId).exists(_.pedigreeAssociation)
      }
      .map { profileDatas => Ok(Json.toJson(profileDatas)) }
  }
}
