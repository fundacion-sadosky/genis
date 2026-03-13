package controllers

import configdata._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import types.AlphanumericId

import scala.concurrent.Future

class CategoriesControllerSpec extends PlaySpec with GuiceOneAppPerTest with MockitoSugar {

  val grpA = Group(AlphanumericId("GRP_A"), "Grupo A", None)
  val catA = Category(AlphanumericId("CAT_A"), AlphanumericId("GRP_A"), "Categoria A", isReference = true, None)

  val stubFull = FullCategory(
    id = catA.id, name = catA.name, description = None,
    group = catA.group, isReference = true,
    filiationDataRequired = false,
    configurations = Map.empty, associations = Nil, aliases = Nil, matchingRules = Nil
  )

  val stubCategoryService: CategoryService = new CategoryService {
    override def categoryTree                    = Future.successful(Map(grpA -> Seq(catA)))
    override def listCategories                  = Future.successful(Map(catA.id -> stubFull))
    override def listGroups                      = Future.successful(Seq(grpA))
    override def listCategoriesWithProfiles      = Future.successful(Map(catA.id -> catA.name))
    override def categoryTreeManualLoading       = Future.successful(Map(grpA -> Seq(catA)))
    override def listConfigurations              = Future.successful(Seq.empty)
    override def listAssociations                = Future.successful(Seq.empty)
    override def listAlias                       = Future.successful(Seq.empty)
    override def listMatchingRules               = Future.successful(Seq.empty)
    override def getCategory(id: AlphanumericId) = Future.successful(Some(stubFull))
    override def getCategoryType(id: AlphanumericId) = Future.successful(None)
    override def getCategoryTypeFromFullCategory(fc: FullCategory) = None
    override def addCategory(cat: Category)      = Future.successful(Right(stubFull))
    override def updateCategory(cat: Category)   = Future.successful(Right(1))
    override def updateCategory(cat: FullCategory) = Future.successful(Right(1))
    override def removeCategory(id: AlphanumericId) = Future.successful(Right(1))
    override def removeAllCategories()           = Future.successful(0)
    override def addGroup(g: Group)              = Future.successful(Right(g.id))
    override def updateGroup(g: Group)           = Future.successful(Right(1))
    override def removeGroup(id: AlphanumericId) = Future.successful(Right(1))
    override def removeAllGroups()               = Future.successful(0)
    override def insertOrUpdateMapping(m: CategoryMappingList) = Future.successful(Right(()))
    override def listCategoriesMapping           = Future.successful(List.empty)
    override def getCategoriesMappingById(id: AlphanumericId) = Future.successful(None)
    override def getCategoriesMappingReverseById(id: AlphanumericId) = Future.successful(None)
    override def registerCategoryModification(from: AlphanumericId, to: AlphanumericId) = Future.successful(Some(1))
    override def unregisterCategoryModification(from: AlphanumericId, to: AlphanumericId) = Future.successful(1)
    override def retrieveAllCategoryModificationAllowed = Future.successful(Seq.empty)
    override def getCategoryModificationAllowed(id: AlphanumericId) = Future.successful(Seq.empty)
    override def exportCategories(filePath: String) = Future.successful(Right("ok"))
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(bind[CategoryService].toInstance(stubCategoryService))
      .build()

  // ─── Category read endpoints ───────────────────────────────────────────────

  "CategoriesController GET /api/v2/categories" should {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categories")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }
  }

  "CategoriesController GET /api/v2/categories/full" should {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categories/full")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }
  }

  "CategoriesController GET /api/v2/categories/manualLoading" should {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categories/manualLoading")).get
      status(result) mustBe OK
    }
  }

