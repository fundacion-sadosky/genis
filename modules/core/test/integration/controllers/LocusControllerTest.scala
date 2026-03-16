package controllers

import kits._
import pedigree.{MutationService, NoOpMutationService}
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import security.{StubUserRepository, UserRepository}
import user.{RoleRepository, UsersModule}
import security.StubRoleRepository
import fixtures.LocusFixtures

import scala.concurrent.Future

class LocusControllerTest extends PlaySpec with GuiceOneAppPerTest:

  val stubLocusService: LocusService = new LocusService:
    override def add(locus: FullLocus): Future[Either[String, String]] =
      if locus.locus.id == "ERROR" then Future.successful(Left("Error message"))
      else Future.successful(Right(locus.locus.id))

    override def update(locus: FullLocus): Future[Either[String, Unit]] =
      if locus.locus.id == "ERROR" then Future.successful(Left("Error message"))
      else Future.successful(Right(()))

    override def listFull(): Future[Seq[FullLocus]] =
      Future.successful(LocusFixtures.fullLocusList)

    override def list(): Future[Seq[Locus]] =
      Future.successful(LocusFixtures.fullLocusList.map(_.locus))

    override def delete(id: String): Future[Either[String, String]] =
      if id == "ERROR" then Future.successful(Left("Error message"))
      else Future.successful(Right(id))

    override def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[String]] =
      Future.successful(Seq("LOCUS 1"))

    override def getLocusByAnalysisType(analysisType: Int): Future[Seq[String]] =
      Future.successful(Seq("LOCUS 1"))

    override def locusRangeMap(): Future[Map[String, AleleRange]] =
      Future.successful(Map("LOCUS 1" -> AleleRange(BigDecimal(5), BigDecimal(30))))

  private val stubStrKitService: StrKitService = new StrKitService:
    override def get(id: String) = Future.successful(None)
    override def getFull(id: String) = Future.successful(None)
    override def list() = Future.successful(Seq.empty)
    override def listFull() = Future.successful(Seq.empty)
    override def findLociByKit(kitId: String) = Future.successful(List.empty)
    override def findLociByKits(kitIds: Seq[String]) = Future.successful(Map.empty)
    override def getKitAlias = Future.successful(Map.empty)
    override def getLocusAlias = Future.successful(Map.empty)
    override def add(kit: StrKit) = Future.successful(Left("stub"))
    override def addAlias(id: String, alias: String) = Future.successful(Left("stub"))
    override def addLocus(id: String, locus: NewStrKitLocus) = Future.successful(Left("stub"))
    override def update(kit: StrKit) = Future.successful(Left("stub"))
    override def delete(id: String) = Future.successful(Left("stub"))
    override def deleteAlias(id: String) = Future.successful(Left("stub"))
    override def deleteLocus(id: String) = Future.successful(Left("stub"))

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[StrKitModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[LocusService].toInstance(stubLocusService),
        bind[StrKitService].toInstance(stubStrKitService)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  "LocusController" must {

    "return 200 OK for GET /api/v2/locus-full" in {
      val result = route(app, FakeRequest(GET, "/api/v2/locus-full")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }

    "return 200 OK for GET /api/v2/locus" in {
      val result = route(app, FakeRequest(GET, "/api/v2/locus")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }

    "return 200 OK for DELETE /api/v2/locus/delete/:id with success" in {
      val result = route(app, FakeRequest(DELETE, "/api/v2/locus/delete/LOCUS1")).get
      status(result) mustBe OK
      header("X-CREATED-ID", result) mustBe Some("LOCUS1")
    }

    "return 400 Bad Request for DELETE /api/v2/locus/delete/:id with error" in {
      val result = route(app, FakeRequest(DELETE, "/api/v2/locus/delete/ERROR")).get
      status(result) mustBe BAD_REQUEST
    }

    "return 200 OK for POST /api/v2/locus/create with valid JSON" in {
      val body = Json.toJson(LocusFixtures.fullLocus1)
      val result = route(app, FakeRequest(POST, "/api/v2/locus/create").withJsonBody(body)).get
      status(result) mustBe OK
      header("X-CREATED-ID", result) mustBe Some(LocusFixtures.fullLocus1.locus.id)
    }

    "return 200 OK for PUT /api/v2/locus with valid JSON" in {
      val body = Json.toJson(LocusFixtures.fullLocus1)
      val result = route(app, FakeRequest(PUT, "/api/v2/locus").withJsonBody(body)).get
      status(result) mustBe OK
      header("X-CREATED-ID", result) mustBe Some(LocusFixtures.fullLocus1.locus.id)
    }

    "return 400 Bad Request for POST /api/v2/locus/create with service error" in {
      val errorLocus = LocusFixtures.fullLocus1.copy(locus = LocusFixtures.fullLocus1.locus.copy(id = "ERROR"))
      val body = Json.toJson(errorLocus)
      val result = route(app, FakeRequest(POST, "/api/v2/locus/create").withJsonBody(body)).get
      status(result) mustBe BAD_REQUEST
    }

    "return 400 Bad Request for POST /api/v2/locus/create with invalid JSON" in {
      val body = Json.obj("invalid" -> "data")
      val result = route(app, FakeRequest(POST, "/api/v2/locus/create").withJsonBody(body)).get
      status(result) mustBe BAD_REQUEST
    }

    "return 400 Bad Request for PUT /api/v2/locus with service error" in {
      val errorLocus = LocusFixtures.fullLocus1.copy(locus = LocusFixtures.fullLocus1.locus.copy(id = "ERROR"))
      val body = Json.toJson(errorLocus)
      val result = route(app, FakeRequest(PUT, "/api/v2/locus").withJsonBody(body)).get
      status(result) mustBe BAD_REQUEST
    }

    "return 200 OK for GET /api/v2/locus/ranges" in {
      val result = route(app, FakeRequest(GET, "/api/v2/locus/ranges")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }

    "return 200 OK for GET /api/v2/locus/export with Content-Disposition header" in {
      val result = route(app, FakeRequest(GET, "/api/v2/locus/export")).get
      status(result) mustBe OK
      header("Content-Disposition", result) mustBe Some("attachment; filename=locus.json")
    }
  }
