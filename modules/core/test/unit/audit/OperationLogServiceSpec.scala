package unit.audit

import java.util.Date

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import audit.*
import types.TotpToken

class OperationLogServiceSpec extends AnyWordSpec with Matchers with MockitoSugar {

  given ec: ExecutionContext = ExecutionContext.global
  private val timeout = 5.seconds

  private val lotSize   = 10000
  private val chunkSize = 100

  private def makeEntry(idx: Long, lotId: Long): SignedOperationLogEntry =
    SignedOperationLogEntry(
      index       = idx,
      userId      = "user1",
      otp         = Some(TotpToken("123456")),
      timestamp   = new Date(),
      method      = "GET",
      path        = "/api/v2/profiles",
      action      = "controllers.ProfilesController.findByCode()",
      buildNo     = "develop",
      result      = None,
      status      = 200,
      lotId       = lotId,
      signature   = Key(Seq.fill(32)(0.toByte)),
      description = "Ver perfil"
    )

  "PeoOperationLogService.getLogsLength" must {
    "delegate to repository.countLogs" in {
      val repo   = mock[OperationLogRepository]
      val signer = mock[PEOSignerService]
      val search = OperationLogSearch(lotId = 1L)
      when(repo.countLogs(search)).thenReturn(Future.successful(42))

      val service = new PeoOperationLogService(repo, signer, lotSize, chunkSize)
      Await.result(service.getLogsLength(search), timeout) mustBe 42
    }
  }

  "PeoOperationLogService.getLotsLength" must {
    "delegate to repository.countLots" in {
      val repo   = mock[OperationLogRepository]
      val signer = mock[PEOSignerService]
      when(repo.countLots()).thenReturn(Future.successful(7))

      val service = new PeoOperationLogService(repo, signer, lotSize, chunkSize)
      Await.result(service.getLotsLength(), timeout) mustBe 7
    }
  }

  "PeoOperationLogService.searchLogs" must {
    "return OperationLogEntry list mapped from SignedOperationLogEntry" in {
      val repo   = mock[OperationLogRepository]
      val signer = mock[PEOSignerService]
      val search = OperationLogSearch(lotId = 1L)
      val signed = IndexedSeq(makeEntry(0L, 1L), makeEntry(1L, 1L))
      when(repo.searchLogs(search)).thenReturn(Future.successful(signed))

      val service = new PeoOperationLogService(repo, signer, lotSize, chunkSize)
      val result  = Await.result(service.searchLogs(search), timeout)

      result.length mustBe 2
      result.head.userId mustBe "user1"
      result.head.status mustBe 200
    }
  }

  "PeoOperationLogService.listLotsView" must {
    "return OperationLogLotView list without kZero" in {
      val repo   = mock[OperationLogRepository]
      val signer = mock[PEOSignerService]
      val now    = new Date()
      val lots   = Seq(
        OperationLogLot(1L, now, Key(Seq.fill(32)(0.toByte))),
        OperationLogLot(2L, now, Key(Seq.fill(32)(1.toByte)))
      )
      when(repo.listLots(10, 0)).thenReturn(Future.successful(lots))

      val service = new PeoOperationLogService(repo, signer, lotSize, chunkSize)
      val result  = Await.result(service.listLotsView(0, 10), timeout)

      result.length mustBe 2
      result.head.id mustBe 1L
      result(1).id mustBe 2L
    }
  }
}
