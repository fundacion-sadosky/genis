package integration.controllers

import fixtures.StubLdapHealthService
import motive.*
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import security.{StubUserRepository, StubRoleRepository, UserRepository}
import user.{LdapHealthService, RoleRepository, UsersModule}

import scala.concurrent.Future

class MotiveControllerTest extends PlaySpec with GuiceOneAppPerTest {

  val stubMotiveService: MotiveService = new MotiveService {
    override def getMotives(motiveType: Long, editable: Boolean): Future[List[Motive]] =
      Future.successful(List(Motive(1, motiveType, "Test motive", false)))
    override def getMotivesTypes(): Future[List[MotiveType]] =
      Future.successful(List(MotiveType(1, "Causa penal")))
    override def deleteMotiveById(id: Long): Future[Either[String, Unit]] =
      Future.successful(Right(()))
    override def insert(row: Motive): Future[Either[String, Long]] =
      Future.successful(Right(1L))
    override def update(row: Motive): Future[Either[String, Unit]] =
      Future.successful(Right(()))
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[MotiveModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[MotiveService].toInstance(stubMotiveService),
        bind[LdapHealthService].toInstance(new StubLdapHealthService)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  "MotiveController" should {

    "return 200 OK for GET /api/v2/motive-types" in {
      val result = route(app, FakeRequest(GET, "/api/v2/motive-types")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }

    "return 200 OK for GET /api/v2/motive with params" in {
      val result = route(app, FakeRequest(GET, "/api/v2/motive?id=1&abm=false")).get
      status(result) mustBe OK
    }

    "return 200 OK for POST /api/v2/motive with valid JSON" in {
      val body   = Json.obj("id" -> 0, "motiveType" -> 1, "description" -> "nuevo", "freeText" -> false)
      val result = route(app, FakeRequest(POST, "/api/v2/motive").withJsonBody(body)).get
      status(result) mustBe OK
    }

    "return 200 OK for PUT /api/v2/motive with valid JSON" in {
      val body   = Json.obj("id" -> 1, "motiveType" -> 1, "description" -> "editado", "freeText" -> false)
      val result = route(app, FakeRequest(PUT, "/api/v2/motive").withJsonBody(body)).get
      status(result) mustBe OK
    }

    "return 200 OK for DELETE /api/v2/motive/:id" in {
      val result = route(app, FakeRequest(DELETE, "/api/v2/motive/1")).get
      status(result) mustBe OK
    }
  }
}
