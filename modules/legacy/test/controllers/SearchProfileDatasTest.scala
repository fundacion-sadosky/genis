package controllers

import configdata.CategoryService
import org.mockito.Mockito.when
import org.mockito.Matchers.any
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import profiledata.ProfileDataSearch
import search.FullTextSearchService
import specs.PdgSpec
import stubs.Stubs
import user.UserService

import scala.concurrent.Future

class SearchProfileDatasTest extends PdgSpec with MockitoSugar with Results {

  val profileDatas = Seq(Stubs.profileData, Stubs.profileData2nd)
  val profilesFull = Seq(Stubs.profileDataFull, Stubs.profileData2ndFull)

  val search = ProfileDataSearch("pdg", false)

  "SearchProfileDatas controller" must {

    "search - ok" in {
      val fullTextSearchService = mock[FullTextSearchService]
      when(fullTextSearchService.searchProfileDatas(search)).thenReturn(Future.successful(profilesFull))

      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val target = new SearchProfileDatas(fullTextSearchService, null, userService)

      val jsRequest = Json.toJson(search)
      val request = FakeRequest().withBody(jsRequest)
      val result: Future[Result] = target.search().apply(request)

      status(result) mustBe OK
    }

    "search - bad request" in {
      val fullTextSearchService = mock[FullTextSearchService]
      when(fullTextSearchService.searchProfileDatas(search)).thenReturn(Future.successful(profilesFull))
      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val target = new SearchProfileDatas(fullTextSearchService, null, userService)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = target.search().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "search total - ok" in {
      val fullTextSearchService = mock[FullTextSearchService]
      when(fullTextSearchService.searchFilterTotalAndTotalProfileDatas(search)).thenReturn(Future.successful((4, 6)))
      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val target = new SearchProfileDatas(fullTextSearchService, null, userService)

      val jsRequest = Json.toJson(search)
      val request = FakeRequest().withBody(jsRequest)
      val result: Future[Result] = target.searchTotal().apply(request)

      status(result) mustBe OK
      header("X-PROFILES-LENGTH", result).get mustBe "4"
      header("X-PROFILES-TOTAL-LENGTH", result).get mustBe "6"
    }

    "search total - bad request" in {
      val fullTextSearchService = mock[FullTextSearchService]
      when(fullTextSearchService.searchTotalProfileDatas(search)).thenReturn(Future.successful(4))
      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val target = new SearchProfileDatas(fullTextSearchService, null, userService)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = target.searchTotal().apply(request)

      status(result) mustBe BAD_REQUEST
    }

/*    "search profiles associable" in {
      val fullTextSearchService = mock[FullTextSearchService]
      when(fullTextSearchService.searchProfileDatasWithFilter("FE01")(_ => true)).thenReturn(Future.successful(profileDatas))

      val categoryService = mock[CategoryService]
      when(categoryService.listCategories).thenReturn(Stubs.categoryMap)

      val target = new SearchProfileDatas(fullTextSearchService, categoryService, null)

      val result: Future[Result] = target.searchProfilesAssociable("FE01", "SOSPECHOSO").apply(FakeRequest())

      status(result) mustBe OK
    }

    "search profiles for pedigree" in {
      val fullTextSearchService = mock[FullTextSearchService]
      when(fullTextSearchService.searchProfileDatasWithFilter("FE01")(_ => true)).thenReturn(Future.successful(profileDatas))

      val categoryService = mock[CategoryService]
      when(categoryService.listCategories).thenReturn(Stubs.categoryMap)

      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((true)))

      val target = new SearchProfileDatas(fullTextSearchService, categoryService, null)

      val result: Future[Result] = target.searchProfilesForPedigree("FE01").apply(FakeRequest())

      status(result) mustBe OK
    }
*/
  }

}
