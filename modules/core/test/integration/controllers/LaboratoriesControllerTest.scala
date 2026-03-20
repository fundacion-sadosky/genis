package integration.controllers

import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.Application
import play.api.test.*
import play.api.test.Helpers.*
import play.api.libs.json.Json

import fixtures.{StubCountryService, StubLaboratoryService, StubLdapHealthService, StubStrKitService}
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import services.{CountryService, LaboratoryService}
import user.{LdapHealthService, RoleRepository, UsersModule}
import kits.{StrKitModule, StrKitService}
import types.Laboratory

class LaboratoriesControllerTest extends PlaySpec with GuiceOneAppPerTest {

  private var labStub: StubLaboratoryService = _
  private var countryStub: StubCountryService = _

  override def fakeApplication(): Application = {
    labStub = new StubLaboratoryService
    countryStub = new StubCountryService
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[StrKitModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[LaboratoryService].toInstance(labStub),
        bind[CountryService].toInstance(countryStub),
        bind[StrKitService].toInstance(new StubStrKitService),
        bind[LdapHealthService].toInstance(new StubLdapHealthService)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()
  }

  private val lab = Laboratory(
    name = "Lab Central",
    code = "LC01",
    country = "AR",
    province = "Buenos Aires",
    address = "Av. Siempre Viva 123",
    telephone = "1122334455",
    contactEmail = "lab@example.com",
    dropIn = 0.5,
    dropOut = 0.8
  )

  "LaboratoriesController" must {

    "list laboratories returning JSON array" in {
      labStub.listResult = scala.concurrent.Future.successful(Seq(lab))

      val request = FakeRequest(GET, "/api/v2/laboratory")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val labs = contentAsJson(result).as[Seq[Laboratory]]
      labs mustBe Seq(lab)
    }

    "list countries" in {
      countryStub.listCountriesResult = scala.concurrent.Future.successful(Seq("AR", "BR"))

      val request = FakeRequest(GET, "/api/v2/country")
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[Seq[String]] mustBe Seq("AR", "BR")
    }

    "list provinces for a country" in {
      countryStub.listProvincesResult = scala.concurrent.Future.successful(Seq("Buenos Aires", "Córdoba"))

      val request = FakeRequest(GET, "/api/v2/provinces/AR")
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[Seq[String]] mustBe Seq("Buenos Aires", "Córdoba")
    }

    "add laboratory successfully with X-CREATED-ID header" in {
      labStub.addResult = scala.concurrent.Future.successful(1)

      val request = FakeRequest(POST, "/api/v2/laboratory").withBody(Json.toJson(lab))
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      header("X-CREATED-ID", result) mustBe defined
    }

    "return BadRequest for invalid JSON on POST" in {
      val request = FakeRequest(POST, "/api/v2/laboratory")
        .withBody(Json.obj("wrong" -> "data"))
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }

    "get existing laboratory" in {
      labStub.getResult = scala.concurrent.Future.successful(Some(lab))

      val request = FakeRequest(GET, "/api/v2/laboratory/LC01")
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[Laboratory] mustBe lab
    }

    "return NoContent for non-existing laboratory" in {
      labStub.getResult = scala.concurrent.Future.successful(None)

      val request = FakeRequest(GET, "/api/v2/laboratory/UNKNOWN")
      val result = route(app, request).get

      status(result) mustBe NO_CONTENT
    }

    "update laboratory successfully" in {
      labStub.updateResult = scala.concurrent.Future.successful(1)

      val request = FakeRequest(PUT, "/api/v2/laboratory").withBody(Json.toJson(lab))
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[String] mustBe "1"
    }

    "return BadRequest for invalid JSON on PUT" in {
      val request = FakeRequest(PUT, "/api/v2/laboratory")
        .withBody(Json.obj("wrong" -> "data"))
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }
  }
}
