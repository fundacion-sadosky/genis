package integration.controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.Application
import play.api.test._
import play.api.test.Helpers._

import com.unboundid.ldap.sdk.LDAPConnectionPool
import org.mockito.Mockito.mock as mockOf
import security.{UserRepository, StubUserRepository}
import user.{RoleRepository, UsersModule}
import security.StubRoleRepository
import kits.{StrKitModule, StrKitService}
import fixtures.StubStrKitService

class StatusControllerTest extends PlaySpec with GuiceOneAppPerTest {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[StrKitModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[StrKitService].toInstance(new StubStrKitService),
        bind[LDAPConnectionPool].toInstance(mockOf(classOf[LDAPConnectionPool]))
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
