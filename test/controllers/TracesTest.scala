package controllers

import java.util.Date

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import specs.PdgSpec
import trace.{ProfileDataInfo, Trace, TraceSearch, TraceService}
import types.SampleCode

import scala.concurrent.Future

class TracesTest extends PdgSpec with MockitoSugar with Results {

  val trace: Trace = Trace(SampleCode("AR-B-SHDG-1"), "assignee", new Date(), ProfileDataInfo)
  val traceSearch = TraceSearch(0, 30, SampleCode("AR-B-SHDG-1"), "pdg", false)

  "search - ok" in {
    val traceService = mock[TraceService]
    when(traceService.search(any[TraceSearch])).thenReturn(Future.successful(Seq(trace)))

    val target = new Traces(traceService)

    val jsRequest = Json.toJson(traceSearch)
    val request = FakeRequest().withBody(jsRequest)
    val result: Future[Result] = target.search().apply(request)

    status(result) mustBe OK
  }

  "search - bad request" in {
    val traceService = mock[TraceService]
    when(traceService.search(any[TraceSearch])).thenReturn(Future.successful(Seq(trace)))

    val target = new Traces(traceService)

    val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
    val result: Future[Result] = target.search().apply(request)

    status(result) mustBe BAD_REQUEST
  }

  "count - ok" in {
    val traceService = mock[TraceService]
    when(traceService.count(any[TraceSearch])).thenReturn(Future.successful(4))

    val target = new Traces(traceService)

    val jsRequest = Json.toJson(traceSearch)
    val request = FakeRequest().withBody(jsRequest)
    val result: Future[Result] = target.count().apply(request)

    status(result) mustBe OK
    header("X-TRACE-LENGTH", result).get mustBe "4"
  }

  "count - bad request" in {
    val traceService = mock[TraceService]
    when(traceService.count(any[TraceSearch])).thenReturn(Future.successful(4))

    val target = new Traces(traceService)

    val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
    val result: Future[Result] = target.count().apply(request)

    status(result) mustBe BAD_REQUEST
  }

  "get full description" in {
    val traceService = mock[TraceService]
    when(traceService.getFullDescription(any[Long])).thenReturn(Future.successful("Description"))

    val target = new Traces(traceService)

    val result: Future[Result] = target.getFullDescription(1).apply(FakeRequest())

    status(result) mustBe OK
  }

}
