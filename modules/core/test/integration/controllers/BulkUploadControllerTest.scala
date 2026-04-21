package integration.controllers

import bulkupload.{BulkUploadModule, BulkUploadService}
import profiledata.ProfileDataModule
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
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import services.UserService
import fixtures.{StubUserService, StubBulkUploadService, StubLdapHealthService, StubProbabilityService, StubStrKitService}
import user.{LdapHealthService, RoleRepository, UsersModule}

import scala.concurrent.ExecutionContext

class BulkUploadControllerTest extends PlaySpec with GuiceOneAppPerTest:

  private var bulkStub: StubBulkUploadService = _
  private var userStub: StubUserService       = _

  override def fakeApplication(): Application =
    bulkStub = new StubBulkUploadService
    userStub  = new StubUserService
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[StrKitModule]
      .disable[ProbabilityModule]
      .disable[BulkUploadModule]
      .disable[ProfileDataModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[BulkUploadService].toInstance(bulkStub),
        bind[profiledata.ProfileDataService].toInstance(new fixtures.StubProfileDataService),
        bind[profiledata.ProfileDataRepository].to[profiledata.ProfileDataRepositoryStub],
        bind[UserService].toInstance(userStub),
        bind[StrKitService].toInstance(new StubStrKitService),
        bind[ProbabilityService].toInstance(new StubProbabilityService),
        bind[LdapHealthService].toInstance(new StubLdapHealthService),
        bind[ExecutionContext].qualifiedWith("lrmix-context").toInstance(ExecutionContext.global)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  "BulkUploadController.deleteBatch" must {

    "return 200 when service successfully deletes the batch" in {
      bulkStub.deleteBatchResult = scala.concurrent.Future.successful(Right(1L))
      val result = route(app, FakeRequest(DELETE, "/api/v2/bulkupload/1")).get
      status(result) mustBe OK
    }

    "return 400 when service returns Left (batch has imported samples or DB error)" in {
      bulkStub.deleteBatchResult = scala.concurrent.Future.successful(Left("No se puede eliminar"))
      val result = route(app, FakeRequest(DELETE, "/api/v2/bulkupload/1")).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "BulkUploadController.updateProtoProfileData" must {

    "return 200 when service updates category successfully" in {
      import bulkupload.ProtoProfile
      import bulkupload.ProtoProfileStatus
      val pp = ProtoProfile(1L, "sample1", "user1", "SOSPECHOSO", ProtoProfileStatus.ReadyForApproval, "kit", Nil, Map.empty, Nil, Nil, "", None)
      bulkStub.updateProtoProfileDataResult = scala.concurrent.Future.successful(Right(pp))
      val result = route(app, FakeRequest(POST, "/api/v2/protoprofiles/1/subcategory?category=SOSPECHOSO")
        .withHeaders("X-USER" -> "user1")).get
      status(result) mustBe OK
    }

    "return 400 when service returns Left (category not found or validation error)" in {
      bulkStub.updateProtoProfileDataResult = scala.concurrent.Future.successful(Left(Seq("No existe la categoria")))
      val result = route(app, FakeRequest(POST, "/api/v2/protoprofiles/1/subcategory?category=UNKNOWN")
        .withHeaders("X-USER" -> "user1")).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "BulkUploadController.updateBatchStatus" must {

    "return 200 with valid JSON body and successful service response" in {
      bulkStub.updateBatchStatusResult = scala.concurrent.Future.successful(Right(1L))
      val result = route(app,
        FakeRequest(POST, "/api/v2/protoprofiles/multiple-status?idBatch=1&status=Approved&replicateAll=false")
          .withHeaders("X-USER" -> "user1", CONTENT_TYPE -> "application/json")
          .withBody(Json.arr())
      ).get
      status(result) mustBe OK
    }

    "return 400 when request body is not valid JSON" in {
      val result = route(app,
        FakeRequest(POST, "/api/v2/protoprofiles/multiple-status?idBatch=1&status=Approved&replicateAll=false")
          .withHeaders("X-USER" -> "user1", CONTENT_TYPE -> "application/json")
          .withTextBody("not-json")
      ).get
      status(result) mustBe BAD_REQUEST
    }

    "return 400 when service returns Left (error during batch transition)" in {
      bulkStub.updateBatchStatusResult = scala.concurrent.Future.successful(Left("Error en lote"))
      val result = route(app,
        FakeRequest(POST, "/api/v2/protoprofiles/multiple-status?idBatch=1&status=Approved&replicateAll=false")
          .withHeaders("X-USER" -> "user1", CONTENT_TYPE -> "application/json")
          .withBody(Json.arr())
      ).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "BulkUploadController.updateProtoProfileRulesMismatch" must {

    "return 200 with X-CREATED-ID header when service updates successfully" in {
      bulkStub.updateProtoProfileRulesMismatchResult = scala.concurrent.Future.successful(true)
      val body = Json.obj("id" -> 7, "mismatches" -> Json.obj(), "matchingRules" -> Json.arr())
      val result = route(app,
        FakeRequest(POST, "/api/v2/protoprofiles/matchingrules")
          .withHeaders(CONTENT_TYPE -> "application/json")
          .withBody(body)
      ).get
      status(result) mustBe OK
      header("X-CREATED-ID", result) mustBe Some("7")
    }

    "return 400 when JSON body does not match ProtoProfileMatchingQuality" in {
      val result = route(app,
        FakeRequest(POST, "/api/v2/protoprofiles/matchingrules")
          .withHeaders(CONTENT_TYPE -> "application/json")
          .withBody(Json.obj("unexpected" -> "fields"))
      ).get
      status(result) mustBe BAD_REQUEST
    }
  }
