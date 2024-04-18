package disclaimer

import connections.{Connection, InterconnectionService}
import controllers.{DisclaimerController, Interconnections}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import specs.PdgSpec
import play.api.http.Status.{NOT_FOUND, OK}

import scala.concurrent.Future
import connections._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import specs.PdgSpec
import stubs.Stubs

import scala.concurrent.Future
class DisclaimerControllerTest extends PdgSpec with MockitoSugar with Results {
  "DisclaimerControllerTest" must {

    "get disclaimer no ok" in {
      val disclaimerServiceImpl = mock[DisclaimerServiceImpl]
      when(disclaimerServiceImpl.get()).
        thenReturn(Future.successful(Disclaimer(None)))

      val target = new DisclaimerController(disclaimerServiceImpl)

      val request = FakeRequest()

      val result: Future[Result] = target.getDisclaimer.apply(request)
      status(result) mustBe NOT_FOUND
    }

    "get disclaimer ok" in {
      val disclaimerServiceImpl = mock[DisclaimerServiceImpl]
      when(disclaimerServiceImpl.get()).
        thenReturn(Future.successful(Disclaimer(Some("hola"))))

      val target = new DisclaimerController(disclaimerServiceImpl)

      val request = FakeRequest()

      val result: Future[Result] = target.getDisclaimer.apply(request)
      status(result) mustBe OK
    }
  }
}
