package integration.controllers

import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.Application
import play.api.test.*
import play.api.test.Helpers.*
import play.api.libs.json.Json

import fixtures.StubStrKitService
import kits.{FullStrKit, NewStrKitLocus, StrKit, StrKitModule, StrKitService}
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import user.{RoleRepository, UsersModule}

class StrKitsControllerTest extends PlaySpec with GuiceOneAppPerTest {

  private var kitStub: StubStrKitService = _

  override def fakeApplication(): Application = {
    kitStub = new StubStrKitService
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[StrKitModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[StrKitService].toInstance(kitStub)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()
  }

  private val kit = StrKit("PP16", "PowerPlex 16", 1, 16, 15)
  private val fullKit = FullStrKit("PP16", "PowerPlex 16", 1, 16, 15,
    Seq("PowerPlex16"),
    Seq(NewStrKitLocus("D3S1358", Some("Blue"), 1))
  )

  "StrKitsController" must {

    "list kits returning JSON array" in {
      kitStub.listResult = scala.concurrent.Future.successful(Seq(kit))

      val request = FakeRequest(GET, "/api/v2/strkits")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result).as[Seq[StrKit]] mustBe Seq(kit)
    }

    "list full kits returning JSON array" in {
      kitStub.listFullResult = scala.concurrent.Future.successful(Seq(fullKit))

      val request = FakeRequest(GET, "/api/v2/strkits-full")
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[Seq[FullStrKit]] mustBe Seq(fullKit)
    }

    "get existing kit by id" in {
      kitStub.getResult = scala.concurrent.Future.successful(Some(kit))

      val request = FakeRequest(GET, "/api/v2/strkits/PP16")
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[StrKit] mustBe kit
    }

    "return NotFound for non-existing kit" in {
      kitStub.getResult = scala.concurrent.Future.successful(None)

      val request = FakeRequest(GET, "/api/v2/strkits/UNKNOWN")
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND
    }

    "get full kit by id" in {
      kitStub.getFullResult = scala.concurrent.Future.successful(Some(fullKit))

      val request = FakeRequest(GET, "/api/v2/strkits-full/PP16")
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[FullStrKit] mustBe fullKit
    }

    "return NotFound for non-existing full kit" in {
      kitStub.getFullResult = scala.concurrent.Future.successful(None)

      val request = FakeRequest(GET, "/api/v2/strkits-full/UNKNOWN")
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND
    }

    "return NotImplemented on POST /api/v2/strkits" in {
      val request = FakeRequest(POST, "/api/v2/strkits")
      val result = route(app, request).get

      status(result) mustBe NOT_IMPLEMENTED
    }

    "return NotImplemented on PUT /api/v2/strkits" in {
      val request = FakeRequest(PUT, "/api/v2/strkits")
      val result = route(app, request).get

      status(result) mustBe NOT_IMPLEMENTED
    }

    "return NotImplemented on DELETE /api/v2/strkits/:id" in {
      val request = FakeRequest(DELETE, "/api/v2/strkits/PP16")
      val result = route(app, request).get

      status(result) mustBe NOT_IMPLEMENTED
    }
  }
}