  "CategoriesController GET /api/v2/categories/combo" should {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categories/combo")).get
      status(result) mustBe OK
    }
  }

  "CategoriesController GET /api/v2/categories/withProfiles" should {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categories/withProfiles")).get
      status(result) mustBe OK
    }
  }

  // ─── Category CRUD ─────────────────────────────────────────────────────────

  "CategoriesController POST /api/v2/categories" should {
    "return 200 OK with valid JSON" in {
      val body = Json.obj(
        "id" -> "CAT_A", "group" -> "GRP_A", "name" -> "Categoria A",
        "isReference" -> true, "description" -> ""
      )
      val result = route(app, FakeRequest(POST, "/api/v2/categories").withJsonBody(body)).get
      status(result) mustBe OK
    }

    "return 400 when body is missing" in {
      val result = route(app, FakeRequest(POST, "/api/v2/categories")).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "CategoriesController PUT /api/v2/categories/:catId" should {
    "return 200 OK with valid JSON" in {
      val body = Json.obj(
        "id" -> "CAT_A", "group" -> "GRP_A", "name" -> "Categoria A actualizada",
        "isReference" -> true, "description" -> ""
      )
      val result = route(app, FakeRequest(PUT, "/api/v2/categories/CAT_A").withJsonBody(body)).get
      status(result) mustBe OK
    }
  }

  "CategoriesController DELETE /api/v2/categories/:categoryId" should {
    "return 200 OK" in {
      val result = route(app, FakeRequest(DELETE, "/api/v2/categories/CAT_A")).get
      status(result) mustBe OK
    }
  }

  // ─── Group CRUD ────────────────────────────────────────────────────────────

  "CategoriesController GET /api/v2/groups" should {
    "return 200 OK with Content-Disposition header" in {
      val result = route(app, FakeRequest(GET, "/api/v2/groups")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      header("Content-Disposition", result) mustBe Some("attachment; filename=groups.json")
    }
  }

  "CategoriesController POST /api/v2/groups" should {
    "return 200 OK with valid JSON" in {
      val body = Json.obj("id" -> "GRP_A", "name" -> "Grupo A", "description" -> "")
      val result = route(app, FakeRequest(POST, "/api/v2/groups").withJsonBody(body)).get
      status(result) mustBe OK
    }

    "return 400 when body is missing" in {
      val result = route(app, FakeRequest(POST, "/api/v2/groups")).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "CategoriesController PUT /api/v2/groups/:groupId" should {
    "return 200 OK with valid JSON" in {
      val body = Json.obj("id" -> "GRP_A", "name" -> "Grupo A editado", "description" -> "")
      val result = route(app, FakeRequest(PUT, "/api/v2/groups/GRP_A").withJsonBody(body)).get
      status(result) mustBe OK
    }
  }

  "CategoriesController DELETE /api/v2/groups/:groupId" should {
    "return 200 OK" in {
      val result = route(app, FakeRequest(DELETE, "/api/v2/groups/GRP_A")).get
      status(result) mustBe OK
    }
  }

  // ─── Mappings ──────────────────────────────────────────────────────────────

  "CategoriesController GET /api/v2/categoryMappings" should {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categoryMappings")).get
      status(result) mustBe OK
    }
  }

  "CategoriesController POST /api/v2/categoryMappings" should {
    "return 200 OK with valid JSON" in {
      val body = Json.obj("categoryMappingList" -> Json.arr())
      val result = route(app, FakeRequest(POST, "/api/v2/categoryMappings").withJsonBody(body)).get
      status(result) mustBe OK
    }
  }

  // ─── Category modifications ────────────────────────────────────────────────

  "CategoriesController GET /api/v2/categoryModifications" should {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categoryModifications")).get
      status(result) mustBe OK
    }
  }

  "CategoriesController GET /api/v2/categoryModifications/:catId" should {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categoryModifications/CAT_A")).get
      status(result) mustBe OK
    }
  }

  "CategoriesController POST /api/v2/categoryModifications/:from/:to" should {
    "return 200 OK with success status" in {
      val result = route(app, FakeRequest(POST, "/api/v2/categoryModifications/CAT_A/CAT_B")).get
      status(result) mustBe OK
      (contentAsJson(result) \ "status").as[String] mustBe "success"
    }
  }

  "CategoriesController DELETE /api/v2/categoryModifications/:from/:to" should {
    "return 200 OK with success status" in {
      val result = route(app, FakeRequest(DELETE, "/api/v2/categoryModifications/CAT_A/CAT_B")).get
      status(result) mustBe OK
      (contentAsJson(result) \ "status").as[String] mustBe "success"
    }
  }
}