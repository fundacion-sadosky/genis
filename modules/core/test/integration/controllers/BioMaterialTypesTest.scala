package integration.controllers

import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.Application
import play.api.test.*
import play.api.test.Helpers.*
import play.api.libs.json.Json

import configdata.{BioMaterialType, BioMaterialTypeService}
import fixtures.{StubBioMaterialTypeService, StubLdapHealthService, StubStrKitService}
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import user.{LdapHealthService, RoleRepository, UsersModule}
import kits.{StrKitModule, StrKitService}
import types.AlphanumericId

class BioMaterialTypesTest extends PlaySpec with GuiceOneAppPerTest {

  private var bmtStub: StubBioMaterialTypeService = _

  override def fakeApplication(): Application = {
    bmtStub = new StubBioMaterialTypeService
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[StrKitModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[BioMaterialTypeService].toInstance(bmtStub),
        bind[StrKitService].toInstance(new StubStrKitService),
        bind[LdapHealthService].toInstance(new StubLdapHealthService)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()
  }

  private val bmt = BioMaterialType(AlphanumericId("BLOOD"), "Sangre", Some("Muestra de sangre"))

  "BioMaterialTypes controller" must {

    "list bio material types" in {
      bmtStub.listResult = scala.concurrent.Future.successful(Seq(bmt))

      val request = FakeRequest(GET, "/api/v2/bioMaterialTypes")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result).as[Seq[BioMaterialType]] mustBe Seq(bmt)
    }

    "insert bio material type with valid JSON" in {
      bmtStub.insertResult = scala.concurrent.Future.successful(1)

      val request = FakeRequest(POST, "/api/v2/bioMaterialTypes").withBody(Json.toJson(bmt))
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[Int] mustBe 1
    }

    "return BadRequest for invalid JSON on insert" in {
      val request = FakeRequest(POST, "/api/v2/bioMaterialTypes")
        .withBody(Json.obj("wrong" -> "data"))
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }

    "return 415 for plain text body on insert" in {
      val request = FakeRequest(POST, "/api/v2/bioMaterialTypes")
        .withTextBody("not json")
      val result = route(app, request).get

      status(result) mustBe UNSUPPORTED_MEDIA_TYPE
    }

    "update bio material type with valid JSON" in {
      bmtStub.updateResult = scala.concurrent.Future.successful(1)

      val request = FakeRequest(PUT, "/api/v2/bioMaterialTypes").withBody(Json.toJson(bmt))
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[Int] mustBe 1
    }

    "return BadRequest for invalid JSON on update" in {
      val request = FakeRequest(PUT, "/api/v2/bioMaterialTypes")
        .withBody(Json.obj("wrong" -> "data"))
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }

    "remove bio material type" in {
      bmtStub.deleteResult = scala.concurrent.Future.successful(1)

      val request = FakeRequest(DELETE, "/api/v2/bioMaterialTypes/BLOOD")
      val result = route(app, request).get

      status(result) mustBe OK
      (contentAsJson(result) \ "deletes").as[Int] mustBe 1
    }
  }
}
