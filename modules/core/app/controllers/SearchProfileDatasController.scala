package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import search.FullTextSearchService
import configdata.CategoryService
import profiledata.ProfileDataSearch
import types.AlphanumericId
import services.UserService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SearchProfileDatasController @Inject()(
  cc: ControllerComponents,
  fullTextSearchService: FullTextSearchService,
  categoryService: CategoryService,
  userService: UserService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def search: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[ProfileDataSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search =>
        userService.isSuperUser(search.userId).flatMap { isSuperUser =>
          fullTextSearchService
            .searchProfileDatas(search.copy(isSuperUser = isSuperUser))
            .map(profileDatas => Ok(Json.toJson(profileDatas)))
        }
    )
  }

  def searchTotal: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[ProfileDataSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search =>
        userService.isSuperUser(search.userId).flatMap { isSuperUser =>
          fullTextSearchService
            .searchFilterTotalAndTotalProfileDatas(search.copy(isSuperUser = isSuperUser))
            .map { case (length, totalLength) =>
              Ok("").withHeaders(
                "X-PROFILES-LENGTH"       -> length.toString,
                "X-PROFILES-TOTAL-LENGTH" -> totalLength.toString
              )
            }
        }
    )
  }

  def searchProfilesAssociable(input: String, category: String): Action[AnyContent] = Action.async {
    categoryService.listCategories.flatMap { cats =>
      val associables = cats.get(AlphanumericId(category))
        .map(_.associations)
        .getOrElse(Seq.empty)
      fullTextSearchService
        .searchProfileDatasWithFilter(input) { profile =>
          associables.exists(_.categoryRelated == profile.category)
        }
        .map(profileDatas => Ok(Json.toJson(profileDatas)))
    }
  }

  def searchProfilesForPedigree(input: String): Action[AnyContent] = Action.async {
    categoryService.listCategories.flatMap { cats =>
      fullTextSearchService
        .searchProfileDatasWithFilter(input) { profile =>
          cats.get(profile.category).exists(_.pedigreeAssociation)
        }
        .map(profileDatas => Ok(Json.toJson(profileDatas)))
    }
  }
}
