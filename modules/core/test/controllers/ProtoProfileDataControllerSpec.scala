package controllers

import com.google.inject.name.Names
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import profiledata._
import security.{StubUserRepository, UserRepository}
import user.{RoleRepository, UsersModule}
import security.StubRoleRepository
import types.{AlphanumericId, SampleCode}

import scala.concurrent.Future

class ProtoProfileDataControllerSpec extends PlaySpec with GuiceOneAppPerTest with MockitoSugar {

  val sampleCode = SampleCode("AR-B-SIGE-1")

  val testProfile = ProfileData(
    category              = AlphanumericId("CATX"),
    globalCode            = sampleCode,
    attorney              = None,
    bioMaterialType       = None,
    court                 = None,
    crimeInvolved         = None,
    crimeType             = None,
    criminalCase          = None,
    internalSampleCode    = "INT-001",
    assignee              = "gen1",
    laboratory            = "SIGE",
    deleted               = false,
    deletedMotive         = None,
    responsibleGeneticist = None,
    profileExpirationDate = None,
    sampleDate            = None,
    sampleEntryDate       = None,
    dataFiliation         = None,
    isExternal            = false
  )

  val validAttemptJson = Json.obj(
    "category"           -> "CATX",
    "internalSampleCode" -> "INT-001",
    "assignee"           -> "gen1"
  )

  val mockService: ProfileDataService = mock[ProfileDataService]

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[ProfileDataModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[ProfileDataService].toInstance(mockService),
        bind[ProfileDataService].qualifiedWith(Names.named("stashed")).toInstance(mockService)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  // --- GET /api/v2/protoprofiledata-editable/:id ---

  "GET /api/v2/protoprofiledata-editable/:id" should {

    "return 200 with true always" in {
      val result = route(app, FakeRequest(GET, "/api/v2/protoprofiledata-editable/AR-B-SIGE-1")).get
      status(result) mustBe OK
      contentAsJson(result).as[Boolean] mustBe true
    }
  }

  // --- GET /api/v2/protoprofiledata-complete/:id ---

  "GET /api/v2/protoprofiledata-complete/:id" should {

    "return 200 with profile JSON when found" in {
      when(mockService.get(sampleCode)).thenReturn(Future.successful(Some(testProfile)))
      val result = route(app, FakeRequest(GET, "/api/v2/protoprofiledata-complete/AR-B-SIGE-1")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }

    "return 404 when profile not found" in {
      when(mockService.get(sampleCode)).thenReturn(Future.successful(None))
      val result = route(app, FakeRequest(GET, "/api/v2/protoprofiledata-complete/AR-B-SIGE-1")).get
      status(result) mustBe NOT_FOUND
    }
  }

  // --- POST /api/v2/protoprofiledata ---

  "POST /api/v2/protoprofiledata" should {

    "return 200 with sampleCode on successful create" in {
      when(mockService.create(org.mockito.ArgumentMatchers.any())).thenReturn(Future.successful(Right(sampleCode)))
      val result = route(app, FakeRequest(POST, "/api/v2/protoprofiledata").withJsonBody(validAttemptJson)).get
      status(result) mustBe OK
      (contentAsJson(result) \ "sampleCode").as[String] mustBe "AR-B-SIGE-1"
      header("X-CREATED-ID", result) mustBe Some("AR-B-SIGE-1")
    }

    "return 400 when service returns Left error" in {
      when(mockService.create(org.mockito.ArgumentMatchers.any())).thenReturn(Future.successful(Left("duplicate")))
      val result = route(app, FakeRequest(POST, "/api/v2/protoprofiledata").withJsonBody(validAttemptJson)).get
      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "status").as[String] mustBe "KO"
    }

    "return 400 for malformed JSON" in {
      val result = route(app, FakeRequest(POST, "/api/v2/protoprofiledata")
        .withJsonBody(Json.obj("wrong" -> "data"))).get
      status(result) mustBe BAD_REQUEST
    }

    "return 400 when body is not JSON" in {
      val result = route(app, FakeRequest(POST, "/api/v2/protoprofiledata")
        .withTextBody("not json")).get
      status(result) mustBe BAD_REQUEST
    }
  }

  // --- PUT /api/v2/protoprofiledata/:id ---

  "PUT /api/v2/protoprofiledata/:id" should {

    "return 200 with true on successful update" in {
      when(mockService.updateProfileData(org.mockito.ArgumentMatchers.eq(sampleCode), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))
      val result = route(app, FakeRequest(PUT, "/api/v2/protoprofiledata/AR-B-SIGE-1").withJsonBody(validAttemptJson)).get
      status(result) mustBe OK
      contentAsJson(result).as[Boolean] mustBe true
    }

    "return 200 with false when update fails" in {
      when(mockService.updateProfileData(org.mockito.ArgumentMatchers.eq(sampleCode), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(Future.successful(false))
      val result = route(app, FakeRequest(PUT, "/api/v2/protoprofiledata/AR-B-SIGE-1").withJsonBody(validAttemptJson)).get
      status(result) mustBe OK
      contentAsJson(result).as[Boolean] mustBe false
    }

    "return 400 for malformed JSON" in {
      val result = route(app, FakeRequest(PUT, "/api/v2/protoprofiledata/AR-B-SIGE-1")
        .withJsonBody(Json.obj("wrong" -> "data"))).get
      status(result) mustBe BAD_REQUEST
    }
  }

  // --- GET /api/v2/resources/proto/static/:type/:id ---

  "GET /api/v2/resources/proto/static/:type/:id" should {

    "return 200 with binary data when resource found" in {
      val bytes = Array[Byte](1, 2, 3)
      when(mockService.getResource("I", 1L)).thenReturn(Future.successful(Some(bytes)))
      val result = route(app, FakeRequest(GET, "/api/v2/resources/proto/static/I/1")).get
      status(result) mustBe OK
    }

    "return 404 when resource not found" in {
      when(mockService.getResource("I", 99L)).thenReturn(Future.successful(None))
      val result = route(app, FakeRequest(GET, "/api/v2/resources/proto/static/I/99")).get
      status(result) mustBe NOT_FOUND
    }
  }
}
