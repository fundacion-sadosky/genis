package integration.controllers

import bulkupload.{BulkUploadModule, BulkUploadService}
import profiledata.ProfileDataModule
import fixtures.{StubBulkUploadService, StubLdapHealthService, StubProbabilityService, StubProfileDataService, StubStrKitService}
import kits.{StrKitModule, StrKitService}
import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.*
import play.api.test.Helpers.*
import probability.{ProbabilityModule, ProbabilityService}
import profiledata.ProfileDataService
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import types.SampleCode
import user.{LdapHealthService, RoleRepository, UsersModule}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ProtoProfileDataControllerTest extends PlaySpec with GuiceOneAppPerTest:

  private var profileDataStub: StubProfileDataService = _

  override def fakeApplication(): Application =
    profileDataStub = new StubProfileDataService
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[StrKitModule]
      .disable[ProbabilityModule]
      .disable[BulkUploadModule]
      .disable[ProfileDataModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[BulkUploadService].toInstance(new StubBulkUploadService),
        bind[profiledata.ProfileDataRepository].to[profiledata.ProfileDataRepositoryStub],
        bind[ProfileDataService].toInstance(profileDataStub),
        bind[StrKitService].toInstance(new StubStrKitService),
        bind[ProbabilityService].toInstance(new StubProbabilityService),
        bind[LdapHealthService].toInstance(new StubLdapHealthService),
        bind[ExecutionContext].qualifiedWith("lrmix-context").toInstance(ExecutionContext.global)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  private val sampleCode = "AR-C-SHDG-1"

  "ProtoProfileDataController.isEditable" must {

    "always return 200 with body true" in {
      val result = route(app, FakeRequest(GET, s"/api/v2/protoprofiledata-editable/$sampleCode")).get
      status(result) mustBe OK
      contentAsJson(result).as[Boolean] mustBe true
    }
  }

  "ProtoProfileDataController.create" must {

    "return 200 with X-CREATED-ID header when service creates a proto profile data" in {
      profileDataStub.createResult = Future.successful(Right(SampleCode(sampleCode)))
      val body = Json.obj(
        "category"           -> "SOSPECHOSO",
        "internalSampleCode" -> "TEST-001",
        "assignee"           -> "geneticist1"
      )
      val result = route(app,
        FakeRequest(POST, "/api/v2/protoprofiledata")
          .withHeaders(CONTENT_TYPE -> "application/json")
          .withBody(body)
      ).get
      status(result) mustBe OK
      header("X-CREATED-ID", result) mustBe Some(sampleCode)
    }

    "return 400 when service returns Left (validation error)" in {
      profileDataStub.createResult = Future.successful(Left("Error de validación"))
      val body = Json.obj(
        "category"           -> "SOSPECHOSO",
        "internalSampleCode" -> "TEST-001",
        "assignee"           -> "geneticist1"
      )
      val result = route(app,
        FakeRequest(POST, "/api/v2/protoprofiledata")
          .withHeaders(CONTENT_TYPE -> "application/json")
          .withBody(body)
      ).get
      status(result) mustBe BAD_REQUEST
    }

    "return 400 when JSON body does not match ProfileDataAttempt" in {
      val result = route(app,
        FakeRequest(POST, "/api/v2/protoprofiledata")
          .withHeaders(CONTENT_TYPE -> "application/json")
          .withBody(Json.obj("unexpected" -> "field"))
      ).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "ProtoProfileDataController.getByCode" must {

    "return 200 with profile data when found" in {
      import profiledata.ProfileData
      import types.AlphanumericId
      val pd = ProfileData(
        category = AlphanumericId("SOSPECHOSO"),
        globalCode = SampleCode(sampleCode),
        attorney = None, bioMaterialType = None, court = None,
        crimeInvolved = None, crimeType = None, criminalCase = None,
        internalSampleCode = "TEST-001",
        assignee = "geneticist1",
        laboratory = "SHDG",
        deleted = false,
        deletedMotive = None,
        responsibleGeneticist = None,
        profileExpirationDate = None,
        sampleDate = None,
        sampleEntryDate = None,
        dataFiliation = None,
        isExternal = false
      )
      profileDataStub.getResult = Future.successful(Some(pd))
      val result = route(app, FakeRequest(GET, s"/api/v2/protoprofiledata-complete/$sampleCode")).get
      status(result) mustBe OK
    }

    "return 404 when profile data is not found" in {
      profileDataStub.getResult = Future.successful(None)
      val result = route(app, FakeRequest(GET, s"/api/v2/protoprofiledata-complete/$sampleCode")).get
      status(result) mustBe NOT_FOUND
    }
  }

  "ProtoProfileDataController.getResources" must {

    "return 200 with bytes when resource is found" in {
      profileDataStub.getResourceResult = Future.successful(Some(Array[Byte](1, 2, 3)))
      val result = route(app, FakeRequest(GET, "/api/v2/resources/proto/static/image/1")).get
      status(result) mustBe OK
    }

    "return 404 when resource is not found" in {
      profileDataStub.getResourceResult = Future.successful(None)
      val result = route(app, FakeRequest(GET, "/api/v2/resources/proto/static/image/1")).get
      status(result) mustBe NOT_FOUND
    }
  }
