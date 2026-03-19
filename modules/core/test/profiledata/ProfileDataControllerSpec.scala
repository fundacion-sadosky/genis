package profiledata

import controllers.ProfileDataController
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
import security.{StubUserRepository, UserRepository}
import user.{RoleRepository, UsersModule}
import security.StubRoleRepository
import types.{AlphanumericId, SampleCode}
import models.{ExternalProfileDataRow, ProfileReceivedRow, ProfileSentRow, ProfileUploadedRow}

import com.google.inject.name.Names

import java.io.File
import scala.concurrent.Future

class ProfileDataControllerSpec extends PlaySpec with GuiceOneAppPerTest with MockitoSugar {

  val sampleCode = SampleCode("AR-B-SIGE-1")

  val testProfile = ProfileData(
    category             = AlphanumericId("CATX"),
    globalCode           = sampleCode,
    attorney             = None,
    bioMaterialType      = None,
    court                = None,
    crimeInvolved        = None,
    crimeType            = None,
    criminalCase         = None,
    internalSampleCode   = "INT-001",
    assignee             = "gen1",
    laboratory           = "SIGE",
    deleted              = false,
    deletedMotive        = None,
    responsibleGeneticist = None,
    profileExpirationDate = None,
    sampleDate           = None,
    sampleEntryDate      = None,
    dataFiliation        = None,
    isExternal           = false
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

  // --- GET /api/v2/profiledata/desktop ---

  "GET /api/v2/profiledata/desktop" should {

    "return 200 with JSON array of global codes" in {
      when(mockService.getDesktopProfiles()).thenReturn(Future.successful(Seq(sampleCode)))
      val result = route(app, FakeRequest(GET, "/api/v2/profiledata/desktop")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }

    "return 200 with empty JSON array when no desktop profiles" in {
      when(mockService.getDesktopProfiles()).thenReturn(Future.successful(Seq.empty))
      val result = route(app, FakeRequest(GET, "/api/v2/profiledata/desktop")).get
      status(result) mustBe OK
    }
  }

  // --- GET /api/v2/profiledata/deleted/:globalCode ---

  "GET /api/v2/profiledata/deleted/:globalCode" should {

    "return 200 with true when profile is deleted" in {
      when(mockService.isDeleted(sampleCode)).thenReturn(Future.successful(Some(true)))
      val result = route(app, FakeRequest(GET, "/api/v2/profiledata/deleted/AR-B-SIGE-1")).get
      status(result) mustBe OK
      contentAsJson(result).as[Boolean] mustBe true
    }

    "return 200 with false when profile is not deleted" in {
      when(mockService.isDeleted(sampleCode)).thenReturn(Future.successful(Some(false)))
      val result = route(app, FakeRequest(GET, "/api/v2/profiledata/deleted/AR-B-SIGE-1")).get
      status(result) mustBe OK
      contentAsJson(result).as[Boolean] mustBe false
    }

    "return 404 when profile does not exist" in {
      when(mockService.isDeleted(sampleCode)).thenReturn(Future.successful(None))
      val result = route(app, FakeRequest(GET, "/api/v2/profiledata/deleted/AR-B-SIGE-1")).get
      status(result) mustBe NOT_FOUND
    }
  }

  // --- GET /api/v2/profiledata/deletemotive/:globalCode ---

  "GET /api/v2/profiledata/deletemotive/:globalCode" should {

    "return 200 with delete motive JSON" in {
      val motive = DeletedMotive("sol", "motive text", 1L)
      when(mockService.getDeleteMotive(sampleCode)).thenReturn(Future.successful(Some(motive)))
      val result = route(app, FakeRequest(GET, "/api/v2/profiledata/deletemotive/AR-B-SIGE-1")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }

    "return 404 when no delete motive exists" in {
      when(mockService.getDeleteMotive(sampleCode)).thenReturn(Future.successful(None))
      val result = route(app, FakeRequest(GET, "/api/v2/profiledata/deletemotive/AR-B-SIGE-1")).get
      status(result) mustBe NOT_FOUND
    }
  }

  // --- GET /api/v2/profiledata/code/:globalCode ---

  "GET /api/v2/profiledata/code/:globalCode" should {

    "return 200 with profile JSON when found" in {
      when(mockService.findByCode(sampleCode)).thenReturn(Future.successful(Some(testProfile)))
      val result = route(app, FakeRequest(GET, "/api/v2/profiledata/code/AR-B-SIGE-1")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }

    "return 404 when profile not found" in {
      when(mockService.findByCode(sampleCode)).thenReturn(Future.successful(None))
      val result = route(app, FakeRequest(GET, "/api/v2/profiledata/code/AR-B-SIGE-1")).get
      status(result) mustBe NOT_FOUND
    }
  }

  // --- GET /api/v2/profiledata/:globalCode ---

  "GET /api/v2/profiledata/:globalCode" should {

    "return 200 with profile JSON when found" in {
      when(mockService.get(sampleCode)).thenReturn(Future.successful(Some(testProfile)))
      val result = route(app, FakeRequest(GET, "/api/v2/profiledata/AR-B-SIGE-1")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
    }

    "return 404 when profile not found" in {
      when(mockService.get(sampleCode)).thenReturn(Future.successful(None))
      val result = route(app, FakeRequest(GET, "/api/v2/profiledata/AR-B-SIGE-1")).get
      status(result) mustBe NOT_FOUND
    }
  }

  // --- GET /api/v2/profiledata/resource/:type/:id ---

  "GET /api/v2/profiledata/resource/:resourceType/:id" should {

    "return 200 with binary data when resource found" in {
      val bytes = Array[Byte](1, 2, 3)
      when(mockService.getResource("I", 1L)).thenReturn(Future.successful(Some(bytes)))
      val result = route(app, FakeRequest(GET, "/api/v2/profiledata/resource/I/1")).get
      status(result) mustBe OK
    }

    "return 404 when resource not found" in {
      when(mockService.getResource("I", 99L)).thenReturn(Future.successful(None))
      val result = route(app, FakeRequest(GET, "/api/v2/profiledata/resource/I/99")).get
      status(result) mustBe NOT_FOUND
    }
  }

  // --- DELETE /api/v2/profiledata/remove/:globalCode ---

  "DELETE /api/v2/profiledata/remove/:globalCode" should {

    "return 200 when profile is removed successfully" in {
      when(mockService.removeProfile(sampleCode)).thenReturn(Future.successful(Right(sampleCode)))
      val result = route(app, FakeRequest(DELETE, "/api/v2/profiledata/remove/AR-B-SIGE-1")).get
      status(result) mustBe OK
    }

    "return 404 when profile to remove does not exist" in {
      when(mockService.removeProfile(sampleCode)).thenReturn(Future.successful(Left("Profile not found: AR-B-SIGE-1")))
      val result = route(app, FakeRequest(DELETE, "/api/v2/profiledata/remove/AR-B-SIGE-1")).get
      status(result) mustBe NOT_FOUND
    }
  }

  // --- Not yet implemented endpoints ---

  "POST /api/v2/profiledata" should {
    "return 501 NotImplemented" in {
      val result = route(app, FakeRequest(POST, "/api/v2/profiledata").withJsonBody(Json.obj())).get
      status(result) mustBe NOT_IMPLEMENTED
    }
  }

  "PUT /api/v2/profiledata/:globalCode" should {
    "return 501 NotImplemented" in {
      val result = route(app, FakeRequest(PUT, "/api/v2/profiledata/AR-B-SIGE-1").withJsonBody(Json.obj())).get
      status(result) mustBe NOT_IMPLEMENTED
    }
  }

  "DELETE /api/v2/profiledata/:globalCode" should {
    "return 501 NotImplemented" in {
      val result = route(app, FakeRequest(DELETE, "/api/v2/profiledata/AR-B-SIGE-1")).get
      status(result) mustBe NOT_IMPLEMENTED
    }
  }
}
