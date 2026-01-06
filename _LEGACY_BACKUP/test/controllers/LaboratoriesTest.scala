package controllers

import specs.PdgSpec
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Results
import stubs.Stubs
import play.api.libs.json.Json
import laboratories.LaboratoryService
import org.mockito.Mockito.when
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test.Helpers.contentAsJson
import play.api.test.Helpers.contentType
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status
import laboratories.Laboratory

class LaboratoriesTest extends PdgSpec with MockitoSugar with Results {

  val lab = Stubs.laboratory
  val seqLab = List(lab)

  "A Laboratory Controller" must {
    "add a laboratory" in {

      val jsRequest = Json.toJson(lab)
      val labServiceMock = mock[LaboratoryService]
      when(labServiceMock.add(lab)).thenReturn(Future.successful(Right("NME")))

      val request = FakeRequest().withBody(jsRequest)
      val target = new Laboratories(labServiceMock,null)
      val result: Future[Result] = target.addLab().apply(request)

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"

      val jsonVal = contentAsJson(result).as[String]

      jsonVal mustBe "NME"
    }

    "list the laboratories" in {

      val labServiceMock = mock[LaboratoryService]
      when(labServiceMock.list).thenReturn(Future.successful(seqLab))

      val request = FakeRequest()
      val target = new Laboratories(labServiceMock,null)
      val result: Future[Result] = target.list().apply(request)

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"

      val jsonVal = contentAsJson(result).as[Seq[Laboratory]]

      jsonVal mustBe seqLab
    }

    "get a laboratory case some" in {

      val labServiceMock = mock[LaboratoryService]
      when(labServiceMock.get("NME")).thenReturn(Future.successful(Some(lab)))

      val request = FakeRequest()
      val target = new Laboratories(labServiceMock,null)
      val result: Future[Result] = target.getLaboratory("NME").apply(request)

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"

      val jsonVal = contentAsJson(result).as[Option[Laboratory]]

      jsonVal mustBe Some(lab)
    }

    "get a laboratory case none" in {

      val labServiceMock = mock[LaboratoryService]
      when(labServiceMock.get("NME")).thenReturn(Future.successful(None))

      val request = FakeRequest()
      val target = new Laboratories(labServiceMock,null)
      val result: Future[Result] = target.getLaboratory("NME").apply(request)

      status(result) mustBe NO_CONTENT
    }

    "update a laboratory right" in {

      val jsRequest = Json.toJson(lab)
      val labServiceMock = mock[LaboratoryService]
      when(labServiceMock.update(lab)).thenReturn(Future.successful(Right("NME")))

      val request = FakeRequest().withBody(jsRequest)
      val target = new Laboratories(labServiceMock,null)
      val result: Future[Result] = target.updateLab().apply(request)

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"

      val jsonVal = contentAsJson(result).as[String]

      jsonVal mustBe "NME"
    }

    "update a laboratory left" in {

      val jsRequest = Json.toJson(lab)
      val labServiceMock = mock[LaboratoryService]
      when(labServiceMock.update(lab)).thenReturn(Future.successful(Left("ERROR")))

      val request = FakeRequest().withBody(jsRequest)
      val target = new Laboratories(labServiceMock,null)
      val result: Future[Result] = target.updateLab().apply(request)

      status(result) mustBe BAD_REQUEST
      contentType(result).get mustBe "application/json"

      val jsonVal = contentAsJson(result).as[String]

      jsonVal mustBe "ERROR"
    }
  }
}