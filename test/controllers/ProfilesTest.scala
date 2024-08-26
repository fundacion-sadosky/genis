package controllers

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import scala.concurrent.Future
import org.apache.xalan.xsltc.compiler.ForEach
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.specs2.matcher.ValueCheck.valueIsTypedValueCheck
import org.specs2.mutable.Specification
import specs.PdgSpec
import stubs.Stubs
import profile.{Allele, FileUploadedType, NewAnalysis, Profile, ProfileModelView, ProfileService}
import services.CacheService
import types.SampleCode
import profiledata.ProfileDataRepository
import configdata.CategoryService

class ProfilesTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val mockProfile = Stubs.newProfile

  "A Profile controller" must {
    "parse a NewAnalysis" in {

      val mockCategoryService = mock[CategoryService]
      val mockProfileService = mock[ProfileService]
      when(mockProfileService.create(Stubs.newAnalysis)).thenReturn(Future.successful(Right(mockProfile)))
      when(mockProfileService.findByCode(Stubs.newAnalysis.globalCode)).thenReturn(Future.successful(None))

      val jsRequest = Json.obj("globalCode" -> Stubs.newAnalysis.globalCode.text,
        "userId" -> Stubs.newAnalysis.userId,
        "token" -> "token",
        "kit" -> Stubs.newAnalysis.kit,
        "genotypification" -> Stubs.newAnalysis.genotypification,
        "contributors" -> 1)

      val request = FakeRequest().withBody(jsRequest)

      val target: Profiles = new Profiles(mockCategoryService, mockProfileService, null,null, null)

      val result: Future[Result] = target.create().apply(request)

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"
      contentAsJson(result).as[Profile] mustBe mockProfile
    }
  }

  "A Profile controller" must {
    "fail if the globalCode has an incorrect sampleCode format" in {

      val mockCategoryService = mock[CategoryService]

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.create(Stubs.newAnalysis)).thenReturn(Future.successful(Right(mockProfile)))
      when(mockProfileService.findByCode(Stubs.newAnalysis.globalCode)).thenReturn(Future.successful(None))

      val jsRequest = Json.obj("globalCode" -> "mal sample code",
        "userId" -> Stubs.newAnalysis.userId,
        "token" -> "token",
        "kit" -> Stubs.newAnalysis.kit,
        "genotypification" -> Stubs.newAnalysis.genotypification)

      val request = FakeRequest().withBody(jsRequest)
      val target: Profiles = new Profiles(mockCategoryService, mockProfileService, null,null, null)
      val result: Future[Result] = target.create().apply(request)

      status(result) mustBe 400
      contentType(result).get mustBe "application/json"
    }
  }

  "A Profile controller" must {
    "findByCode a profile when an existing profileId is provided" in {

      val p = Stubs.newProfile
      val mockCategoryService = mock[CategoryService]
      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(p.globalCode)).thenReturn(Future.successful(Option(p)))

      val target: Profiles = new Profiles(mockCategoryService, mockProfileService, null,null,null)

      val result: Future[Result] = target.findByCode(p.globalCode).apply(FakeRequest())

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"

      val pRet = contentAsJson(result).as[Profile]
      pRet.globalCode mustBe p.globalCode
    }
  }

  "A Profile controller" must {
    "return NotFound when findByCode is executed with an unexisting profileId" in {

      val mockCategoryService = mock[CategoryService]

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(Stubs.sampleCode)).thenReturn(Future.successful(None))

      val target: Profiles = new Profiles(mockCategoryService, mockProfileService, null,null,null)

      val result: Future[Result] = target.findByCode(Stubs.sampleCode).apply(FakeRequest())

      status(result) mustBe 404 //not found
    }
  }

  "A Profile controller" must {
    "return a List(String) with Electropherograms ids" in {

      val expected = List[FileUploadedType](FileUploadedType("im1","name1"), FileUploadedType("im2","name2"), FileUploadedType("im3","name3"))
      val mockCategoryService = mock[CategoryService]
      val mockProfileService = mock[ProfileService]
      when(mockProfileService.getElectropherogramsByCode(Stubs.sampleCode)).thenReturn(Future.successful(expected))

      val target: Profiles = new Profiles(mockCategoryService, mockProfileService, null,null,null)

      val result: Future[Result] = target.getElectropherogramsByCode(Stubs.sampleCode).apply(FakeRequest())

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"
      contentAsJson(result).as[List[FileUploadedType]] mustBe expected
    }
  }

  "A Profile controller" must {
    "return NotFound when the electropherograms ids and profileId are unexisting" in {

      val mockCategoryService = mock[CategoryService]

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.getElectropherogramImage(Stubs.sampleCode, "epgId")).thenReturn(Future.successful(None))

      val target: Profiles = new Profiles(mockCategoryService, mockProfileService, null,null,null)
      val result: Future[Result] = target.getElectropherogramImage(Stubs.sampleCode, "epgId").apply(FakeRequest())

      status(result) mustBe 404 //not found
    }
  }

  "A Profile controller" must {
    "return an array of Bytes when the electropherograms ids and profileId are valid" in {

      val expected: Array[Byte] = new Array[Byte](0)
      val mockCategoryService = mock[CategoryService]
      val mockProfileService = mock[ProfileService]
      when(mockProfileService.getElectropherogramImage(Stubs.sampleCode, "epgId")).thenReturn(Future.successful(Option(expected)))

      val target: Profiles = new Profiles(mockCategoryService, mockProfileService, null,null,null)
      val result: Future[Result] = target.getElectropherogramImage(Stubs.sampleCode, "epgId").apply(FakeRequest())

      status(result) mustBe OK
      contentAsBytes(result) mustBe expected
    }
  }

  "A Profile controller" must {
    "return electropherograms ids when valid sample code and analysisId are provided" in {

      val expected: List[FileUploadedType] = List[FileUploadedType](FileUploadedType("ImageId1","name1"), FileUploadedType("ImageId2","name2"), FileUploadedType("ImageId3","name3"))
      val mockCategoryService = mock[CategoryService]
      val mockProfileService = mock[ProfileService]
      when(mockProfileService.getElectropherogramsByAnalysisId(Stubs.sampleCode, "analysisId")).thenReturn(Future.successful(expected))

      val target: Profiles = new Profiles(mockCategoryService, mockProfileService, null,null,null)
      val result: Future[Result] = target.getElectropherogramsByAnalysisId(Stubs.sampleCode, "analysisId").apply(FakeRequest())

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"
      contentAsJson(result).as[List[FileUploadedType]] mustBe expected
    }
  }

