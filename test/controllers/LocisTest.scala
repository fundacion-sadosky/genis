package controllers

import kits.{FullLocus, Locus, LocusService}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import specs.PdgSpec
import stubs.Stubs

import scala.concurrent.Future

class LocisTest extends PdgSpec with MockitoSugar with Results {

  "Locis controller" must {

    "list full locus" in {

      val locusService = mock[LocusService]
      when(locusService.listFull()).thenReturn(Future.successful(Stubs.fullLocus))

      val target = new Locis(locusService)
      val result: Future[Result] = target.listFull().apply(FakeRequest())

      status(result) mustBe OK
    }

    "list locus" in {

      val locusService = mock[LocusService]
      when(locusService.list()).thenReturn(Future.successful(Stubs.fullLocus.map(_.locus)))

      val target = new Locis(locusService)
      val result: Future[Result] = target.list().apply(FakeRequest())

      status(result) mustBe OK
    }

    "delete locus - ok" in {
      val id = "LOCUS"

      val locusService = mock[LocusService]
      when(locusService.delete(any[String])).thenReturn(Future.successful(Right(id)))

      val target = new Locis(locusService)
      val result: Future[Result] = target.delete(id).apply(FakeRequest())

      status(result) mustBe OK
      header("X-CREATED-ID", result).get mustBe id
    }

    "delete locus - bad request" in {
      val id = "LOCUS"

      val locusService = mock[LocusService]
      when(locusService.delete(any[String])).thenReturn(Future.successful(Left("Error message")))

      val target = new Locis(locusService)
      val result: Future[Result] = target.delete(id).apply(FakeRequest())

      status(result) mustBe BAD_REQUEST
    }

    "add locus - ok" in {
      val id = Stubs.fullLocus.head.locus.id
      val json = Json.toJson(Stubs.fullLocus.head)

      val locusService = mock[LocusService]
      when(locusService.add(any[FullLocus])).thenReturn(Future.successful(Right(id)))

      val target = new Locis(locusService)
      val request = FakeRequest().withBody(json)
      val result: Future[Result] = target.add().apply(request)

      status(result) mustBe OK
      header("X-CREATED-ID", result).get mustBe id
    }

    "update locus - ok" in {
      val json = Json.toJson(Stubs.fullLocus.head)

      val locusService = mock[LocusService]
      when(locusService.update(any[FullLocus])).thenReturn(Future.successful(Right(())))

      val target = new Locis(locusService)
      val request = FakeRequest().withBody(json)
      val result: Future[Result] = target.update().apply(request)

      status(result) mustBe OK
    }

    "add locus - bad request" in {
      val json = Json.toJson(Stubs.fullLocus.head)

      val locusService = mock[LocusService]
      when(locusService.add(any[FullLocus])).thenReturn(Future.successful(Left("Error message")))

      val target = new Locis(locusService)
      val request = FakeRequest().withBody(json)
      val result: Future[Result] = target.add().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "add locus - invalid input" in {
      val json = Json.obj("LOCUS" -> "TPOX")

      val target = new Locis(null)
      val request = FakeRequest().withBody(json)
      val result: Future[Result] = target.add().apply(request)

      status(result) mustBe BAD_REQUEST
    }

  }


}
