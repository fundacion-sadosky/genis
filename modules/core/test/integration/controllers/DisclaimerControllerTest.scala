package integration.controllers

import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.Application
import play.api.test.*
import play.api.test.Helpers.*
import play.api.libs.json.Json

import disclaimer.{Disclaimer, DisclaimerModule, DisclaimerService}
import fixtures.{StubDisclaimerService, StubStrKitService}
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import user.{RoleRepository, UsersModule}
import kits.{StrKitModule, StrKitService}

class DisclaimerControllerTest extends PlaySpec with GuiceOneAppPerTest {

  private var disclaimerStub: StubDisclaimerService = _

  override def fakeApplication(): Application = {
    disclaimerStub = new StubDisclaimerService
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[StrKitModule]
      .disable[DisclaimerModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[DisclaimerService].toInstance(disclaimerStub),
        bind[StrKitService].toInstance(new StubStrKitService)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()
  }

  "DisclaimerController" must {

    "return disclaimer when text exists" in {
      disclaimerStub.getResult = scala.concurrent.Future.successful(Disclaimer(Some("Terms and conditions")))

      val request = FakeRequest(GET, "/api/v2/disclaimer")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      (contentAsJson(result) \ "text").as[String] mustBe "Terms and conditions"
    }

    "return NotFound when no disclaimer text" in {
      disclaimerStub.getResult = scala.concurrent.Future.successful(Disclaimer(None))

      val request = FakeRequest(GET, "/api/v2/disclaimer")
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND
      (contentAsJson(result) \ "message").as[String] mustBe "No existe en disclaimer"
    }
  }
}
