package unit.audit

import java.util.Date

import scala.concurrent.{ExecutionContext, Future}

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import audit.*
import controllers.OperationLogsController
import types.TotpToken

class OperationLogsControllerSpec extends AnyWordSpec with Matchers with MockitoSugar {

  given ec: ExecutionContext = ExecutionContext.global

  private def controller(service: OperationLogService): OperationLogsController =
    new OperationLogsController(stubControllerComponents(), service)

  private val now    = new Date()
  private val lotView = OperationLogLotView(1L, now)

  private def makeEntry(idx: Long): OperationLogEntry =
    OperationLogEntry(
      index       = idx,
      userId      = "user1",
      otp         = Some(TotpToken("123456")),
      timestamp   = now,
      method      = "GET",
      path        = "/api/v2/profiles",
      action      = "controllers.ProfilesController.findByCode()",
      buildNo     = "develop",
      result      = None,
      status      = 200,
      lotId       = 1L,
      description = "Ver perfil"
    )

  // ── getLogLots ────────────────────────────────────────────────────

  "OperationLogsController.getLogLots" must {
    "return 200 with JSON array of lot views" in {
      val svc = mock[OperationLogService]
      when(svc.listLotsView(0, 10)).thenReturn(Future.successful(Seq(lotView)))

      val result = controller(svc).getLogLots(0, 10)(FakeRequest())
      status(result) mustBe OK
      val json = contentAsJson(result)
      (json \ 0 \ "id").as[Long] mustBe 1L
    }
  }

  // ── getTotalLots ──────────────────────────────────────────────────

  "OperationLogsController.getTotalLots" must {
    "return 200 with X-LOTS-LENGTH header" in {
      val svc = mock[OperationLogService]
      when(svc.getLotsLength()).thenReturn(Future.successful(5))

      val result = controller(svc).getTotalLots(FakeRequest())
      status(result) mustBe OK
      header("X-LOTS-LENGTH", result) mustBe Some("5")
    }
  }

  // ── getTotalLogs ──────────────────────────────────────────────────

  "OperationLogsController.getTotalLogs" must {
    "return 200 with X-LOGS-LENGTH header for valid JSON" in {
      val svc    = mock[OperationLogService]
      val search = OperationLogSearch(lotId = 1L)
      when(svc.getLogsLength(search)).thenReturn(Future.successful(42))

      val body   = Json.toJson(search)
      val result = controller(svc).getTotalLogs(FakeRequest().withBody(body))
      status(result) mustBe OK
      header("X-LOGS-LENGTH", result) mustBe Some("42")
    }

    "return 400 for invalid JSON body" in {
      val svc    = mock[OperationLogService]
      val result = controller(svc).getTotalLogs(FakeRequest().withBody(Json.obj()))
      status(result) mustBe BAD_REQUEST
    }
  }

  // ── searchLogs ────────────────────────────────────────────────────

  "OperationLogsController.searchLogs" must {
    "return 200 with JSON array of entries" in {
      val svc    = mock[OperationLogService]
      val search = OperationLogSearch(lotId = 1L)
      when(svc.searchLogs(search)).thenReturn(Future.successful(Seq(makeEntry(0L))))

      val body   = Json.toJson(search)
      val result = controller(svc).searchLogs(FakeRequest().withBody(body))
      status(result) mustBe OK
      val json = contentAsJson(result)
      (json \ 0 \ "userId").as[String] mustBe "user1"
    }

    "return 400 for invalid JSON body" in {
      val svc    = mock[OperationLogService]
      val result = controller(svc).searchLogs(FakeRequest().withBody(Json.obj()))
      status(result) mustBe BAD_REQUEST
    }
  }

  // ── checkLogLot ───────────────────────────────────────────────────

  "OperationLogsController.checkLogLot" must {
    "return 200 (empty) when lot integrity is valid" in {
      val svc = mock[OperationLogService]
      when(svc.checkLot(1L)).thenReturn(Future.successful(Right(())))

      val result = controller(svc).checkLogLot(1L)(FakeRequest())
      status(result) mustBe OK
      contentAsString(result) mustBe ""
    }

    "return 200 with error detail when lot is tampered" in {
      val svc    = mock[OperationLogService]
      val badKey = Key(Seq.fill(32)(0xff.toByte))
      val signed = SignedOperationLogEntry(
        index       = 5L,
        userId      = "user1",
        otp         = None,
        timestamp   = now,
        method      = "GET",
        path        = "/api/v2/profiles",
        action      = "controllers.ProfilesController.findByCode()",
        buildNo     = "develop",
        result      = None,
        status      = 200,
        lotId       = 1L,
        signature   = badKey,
        description = "Ver perfil"
      )
      when(svc.checkLot(1L)).thenReturn(Future.successful(Left((signed, badKey))))

      val result = controller(svc).checkLogLot(1L)(FakeRequest())
      status(result) mustBe OK
      val json = contentAsJson(result)
      (json \ "expectedSignature").as[String] mustBe badKey.asHexaString()
    }
  }
}
