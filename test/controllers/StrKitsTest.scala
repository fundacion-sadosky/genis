package controllers

import kits.{FullLocus, FullStrKit, LocusService, StrKitService}
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

class StrKitsTest extends PdgSpec with MockitoSugar with Results {

  "StrKits controller" must {

    "list full kits" in {

      val kitService = mock[StrKitService]
      when(kitService.listFull()).thenReturn(Future.successful(Stubs.fullKits))

      val target = new StrKits(kitService)
      val result: Future[Result] = target.listFull().apply(FakeRequest())

      status(result) mustBe OK
    }

    "list kits" in {

      val kitService = mock[StrKitService]
      when(kitService.list()).thenReturn(Future.successful(Stubs.strKits))

      val target = new StrKits(kitService)
      val result: Future[Result] = target.list().apply(FakeRequest())

      status(result) mustBe OK
    }

    "find loci by kit" in {
      val id = "KIT"

      val kitService = mock[StrKitService]
      when(kitService.findLociByKit(any[String])).thenReturn(Future.successful(Stubs.loci))

      val target = new StrKits(kitService)
      val result: Future[Result] = target.findLociByKit(id).apply(FakeRequest())

      status(result) mustBe OK
    }

    "delete kit - ok" in {
      val id = "KIT"

      val kitService = mock[StrKitService]
      when(kitService.delete(any[String])).thenReturn(Future.successful(Right(id)))

      val target = new StrKits(kitService)
      val result: Future[Result] = target.delete(id).apply(FakeRequest())

      status(result) mustBe OK
      header("X-CREATED-ID", result).get mustBe id
    }

    "delete kit - bad request" in {
      val id = "KIT"

      val kitService = mock[StrKitService]
      when(kitService.delete(any[String])).thenReturn(Future.successful(Left("Error message")))

      val target = new StrKits(kitService)
      val result: Future[Result] = target.delete(id).apply(FakeRequest())

      status(result) mustBe BAD_REQUEST
    }

    "add kit - ok" in {
      val id = Stubs.fullKits.head.id
      val json = Json.toJson(Stubs.fullKits.head)

      val kitService = mock[StrKitService]
      when(kitService.add(any[FullStrKit])).thenReturn(Future.successful(Right(id)))

      val target = new StrKits(kitService)
      val request = FakeRequest().withBody(json)
      val result: Future[Result] = target.add().apply(request)

      status(result) mustBe OK
      header("X-CREATED-ID", result).get mustBe id
    }

    "add kit - bad request" in {
      val id = Stubs.fullKits.head.id
      val json = Json.toJson(Stubs.fullKits.head)

      val kitService = mock[StrKitService]
      when(kitService.add(any[FullStrKit])).thenReturn(Future.successful(Left("Error message")))

      val target = new StrKits(kitService)
      val request = FakeRequest().withBody(json)
      val result: Future[Result] = target.add().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "add kit - invalid input" in {
      val json = Json.obj("KIT" -> "Powerplex")

      val target = new StrKits(null)
      val request = FakeRequest().withBody(json)
      val result: Future[Result] = target.add().apply(request)

      status(result) mustBe BAD_REQUEST
    }

  }


}