//  "A Profile controller" must {
//    "return a Json with the profile data and the EfgsIds" in {
//
//      val p = Stubs.newProfile
//      
//      val e = ProfileModelView(p.globalCode, p.globalCode, p.categoryId, p.genotypification, None, None, true, true, true)
//      val expected: JsObject = Json.obj("_id" -> p.globalCode.text,
//        "globalCode" -> p.globalCode.text,
//        "categoryId" -> p.categoryId,
//        "categoryId" -> p.categoryId,
//        "genotypification" -> p.genotypification,
//        "labelable" -> true,
//        "editable" -> true)
//
//      val mockCategoryService = mock[CategoryService]
//
//      val mockProfileService = mock[ProfileService]
//      when(mockProfileService.getProfileModelView(Stubs.sampleCode)).thenReturn(Future.successful(Option(e)))
//      //when(mockProfileService.enabledActions(Stubs.sampleCode)).thenReturn(Future.successful((true, true)))
//
//      val target: Profiles = new Profiles(mockCategoryService, mockProfileService, null,null)
//      val result: Future[Result] = target.getFullProfile(Stubs.sampleCode).apply(FakeRequest())
//
//      status(result) mustBe OK
//      contentType(result).get mustBe "application/json"
//      contentAsJson(result).as[JsObject] mustBe expected
//    }
//  }

  "A Profile controller" must {
    "parse a NewAnalysis with Some(labeledGenotypification) and Some(contributors)" in {

      val stubNa = Stubs.newAnalysis

      val na = NewAnalysis(stubNa.globalCode, stubNa.userId, stubNa.token, stubNa.kit, None,
        Map("M1" -> List(Allele(1), Allele(2))),
        Some(Map("label1" -> Map("M1" -> List(Allele(1), Allele(2))))),
        Some(1), None)

      val mockCategoryService = mock[CategoryService]

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.create(na)).thenReturn(Future.successful(Right(mockProfile)))
      when(mockProfileService.findByCode(na.globalCode)).thenReturn(Future.successful(None))

      val jsRequest = Json.obj("globalCode" -> Stubs.newAnalysis.globalCode.text,
        "userId" -> Stubs.newAnalysis.userId,
        "token" -> "token",
        "kit" -> Stubs.newAnalysis.kit,
        "genotypification" -> Json.obj("M1" -> List(1, 2)),
        "labeledGenotypification" -> Json.obj("label1" -> Json.obj("M1" -> List(1, 2))),
        "contributors" -> 1)

      val request = FakeRequest().withBody(jsRequest)

      val target: Profiles = new Profiles(mockCategoryService, mockProfileService, null,null,null)

      val result: Future[Result] = target.create().apply(request)

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"
      contentAsJson(result).as[Profile] mustBe mockProfile
    }
  }

}
