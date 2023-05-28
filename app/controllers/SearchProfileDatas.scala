package controllers

import javax.inject.Inject
import javax.inject.Singleton

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, BodyParsers, Controller}
import search.FullTextSearchService
import configdata.CategoryService
import profiledata.ProfileDataSearch
import types.AlphanumericId
import user.UserService

import scala.concurrent.Future

@Singleton
class SearchProfileDatas @Inject() (
    fullTextSearchService: FullTextSearchService,
    categoryService: CategoryService,
    userService: UserService
) extends Controller {

  def search = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[ProfileDataSearch]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      search => {
        userService.isSuperUser(search.userId).flatMap(isSuperUser => {
          val newSearch = new ProfileDataSearch(search.userId,isSuperUser, search.page, search.pageSize, search.input, search.active, search.inactive, search.notUploaded, search.category)
          fullTextSearchService.searchProfileDatas(newSearch) map {
            profileDatas => Ok(Json.toJson(profileDatas))
          }
        })
      }
    )
  }

  def searchTotal = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[ProfileDataSearch]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      search => {
        userService.isSuperUser(search.userId).flatMap(isSuperUser => {
          val newSearch = new ProfileDataSearch(search.userId,isSuperUser, search.page, search.pageSize, search.input, search.active, search.inactive, search.notUploaded, search.category)
/*
          fullTextSearchService.searchTotalProfileDatas(newSearch).map { size =>
            Ok("").withHeaders("X-PROFILES-LENGTH" -> size.toString)
*/
          fullTextSearchService.searchFilterTotalAndTotalProfileDatas(newSearch).map { (sizes) =>
            Ok("").withHeaders("X-PROFILES-LENGTH" -> sizes._1.toString).withHeaders("X-PROFILES-TOTAL-LENGTH" -> sizes._2.toString)
          }
        })
      }
    )
  }

  def searchProfilesAssociable(input: String, category: String) = Action.async {
    
    val associables = categoryService.listCategories(AlphanumericId(category)).associations
    
    fullTextSearchService.searchProfileDatasWithFilter(input){ profile => 
      associables.exists(_.categoryRelated == profile.category)
    }.map { profileDatas => Ok(Json.toJson(profileDatas)) }
   
  }

  def searchProfilesForPedigree(input: String) = Action.async {

    val categories = categoryService.listCategories

    fullTextSearchService.searchProfileDatasWithFilter(input){ profile =>
      categories(profile.category).pedigreeAssociation
    }.map { profileDatas => Ok(Json.toJson(profileDatas)) }

  }

}