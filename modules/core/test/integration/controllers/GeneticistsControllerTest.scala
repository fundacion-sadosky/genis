package integration.controllers

import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.Application
import play.api.test.*
import play.api.test.Helpers.*
import play.api.libs.json.Json

import fixtures.{StubGeneticistService, StubUserService}
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import services.{GeneticistService, UserService}
import user.{RoleRepository, UsersModule}
import types.{Geneticist, User}

class GeneticistsControllerTest extends PlaySpec with GuiceOneAppPerTest {

  private var genStub: StubGeneticistService = _
  private var userStub: StubUserService = _

  override def fakeApplication(): Application = {
    genStub = new StubGeneticistService
    userStub = new StubUserService
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[GeneticistService].toInstance(genStub),
        bind[UserService].toInstance(userStub)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()
  }

  private val geneticist = Geneticist(
    name = "Juan",
    laboratory = "LC01",
    lastname = "Pérez",
    email = "juan@example.com",
    telephone = "1122334455",
    id = Some(42L)
  )

  "GeneticistsController" must {

    "list all geneticists for a laboratory" in {
      genStub.getAllResult = scala.concurrent.Future.successful(Seq(geneticist))

      val request = FakeRequest(GET, "/api/v2/geneticist/LC01")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result).as[Seq[Geneticist]] mustBe Seq(geneticist)
    }

    "list geneticist users sorted by name" in {
      val users = Seq(
        User("Zoe", "Arias", 2L),
        User("Ana", "López", 1L)
      )
      userStub.findUserAssignableResult = scala.concurrent.Future.successful(users)

      val request = FakeRequest(GET, "/api/v2/geneticist-users")
      val result = route(app, request).get

      status(result) mustBe OK
      val parsed = contentAsJson(result).as[Seq[User]]
      parsed.head.firstName mustBe "Ana"
      parsed.last.firstName mustBe "Zoe"
    }

    "add geneticist successfully" in {
      genStub.addResult = scala.concurrent.Future.successful(42)

      val request = FakeRequest(POST, "/api/v2/geneticist").withBody(Json.toJson(geneticist))
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[Int] mustBe 42
      header("X-CREATED-ID", result) mustBe Some("42")
    }

    "return BadRequest for invalid JSON on POST" in {
      val request = FakeRequest(POST, "/api/v2/geneticist")
        .withBody(Json.obj("wrong" -> "data"))
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }

    "update geneticist successfully" in {
      genStub.updateResult = scala.concurrent.Future.successful(1)

      val request = FakeRequest(PUT, "/api/v2/geneticist").withBody(Json.toJson(geneticist))
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[Int] mustBe 1
    }

    "return BadRequest for invalid JSON on PUT" in {
      val request = FakeRequest(PUT, "/api/v2/geneticist")
        .withBody(Json.obj("wrong" -> "data"))
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }

    "get existing geneticist" in {
      genStub.getResult = scala.concurrent.Future.successful(Some(geneticist))

      val request = FakeRequest(GET, "/api/v2/geneticist?geneticistId=42")
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[Geneticist] mustBe geneticist
    }

    "return NoContent for non-existing geneticist" in {
      genStub.getResult = scala.concurrent.Future.successful(None)

      val request = FakeRequest(GET, "/api/v2/geneticist?geneticistId=999")
      val result = route(app, request).get

      status(result) mustBe NO_CONTENT
    }
  }
}
