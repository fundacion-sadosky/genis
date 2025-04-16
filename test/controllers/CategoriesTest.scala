package controllers

import scala.concurrent.Future
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test.Helpers.contentAsJson
import play.api.test.Helpers.contentType
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status
import configdata.{CategoryMapping, CategoryMappingList, CategoryService, FullCategoryMapping}
import profile.ProfileService
import specs.PdgSpec
import stubs.Stubs
import types.AlphanumericId


class CategoriesTest extends PdgSpec with MockitoSugar with Results {

  "Categories controller" must {
    "return a JSON sturcture with categories, success status code and valid headers" in {

      val mockCategoryService = mock[CategoryService]
      when(mockCategoryService.categoryTree).thenReturn(Stubs.categoryTree)

      val mockProfileService = mock[ProfileService]
      
      val categoriesController = new Categories(mockCategoryService, mockProfileService)
      val result: Future[Result] = categoriesController.categoryTree().apply(FakeRequest())

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"

      val categoryTree = contentAsJson(result).as[Map[String, JsValue]]

      for ((groupId, group) <- categoryTree) {
        val (stubGroup, stubCategories) = Stubs.categoryTree.find(_._1.id == AlphanumericId(groupId)).get
        (group \ "id").as[AlphanumericId] mustBe stubGroup.id
        (group \ "name").as[String] mustBe stubGroup.name

        val categories = (group \ "subcategories").as[Seq[JsValue]]

        for (category <- categories) {
          val categoryId = (category \ "id").as[AlphanumericId]
          val stubCategory = stubCategories.find(_.id == categoryId).get
          (category \ "id").as[AlphanumericId] mustBe stubCategory.id
          (category \ "name").as[String] mustBe stubCategory.name
          (category \ "description").as[Option[String]] mustBe stubCategory.description
        }
      }
    }
    "list mappings" in {
      val categoryService = mock[CategoryService]
      val fcm:List[FullCategoryMapping]= List(FullCategoryMapping(AlphanumericId("ER"),"","",""))

      when(categoryService.listCategoriesMapping).thenReturn(Future.successful(fcm))

      val target = new Categories(categoryService, mock[ProfileService])
      val result: Future[Result] = target.listCategoriesMapping.apply(FakeRequest())
      status(result) mustBe OK
    }
    "insert or update mappings" in {
      val categoryService = mock[CategoryService]
      val cm:List[CategoryMapping]= List(CategoryMapping(AlphanumericId("ER"),"SA"),
        CategoryMapping(AlphanumericId("IR"),"TS"))
      val cml = CategoryMappingList(cm)
      when(categoryService.insertOrUpdateMapping(cml)).thenReturn(Future.successful(Right(())))

      val target = new Categories(categoryService, mock[ProfileService])

      val json = play.api.libs.json.Json.toJson(cml)
      val request = FakeRequest().withBody(json)
      val result = target.insertOrUpdateCategoriesMapping.apply(request)

      status(result) mustBe OK
    }
    "insert or update mappings with badrequest" in {
      val categoryService = mock[CategoryService]
      val cm:List[CategoryMapping]= List(CategoryMapping(AlphanumericId("ER"),"SA"),
        CategoryMapping(AlphanumericId("IR"),"TS"))
      val cml = CategoryMappingList(cm)
      when(categoryService.insertOrUpdateMapping(cml)).thenReturn(Future.successful(Right(())))

      val target = new Categories(categoryService, mock[ProfileService])

      val json = play.api.libs.json.Json.toJson(cm)
      val request = FakeRequest().withBody(json)
      val result = target.insertOrUpdateCategoriesMapping.apply(request)

      status(result) mustBe BAD_REQUEST
    }
  }

  it must {
    "return a JSON sturcture with subcategories, success status code and valid headers" in {

      val mockCategoryService = mock[CategoryService]

      when(mockCategoryService.listCategories).thenReturn(Stubs.categoryMap)

      val target = new Categories(mockCategoryService, mock[ProfileService])
      val result: Future[Result] = target.list().apply(FakeRequest())

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"

      val categories = contentAsJson(result).as[Map[String, JsValue]]

      for ((categoryId, category) <- categories) {
        val stubcategory = Stubs.categoryMap(AlphanumericId(categoryId))
        (category \ "id").as[AlphanumericId] mustBe stubcategory.id
        (category \ "name").as[String] mustBe stubcategory.name
        (category \ "description").as[String] mustBe stubcategory.description.get
        (category \ "isReference").as[Boolean] mustBe stubcategory.isReference
      }
    }
  }

}