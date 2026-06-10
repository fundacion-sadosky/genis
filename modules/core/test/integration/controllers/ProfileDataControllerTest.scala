package integration.controllers

import bulkupload.{BulkUploadModule, BulkUploadService}
import profiledata.ProfileDataModule
import connections.{InterconnectionService, InterconnectionServiceStub}
import fixtures.*
import kits.{StrKitModule, StrKitService}
import matching.{MatchingService, MatchingServiceStub}
import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.*
import play.api.test.Helpers.*
import probability.{ProbabilityModule, ProbabilityService}
import profile.ProfileService
import profiledata.{DeletedMotive, ProfileDataService}
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import types.SampleCode
import user.{LdapHealthService, RoleRepository, UsersModule}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ProfileDataControllerTest extends PlaySpec with GuiceOneAppPerTest:

  import ProfileDataFixtures.*

  private var pdStub: StubProfileDataService = _

  override def fakeApplication(): Application =
    pdStub = new StubProfileDataService
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[ProbabilityModule]
      .disable[BulkUploadModule]
      .disable[ProfileDataModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[BulkUploadService].toInstance(new StubBulkUploadService),
        bind[profiledata.ProfileDataRepository].to[profiledata.ProfileDataRepositoryStub],
        bind[ProfileDataService].toInstance(pdStub),
        bind[ProfileDataService].qualifiedWith("stashed").toInstance(pdStub),
        bind[ProfileService].toInstance(new StubProfileService),
        bind[MatchingService].toInstance(new MatchingServiceStub),
        bind[InterconnectionService].toInstance(new InterconnectionServiceStub),
        bind[StrKitService].toInstance(new StubStrKitService),
        bind[ProbabilityService].toInstance(new StubProbabilityService),
        bind[LdapHealthService].toInstance(new StubLdapHealthService),
        bind[ExecutionContext].qualifiedWith("lrmix-context").toInstance(ExecutionContext.global)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  private val sc = sampleCode.text

  "ProfileDataController.create" must {

    "return 200 with sampleCode body and X-CREATED-ID header when service creates profile" in {
      pdStub.createResult = Future.successful(Right(sampleCode))

      val body = Json.obj("category" -> "SOSPECHOSO", "internalSampleCode" -> "INT-001", "assignee" -> "analyst1")
      val result = route(app, FakeRequest(POST, "/api/v2/profiledata").withJsonBody(body)).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      (contentAsJson(result) \ "sampleCode").as[SampleCode] mustBe sampleCode
      header("X-CREATED-ID", result) mustBe Some(sc)
    }

    "return 400 when JSON body is malformed" in {
      val body = Json.obj("unexpected_field" -> "value")
      val result = route(app, FakeRequest(POST, "/api/v2/profiledata").withJsonBody(body)).get

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some("application/json")
    }

    "return 400 when service returns Left(error)" in {
      pdStub.createResult = Future.successful(Left("E0119: no se pudo crear el perfil"))

      val body = Json.obj("category" -> "SOSPECHOSO", "internalSampleCode" -> "INT-001", "assignee" -> "analyst1")
      val result = route(app, FakeRequest(POST, "/api/v2/profiledata").withJsonBody(body)).get

      status(result) mustBe BAD_REQUEST
    }
  }

  "ProfileDataController.findByCode" must {

    "return 200 with full ProfileData JSON when profile exists" in {
      pdStub.getByCodeResult = Future.successful(Some(profileData))

      val result = route(app, FakeRequest(GET, s"/api/v2/profiledata-complete/$sc")).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")

      val json = contentAsJson(result)
      (json \ "globalCode").as[SampleCode] mustBe sampleCode
      (json \ "category").asOpt[String] mustBe Some("SOSPECHOSO")
      (json \ "internalSampleCode").as[String] mustBe "INT-001"
      (json \ "dataFiliation" \ "identification").asOpt[String] mustBe Some("12345678")
    }

    "return 404 when profile does not exist" in {
      pdStub.getByCodeResult = Future.successful(None)

      val result = route(app, FakeRequest(GET, s"/api/v2/profiledata-complete/$sc")).get

      status(result) mustBe NOT_FOUND
    }
  }

  "ProfileDataController.isEditable" must {

    "return 200 with true when profile is editable" in {
      pdStub.isEditableResult = Future.successful(Some(true))

      val result = route(app, FakeRequest(GET, s"/api/v2/profiledata-editable/$sc")).get

      status(result) mustBe OK
      contentAsJson(result).asOpt[Boolean] mustBe Some(true)
    }

    "return 200 with false when profile is not editable" in {
      pdStub.isEditableResult = Future.successful(Some(false))

      val result = route(app, FakeRequest(GET, s"/api/v2/profiledata-editable/$sc")).get

      status(result) mustBe OK
      contentAsJson(result).asOpt[Boolean] mustBe Some(false)
    }
  }

  "ProfileDataController.getDeleteMotive" must {

    "return 204 No Content when profile has no delete motive" in {
      pdStub.getDeleteMotiveResult = Future.successful(None)

      val result = route(app, FakeRequest(GET, s"/api/v2/profiles-deleted/motive/$sc")).get

      status(result) mustBe NO_CONTENT
    }

    "return 200 with motive JSON when motive exists" in {
      pdStub.getDeleteMotiveResult = Future.successful(Some(deletedMotive))

      val result = route(app, FakeRequest(GET, s"/api/v2/profiles-deleted/motive/$sc")).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      (contentAsJson(result) \ "solicitor").as[String] mustBe deletedMotive.solicitor
    }
  }

  "ProfileDataController.update" must {

    "return 200 when JSON is valid and service updates profile" in {
      pdStub.updateProfileDataResult = Future.successful(true)

      val body = Json.obj("category" -> "SOSPECHOSO", "internalSampleCode" -> "INT-001", "assignee" -> "analyst1")
      val result = route(app, FakeRequest(PUT, s"/api/v2/profiledata/$sc/").withJsonBody(body)).get

      status(result) mustBe OK
    }

    "return 400 when JSON body is malformed" in {
      val body = Json.obj("unexpected_field" -> "value")
      val result = route(app, FakeRequest(PUT, s"/api/v2/profiledata/$sc/").withJsonBody(body)).get

      status(result) mustBe BAD_REQUEST
    }
  }

  "ProfileDataController.removeProfile" must {

    "return 200 when profile is removed successfully" in {
      pdStub.removeProfileResult = Future.successful(Right(sampleCode))

      val result = route(app, FakeRequest(DELETE, s"/api/v2/profiledata/$sc")).get

      status(result) mustBe OK
    }

    "return 400 when removal fails" in {
      pdStub.removeProfileResult = Future.successful(Left("no se puede eliminar"))

      val result = route(app, FakeRequest(DELETE, s"/api/v2/profiledata/$sc")).get

      status(result) mustBe BAD_REQUEST
    }
  }
