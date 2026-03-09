package integration.controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.Application
import play.api.test._
import play.api.test.Helpers._

import security.{UserRepository, StubUserRepository}
import user.{RoleRepository, UsersModule}
import security.StubRoleRepository

class StatusControllerTest extends PlaySpec with GuiceOneAppPerTest {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository]
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  "StatusController" must {

    "return 200 OK on GET /health" in {
      val request = FakeRequest(GET, "/health")
      val result  = route(app, request).get

      status(result) mustBe OK
    }

    "return 200 OK on GET /api/v2/status" in {
      val request = FakeRequest(GET, "/api/v2/status")
      val result  = route(app, request).get

      status(result) mustBe OK
    }

    "return JSON content type on /api/v2/status" in {
      val request = FakeRequest(GET, "/api/v2/status")
      val result  = route(app, request).get

      contentType(result) mustBe Some("application/json")
    }
  }
}
