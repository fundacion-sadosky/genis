package controllers

import configdata.CategoryService
import specs.PdgSpec
import stubs.Stubs
import org.mockito.Mockito.when
import org.mockito.Matchers.any
import org.scalatest.mock.MockitoSugar

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Result
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers.OK
import play.api.test.Helpers.contentAsJson
import play.api.test.Helpers.contentType
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status

import java.util.Date
import profiledata.ProfileDataService
import profile.ProfileService
import types._
import profiledata.ProfileDataAttempt

class ProfileDataTest extends PdgSpec with MockitoSugar with Results {

  val sampleCode = new SampleCode("AR-C-SHDG-1")
  val expectedCodigoMuestra: Future[SampleCode] = Future.successful(sampleCode)
  val eitherCodigoMuestra: Future[Either[String, SampleCode]] = Future.successful(Right(sampleCode))
  val pda = Stubs.profileDataAttempt

  "Profiles controller" must {
    "accept a Json by post, parse it as a ProfileData model class and return the sampleCode with success status code and valid headers" in {

      val jsRequest = Json.obj("category" -> pda.category,
        "category" -> pda.category,
        "attorney" -> pda.attorney,
        "bioMaterialType" -> pda.bioMaterialType,
        "court" -> pda.court,
        "crimeInvolved" -> pda.crimeInvolved,
        "crimeType" -> pda.crimeType,
        "criminalCase" -> pda.criminalCase,
        "internalSampleCode" -> pda.internalSampleCode,
        "assignee" -> pda.assignee,
        "laboratory" -> pda.laboratory,
        "responsibleGeneticist" -> pda.responsibleGeneticist,
        "profileExpirationDate" -> pda.profileExpirationDate,
        "sampleDate" -> pda.sampleDate,
        "sampleEntryDate" -> pda.sampleEntryDate)

      val mockCategoryService = mock[CategoryService]
      val mockProfileService = mock[ProfileService]
      val mockProfileDataService = mock[ProfileDataService]
      when(mockProfileDataService.create(any[ProfileDataAttempt])).thenReturn(eitherCodigoMuestra)

      val request = FakeRequest().withBody(jsRequest)
      val target = new ProfileData(
        mockProfileDataService,
        mockProfileService,
        mockCategoryService
      )

      val result: Future[Result] = target.create().apply(request)

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"

      val jsonCodigoMuestra = contentAsJson(result).as[JsValue]
      (jsonCodigoMuestra \ "sampleCode").as[SampleCode] mustBe sampleCode
    }

    "reject a json if it doesn't have the expected format" in {

      val mockCategoryService = mock[CategoryService]
      val mockProfileDataService = mock[ProfileDataService]
      val mockProfileService = mock[ProfileService]
      when(mockProfileDataService.create(any[ProfileDataAttempt])).thenReturn(eitherCodigoMuestra)

      val request = FakeRequest().withBody(Json.obj("categoryBadJSon" -> pda.category, "category" -> pda.category))

      val target = new ProfileData(
        mockProfileDataService,
        mockProfileService,
        mockCategoryService
      )
      val result: Future[Result] = target.create().apply(request)

      status(result) mustBe 400 //BadRequest
      contentType(result).get mustBe "application/json"
    }

    "return a profileData with the valid format" in {

      val pd = Stubs.profileData
      val mockCategoryService = mock[CategoryService]
      val mockProfileDataService = mock[ProfileDataService]
      val mockProfileService = mock[ProfileService]
      when(mockProfileDataService.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(pd)))

      val target = new ProfileData(
        mockProfileDataService,
        mockProfileService,
        mockCategoryService
      )
      val result: Future[Result] = target.findByCode(pd.globalCode).apply(FakeRequest())

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"

      val jsonValue = contentAsJson(result).as[JsValue]
      (jsonValue \ "category").as[AlphanumericId] mustBe pd.category
      (jsonValue \ "globalCode").as[SampleCode] mustBe pd.globalCode
      (jsonValue \ "attorney").as[Option[String]] mustBe pd.attorney
      (jsonValue \ "bioMaterialType").as[Option[String]] mustBe pd.bioMaterialType
      (jsonValue \ "court").as[Option[String]] mustBe pd.court
      (jsonValue \ "crimeInvolved").as[Option[String]] mustBe pd.crimeInvolved
      (jsonValue \ "crimeType").as[Option[String]] mustBe pd.crimeType
      (jsonValue \ "criminalCase").as[Option[String]] mustBe pd.criminalCase
      (jsonValue \ "internalSampleCode").as[String] mustBe pd.internalSampleCode
      (jsonValue \ "laboratory").as[String] mustBe pd.laboratory
      (jsonValue \ "responsibleGeneticist").as[Option[String]] mustBe pd.responsibleGeneticist
      (jsonValue \ "profileExpirationDate").as[Option[Date]] mustBe pd.profileExpirationDate
      (jsonValue \ "sampleDate").as[Option[Date]] mustBe pd.sampleDate
      (jsonValue \ "sampleEntryDate").as[Option[Date]] mustBe pd.sampleEntryDate
      (jsonValue \ "dataFiliation" \ "identification").as[String] mustBe pd.dataFiliation.get.identification
      (jsonValue \ "dataFiliation" \ "address").as[String] mustBe pd.dataFiliation.get.address
      (jsonValue \ "dataFiliation" \ "nationality").as[String] mustBe pd.dataFiliation.get.nationality
      (jsonValue \ "dataFiliation" \ "fullName").as[String] mustBe pd.dataFiliation.get.fullName
      (jsonValue \ "dataFiliation" \ "inprints").as[Array[String]] mustBe Array[String]()
      (jsonValue \ "dataFiliation" \ "pictures").as[Array[String]] mustBe Array[String]()
    }
  }

}