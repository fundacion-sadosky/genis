package integration.controllers

import configdata.CategoryService
import fixtures.{StubCacheService, StubCategoryService, StubLdapHealthService, StubMongoHealthService, StubProfileService, StubProfileExporterService, StubLimsArchivesExporterService}
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import profile.*
import security.{StubUserRepository, StubRoleRepository, UserRepository}
import services.CacheService
import types.SampleCode
import user.{LdapHealthService, RoleRepository, UsersModule}

import scala.concurrent.Future

class ProfilesControllerTest extends PlaySpec with GuiceOneAppPerTest {

  private var profileStub: StubProfileService = _
  private var exportStub: StubProfileExporterService = _
  private var limsStub: StubLimsArchivesExporterService = _
  private var cacheStub: StubCacheService = _

  override def fakeApplication(): Application = {
    profileStub = new StubProfileService
    exportStub = new StubProfileExporterService
    limsStub = new StubLimsArchivesExporterService
    cacheStub = new StubCacheService

    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[ProfileModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[ProfileService].toInstance(profileStub),
        bind[ProfileExporterService].toInstance(exportStub),
        bind[LimsArchivesExporterService].toInstance(limsStub),
        bind[CategoryService].toInstance(new StubCategoryService),
        bind[CacheService].toInstance(cacheStub),
        bind[LdapHealthService].toInstance(new StubLdapHealthService),
        bind[MongoHealthService].toInstance(new StubMongoHealthService)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()
  }

  // ─── GET endpoints ─────────────────────────────────────────────────

  "GET /api/v2/profiles-labelsets" must {
    "return 200 with label sets" in {
      val result = route(app, FakeRequest(GET, "/api/v2/profiles-labelsets")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val json = contentAsJson(result)
      (json \ "set1" \ "1" \ "caption").as[String] mustBe "Víctima"
    }
  }

  "GET /api/v2/profiles/:id" must {
    "return 200 when profile exists" in {
      val result = route(app, FakeRequest(GET, "/api/v2/profiles/AR-B-IMBICE-1")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      (contentAsJson(result) \ "globalCode").as[String] mustBe "AR-B-IMBICE-1"
    }

    "return 404 when profile not found" in {
      profileStub.findByCodeResult = Future.successful(None)
      val result = route(app, FakeRequest(GET, "/api/v2/profiles/AR-B-IMBICE-999")).get
      status(result) mustBe NOT_FOUND
    }
  }

  "GET /api/v2/profiles" must {
    "return 200 when profiles found" in {
      val result = route(app, FakeRequest(GET, "/api/v2/profiles?globalCodes=AR-B-IMBICE-1")).get
      status(result) mustBe OK
    }

    "return 404 when no profiles found" in {
      profileStub.findByCodesResult = Future.successful(Seq.empty)
      val result = route(app, FakeRequest(GET, "/api/v2/profiles?globalCodes=AR-B-IMBICE-999")).get
      status(result) mustBe NOT_FOUND
    }
  }

  "GET /api/v2/profiles/full/:globalCode" must {
    "return 200 with profile model view" in {
      val result = route(app, FakeRequest(GET, "/api/v2/profiles/full/AR-B-IMBICE-1")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      (contentAsJson(result) \ "associable").as[Boolean] mustBe true
    }
  }

  "GET /api/v2/profiles/:profileId/epgs" must {
    "return 200 with electropherogram list" in {
      val result = route(app, FakeRequest(GET, "/api/v2/profiles/AR-B-IMBICE-1/epgs")).get
      status(result) mustBe OK
      val json = contentAsJson(result)
      json.as[List[FileUploadedType]].head.fileId mustBe "epg1"
    }
  }

  "GET /api/v2/profiles/:profileId/epg/:epgId" must {
    "return 200 with binary data when found" in {
      val result = route(app, FakeRequest(GET, "/api/v2/profiles/AR-B-IMBICE-1/epg/epg1")).get
      status(result) mustBe OK
      contentAsBytes(result).length mustBe 3
    }

    "return 404 when not found" in {
      profileStub.getEpgImageResult = Future.successful(None)
      val result = route(app, FakeRequest(GET, "/api/v2/profiles/AR-B-IMBICE-1/epg/missing")).get
      status(result) mustBe NOT_FOUND
    }
  }

  "GET /api/v2/profiles/:profileId/analysis/:analysisId/epgs" must {
    "return 200 with list" in {
      val result = route(app, FakeRequest(GET, "/api/v2/profiles/AR-B-IMBICE-1/analysis/a1/epgs")).get
      status(result) mustBe OK
    }
  }

  "GET /api/v2/profiles-labels" must {
    "return 200 with labels" in {
      val result = route(app, FakeRequest(GET, "/api/v2/profiles-labels?globalCode=AR-B-IMBICE-1")).get
      status(result) mustBe OK
    }
  }

  "GET /api/v2/profiles-readOnly" must {
    "return 200 with readOnly status" in {
      val result = route(app, FakeRequest(GET, "/api/v2/profiles-readOnly?globalCode=AR-B-IMBICE-1")).get
      status(result) mustBe OK
      (contentAsJson(result) \ "isReadOnly").as[Boolean] mustBe false
    }
  }

  "GET /api/v2/profiles/subcategory-relationships/:subcategoryId" must {
    "return 200 with matching rules" in {
      val result = route(app, FakeRequest(GET, "/api/v2/profiles/subcategory-relationships/CAT_A")).get
      status(result) mustBe OK
    }
  }

  "GET /api/v2/profiles/:profileId/file" must {
    "return 200 with file list" in {
      val result = route(app, FakeRequest(GET, "/api/v2/profiles/AR-B-IMBICE-1/file")).get
      status(result) mustBe OK
    }
  }

  "GET /api/v2/profiles/:profileId/file/:fileId" must {
    "return 200 with binary data" in {
      val result = route(app, FakeRequest(GET, "/api/v2/profiles/AR-B-IMBICE-1/file/f1")).get
      status(result) mustBe OK
      contentAsBytes(result).length mustBe 3
    }

    "return 404 when not found" in {
      profileStub.getFileResult = Future.successful(None)
      val result = route(app, FakeRequest(GET, "/api/v2/profiles/AR-B-IMBICE-1/file/missing")).get
      status(result) mustBe NOT_FOUND
    }
  }

  "GET /api/v2/profiles/:profileId/analysis/:analysisId/file" must {
    "return 200 with file list" in {
      val result = route(app, FakeRequest(GET, "/api/v2/profiles/AR-B-IMBICE-1/analysis/a1/file")).get
      status(result) mustBe OK
    }
  }

  "GET /api/v2/get-profile-export/:user" must {
    "return 200 with file download" in {
      val result = route(app, FakeRequest(GET, "/api/v2/get-profile-export/testuser")).get
      status(result) mustBe OK
    }
  }

  "GET /api/v2/get-alta-file-export" must {
    "return 200 with alta file download" in {
      val result = route(app, FakeRequest(GET, "/api/v2/get-alta-file-export")).get
      status(result) mustBe OK
    }
  }

  "GET /api/v2/get-match-file-export" must {
    "return 200 with match file download" in {
      val result = route(app, FakeRequest(GET, "/api/v2/get-match-file-export")).get
      status(result) mustBe OK
    }
  }

  // ─── POST endpoints ────────────────────────────────────────────────

  "POST /api/v2/profiles" must {
    "return 200 with created profile on valid JSON" in {
      val body = Json.obj(
        "globalCode" -> "AR-B-IMBICE-1",
        "userId" -> "user1",
        "token" -> "token123",
        "genotypification" -> Json.obj("CSF1PO" -> Json.arr("10", "12")),
        "contributors" -> 1,
        "mismatches" -> None.orNull
      )
      val result = route(app, FakeRequest(POST, "/api/v2/profiles").withJsonBody(body)).get
      status(result) mustBe OK
      header("X-CREATED-ID", result) mustBe Some("AR-B-IMBICE-1")
    }

    "return 400 on invalid JSON" in {
      val result = route(app, FakeRequest(POST, "/api/v2/profiles").withJsonBody(Json.obj())).get
      status(result) mustBe BAD_REQUEST
    }

    "return 400 when service returns errors" in {
      profileStub.createResult = Future.successful(Left(List("error1", "error2")))
      val body = Json.obj(
        "globalCode" -> "AR-B-IMBICE-1",
        "userId" -> "user1",
        "token" -> "token123",
        "genotypification" -> Json.obj("CSF1PO" -> Json.arr("10")),
        "contributors" -> 1,
        "mismatches" -> None.orNull
      )
      val result = route(app, FakeRequest(POST, "/api/v2/profiles").withJsonBody(body)).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "POST /api/v2/profiles-xxx/:id (storeUploadedAnalysis)" must {
    "return 400 when no cached analysis" in {
      val result = route(app, FakeRequest(POST, "/api/v2/profiles-xxx/missingToken")).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "POST /api/v2/profiles-labels" must {
    "return 200 on valid save labels request" in {
      val body = Json.obj(
        "globalCode" -> "AR-B-IMBICE-1",
        "labeledGenotypification" -> Json.obj(
          "Víctima" -> Json.obj("CSF1PO" -> Json.arr("10"))
        )
      )
      val req = FakeRequest(POST, "/api/v2/profiles-labels")
        .withJsonBody(body)
        .withHeaders("X-USER" -> "user1")
      val result = route(app, req).get
      status(result) mustBe OK
      (contentAsJson(result) \ "result").as[Boolean] mustBe true
    }

    "return 400 on invalid JSON" in {
      val result = route(app, FakeRequest(POST, "/api/v2/profiles-labels").withJsonBody(Json.obj())).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "POST /api/v2/profiles-mixture-verification" must {
    "return 200 with result on valid input" in {
      val body = Json.obj(
        "mixtureGenotypification" -> Json.obj("1" -> Json.obj("CSF1PO" -> Json.arr("10"))),
        "profile" -> "AR-B-IMBICE-1",
        "subcategoryId" -> "SOSPECHOSO"
      )
      val result = route(app, FakeRequest(POST, "/api/v2/profiles-mixture-verification").withJsonBody(body)).get
      status(result) mustBe OK
      (contentAsJson(result) \ "result").as[Boolean] mustBe true
    }

    "return 400 on invalid JSON" in {
      val result = route(app, FakeRequest(POST, "/api/v2/profiles-mixture-verification").withJsonBody(Json.obj())).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "POST /api/v2/profiles-epg" must {
    "return 200 on successful save" in {
      val result = route(app, FakeRequest(POST,
        "/api/v2/profiles-epg?token=t&globalCode=AR-B-IMBICE-1&idAnalysis=a1&name=test")).get
      status(result) mustBe OK
    }
  }

  "POST /api/v2/profiles-file" must {
    "return 200 on successful save" in {
      val result = route(app, FakeRequest(POST,
        "/api/v2/profiles-file?token=t&globalCode=AR-B-IMBICE-1&idAnalysis=a1&name=test")).get
      status(result) mustBe OK
    }
  }

  "POST /api/v2/profile-export" must {
    "return 400 on invalid JSON" in {
      val result = route(app, FakeRequest(POST, "/api/v2/profile-export").withJsonBody(Json.obj())).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "POST /api/v2/profile-exportToLims" must {
    "return 400 on invalid JSON" in {
      val result = route(app, FakeRequest(POST, "/api/v2/profile-exportToLims").withJsonBody(Json.obj())).get
      status(result) mustBe BAD_REQUEST
    }
  }

  // ─── DELETE endpoints ──────────────────────────────────────────────

  "DELETE /api/v2/profiles-file/file/:fileId" must {
    "return 200 on successful remove" in {
      val req = FakeRequest(DELETE, "/api/v2/profiles-file/file/f1").withHeaders("X-USER" -> "user1")
      val result = route(app, req).get
      status(result) mustBe OK
      (contentAsJson(result) \ "fileId").as[String] mustBe "ok"
    }

    "return 400 when removal fails" in {
      profileStub.removeFileResult = Future.successful(Left("not found"))
      val req = FakeRequest(DELETE, "/api/v2/profiles-file/file/f1").withHeaders("X-USER" -> "user1")
      val result = route(app, req).get
      status(result) mustBe BAD_REQUEST
    }
  }

  "DELETE /api/v2/profiles-file/epg/:fileId" must {
    "return 200 on successful remove" in {
      val req = FakeRequest(DELETE, "/api/v2/profiles-file/epg/e1").withHeaders("X-USER" -> "user1")
      val result = route(app, req).get
      status(result) mustBe OK
    }
  }

  "DELETE /api/v2/profiles/:profileId" must {
    "return 200 on successful delete" in {
      val result = route(app, FakeRequest(DELETE, "/api/v2/profiles/AR-B-IMBICE-1")).get
      status(result) mustBe OK
      (contentAsJson(result) \ "message").as[String] mustBe "Profile removed successfully"
    }

    "return 400 when delete fails" in {
      profileStub.removeProfileResult = Future.successful(Left("cannot delete"))
      val result = route(app, FakeRequest(DELETE, "/api/v2/profiles/AR-B-IMBICE-1")).get
      status(result) mustBe BAD_REQUEST
    }
  }
}
