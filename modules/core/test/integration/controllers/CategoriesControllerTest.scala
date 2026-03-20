package controllers

import com.unboundid.ldap.sdk.LDAPConnectionPool
import configdata._
import fixtures.{StubCategoryService, StubStrKitService}
import org.mockito.Mockito.mock as mockOf
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import user.{RoleRepository, UsersModule}
import kits.{StrKitModule, StrKitService}
import types.AlphanumericId

import scala.concurrent.Future

class CategoriesControllerTest extends AnyWordSpec with Matchers with GuiceOneAppPerTest {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[StrKitModule]
      .overrides(
        bind[CategoryService].to[StubCategoryService],
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[StrKitService].toInstance(new StubStrKitService),
        bind[LDAPConnectionPool].toInstance(mockOf(classOf[LDAPConnectionPool]))
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  // ─── Category read endpoints ───────────────────────────────────────────────

  "CategoriesController GET /api/v2/categories" must {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categories")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }
  }

  "CategoriesController GET /api/v2/categories/full" must {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categories/full")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }
  }

  "CategoriesController GET /api/v2/categories/manualLoading" must {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categories/manualLoading")).get
      status(result) mustBe OK
    }
  }

  "CategoriesController GET /api/v2/categories/combo" must {
    "return 200 OK and exclude AM/PM groups" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categories/combo")).get
      status(result) mustBe OK
      val json = contentAsJson(result)
      (json \ "GRP_A").toOption must not be empty
      (json \ "AM").toOption mustBe None
      (json \ "PM").toOption mustBe None
    }
  }

  "CategoriesController GET /api/v2/categories/withProfiles" must {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categories/withProfiles")).get
      status(result) mustBe OK
    }
  }

  // ─── Category CRUD ─────────────────────────────────────────────────────────

  "CategoriesController POST /api/v2/categories" must {
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

  "CategoriesController PUT /api/v2/categories/:catId" must {
    "return 200 OK with valid JSON" in {
      val body = Json.obj(
        "id" -> "CAT_A", "group" -> "GRP_A", "name" -> "Categoria A actualizada",
        "isReference" -> true, "description" -> ""
      )
      val result = route(app, FakeRequest(PUT, "/api/v2/categories/CAT_A").withJsonBody(body)).get
      status(result) mustBe OK
    }

    "return 400 when body is missing" in {
      val result = route(app, FakeRequest(PUT, "/api/v2/categories/CAT_A")).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "CategoriesController PUT /api/v2/categories/full/:catId" must {
    "return 200 OK with valid FullCategory JSON" in {
      val body = Json.obj(
        "id" -> "CAT_A", "name" -> "Categoria A", "description" -> "desc",
        "group" -> "GRP_A", "isReference" -> true, "filiationDataRequired" -> false,
        "configurations" -> Json.obj(), "associations" -> Json.arr(),
        "aliases" -> Json.arr(), "matchingRules" -> Json.arr()
      )
      val result = route(app, FakeRequest(PUT, "/api/v2/categories/full/CAT_A").withJsonBody(body)).get
      status(result) mustBe OK
    }

    "return 400 when body is missing" in {
      val result = route(app, FakeRequest(PUT, "/api/v2/categories/full/CAT_A")).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "CategoriesController DELETE /api/v2/categories/:categoryId" must {
    "return 200 OK" in {
      val result = route(app, FakeRequest(DELETE, "/api/v2/categories/CAT_A")).get
      status(result) mustBe OK
    }
  }

  // ─── Group CRUD ────────────────────────────────────────────────────────────

  "CategoriesController GET /api/v2/groups" must {
    "return 200 OK with Content-Disposition header" in {
      val result = route(app, FakeRequest(GET, "/api/v2/groups")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      header("Content-Disposition", result) mustBe Some("attachment; filename=groups.json")
    }
  }

  "CategoriesController POST /api/v2/groups" must {
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

  "CategoriesController PUT /api/v2/groups/:groupId" must {
    "return 200 OK with valid JSON" in {
      val body = Json.obj("id" -> "GRP_A", "name" -> "Grupo A editado", "description" -> "")
      val result = route(app, FakeRequest(PUT, "/api/v2/groups/GRP_A").withJsonBody(body)).get
      status(result) mustBe OK
    }

    "return 400 when body is missing" in {
      val result = route(app, FakeRequest(PUT, "/api/v2/groups/GRP_A")).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "CategoriesController DELETE /api/v2/groups/:groupId" must {
    "return 200 OK" in {
      val result = route(app, FakeRequest(DELETE, "/api/v2/groups/GRP_A")).get
      status(result) mustBe OK
    }
  }

  // ─── Mappings ──────────────────────────────────────────────────────────────

  "CategoriesController GET /api/v2/categoryMappings" must {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categoryMappings")).get
      status(result) mustBe OK
    }
  }

  "CategoriesController POST /api/v2/categoryMappings" must {
    "return 200 OK with valid JSON" in {
      val body = Json.obj("categoryMappingList" -> Json.arr())
      val result = route(app, FakeRequest(POST, "/api/v2/categoryMappings").withJsonBody(body)).get
      status(result) mustBe OK
    }

    "return 400 when body is missing" in {
      val result = route(app, FakeRequest(POST, "/api/v2/categoryMappings")).get
      status(result) mustBe BAD_REQUEST
    }
  }

  // ─── Category modifications ────────────────────────────────────────────────

  "CategoriesController GET /api/v2/categoryModifications" must {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categoryModifications")).get
      status(result) mustBe OK
    }
  }

  "CategoriesController GET /api/v2/categoryModifications/:catId" must {
    "return 200 OK" in {
      val result = route(app, FakeRequest(GET, "/api/v2/categoryModifications/CAT_A")).get
      status(result) mustBe OK
    }
  }

  "CategoriesController POST /api/v2/categoryModifications/:from/:to" must {
    "return 200 OK with success status" in {
      val result = route(app, FakeRequest(POST, "/api/v2/categoryModifications/CAT_A/CAT_B")).get
      status(result) mustBe OK
      (contentAsJson(result) \ "status").as[String] mustBe "success"
    }
  }

  "CategoriesController DELETE /api/v2/categoryModifications/:from/:to" must {
    "return 200 OK with success status" in {
      val result = route(app, FakeRequest(DELETE, "/api/v2/categoryModifications/CAT_A/CAT_B")).get
      status(result) mustBe OK
      (contentAsJson(result) \ "status").as[String] mustBe "success"
    }
  }

  // ─── Export endpoints ────────────────────────────────────────────────────────

  "CategoriesController GET /api/v2/export/categoryConfigurations" must {
    "return 200 OK with Content-Disposition header" in {
      val result = route(app, FakeRequest(GET, "/api/v2/export/categoryConfigurations")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      header("Content-Disposition", result) mustBe Some("attachment; filename=categoryConfigurations.json")
    }
  }

  "CategoriesController GET /api/v2/export/categoryAssociations" must {
    "return 200 OK with Content-Disposition header" in {
      val result = route(app, FakeRequest(GET, "/api/v2/export/categoryAssociations")).get
      status(result) mustBe OK
      header("Content-Disposition", result) mustBe Some("attachment; filename=categoryAssociations.json")
    }
  }

  "CategoriesController GET /api/v2/export/categoryAlias" must {
    "return 200 OK with Content-Disposition header" in {
      val result = route(app, FakeRequest(GET, "/api/v2/export/categoryAlias")).get
      status(result) mustBe OK
      header("Content-Disposition", result) mustBe Some("attachment; filename=categoryAlias.json")
    }
  }

  "CategoriesController GET /api/v2/export/categoryMatchingRules" must {
    "return 200 OK with Content-Disposition header" in {
      val result = route(app, FakeRequest(GET, "/api/v2/export/categoryMatchingRules")).get
      status(result) mustBe OK
      header("Content-Disposition", result) mustBe Some("attachment; filename=categoryMatchingRules.json")
    }
  }

  "CategoriesController GET /api/v2/export/categoryModifications" must {
    "return 200 OK with Content-Disposition header" in {
      val result = route(app, FakeRequest(GET, "/api/v2/export/categoryModifications")).get
      status(result) mustBe OK
      header("Content-Disposition", result) mustBe Some("attachment; filename=categoryModifications.json")
    }
  }

  "CategoriesController GET /api/v2/export/categoryMappings" must {
    "return 200 OK with Content-Disposition header" in {
      val result = route(app, FakeRequest(GET, "/api/v2/export/categoryMappings")).get
      status(result) mustBe OK
      header("Content-Disposition", result) mustBe Some("attachment; filename=categoryMappings.json")
    }
  }
}
