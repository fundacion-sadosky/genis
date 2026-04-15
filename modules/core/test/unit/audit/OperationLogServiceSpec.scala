package unit.audit

import java.util.Date

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import audit.*
import types.TotpToken
import fixtures.AuditFixtures

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

  "PeoOperationLogService.add" must {
    "delegate to signerService.addEntry with a PEOEntry whose build produces a signed entry with translated description" in {
      val repo    = mock[OperationLogRepository]
      val signer  = mock[PEOSignerService]
      when(signer.addEntry(any[PEOEntry[OperationLogEntryAttemp, SignedOperationLogEntry, Unit]])(any))
        .thenReturn(Future.successful(()))

      val service = new PeoOperationLogService(repo, signer, lotSize, chunkSize)
      val attempt = AuditFixtures.makeAttempt(
        userId = "alice", method = "GET", path = "/api/v2/profiles"
      )

      Await.result(service.add(attempt), timeout)

      val captor = ArgumentCaptor.forClass(classOf[PEOEntry[OperationLogEntryAttemp, SignedOperationLogEntry, Unit]])
      verify(signer).addEntry(captor.capture())(any)

      val peo      = captor.getValue
      val lotId    = 55L
      val zero     = AuditFixtures.zeroKey32
      val built    = peo.build(attempt, zero, lotId)

      built.userId      mustBe attempt.userId
      built.lotId       mustBe lotId
      built.description mustBe PermissionHelper.translatePermission(attempt.method, attempt.path)
      built.signature   mustBe Signature.computeSignature(attempt.stringify + lotId, zero)
    }
  }

  "PeoOperationLogService.checkLot" must {
    "return None when the lot does not exist" in {
      val repo   = mock[OperationLogRepository]
      val signer = mock[PEOSignerService]
      when(repo.getLot(99L)).thenReturn(Future.successful(None))

      val service = new PeoOperationLogService(repo, signer, lotSize, chunkSize)
      Await.result(service.checkLot(99L), timeout) mustBe None
    }

    "return Some(Right(())) when the lot exists but has 0 records" in {
      val repo   = mock[OperationLogRepository]
      val signer = mock[PEOSignerService]
      val lot    = OperationLogLot(1L, new Date(), AuditFixtures.zeroKey32)
      when(repo.getLot(1L)).thenReturn(Future.successful(Some(lot)))
      when(repo.countLogs(any[OperationLogSearch])).thenReturn(Future.successful(0))

      val service = new PeoOperationLogService(repo, signer, lotSize, chunkSize)
      Await.result(service.checkLot(1L), timeout) mustBe Some(Right(()))
    }

    "query pages with ascending=true, sortField=\"id\", pageSize=chunkSize (regression guard for #213)" in {
      val repo   = mock[OperationLogRepository]
      val signer = mock[PEOSignerService]
      val lot    = OperationLogLot(1L, new Date(), AuditFixtures.zeroKey32)

      when(repo.getLot(1L)).thenReturn(Future.successful(Some(lot)))
      when(repo.countLogs(any[OperationLogSearch])).thenReturn(Future.successful(1))
      // Build a valid 1-entry chain so PEOAlgorithm succeeds and we can inspect the search.
      val signed = AuditFixtures.makeSigned(
        lotId = 1L,
        signature = AuditFixtures.zeroKey32 // will be recomputed below
      )
      val validSig = signed.computeSignature(lot.kZero)
      val signedValid = signed.copy(signature = validSig)
      when(repo.searchLogs(any[OperationLogSearch])).thenReturn(Future.successful(IndexedSeq(signedValid)))

      val service = new PeoOperationLogService(repo, signer, lotSize, chunkSize)
      Await.result(service.checkLot(1L), timeout) mustBe Some(Right(()))

      val captor = ArgumentCaptor.forClass(classOf[OperationLogSearch])
      verify(repo).searchLogs(captor.capture())
      val used = captor.getValue
      used.ascending mustBe Some(true)
      used.sortField mustBe Some("id")
      used.pageSize  mustBe chunkSize
      used.lotId     mustBe 1L
    }

    "return Left with the tampered entry when the signature chain is broken" in {
      val repo   = mock[OperationLogRepository]
      val signer = mock[PEOSignerService]
      val lot    = OperationLogLot(1L, new Date(), AuditFixtures.zeroKey32)
      val tampered = AuditFixtures.makeSigned(
        lotId = 1L,
        signature = Key(Seq.fill(32)(0xff.toByte))
      )

      when(repo.getLot(1L)).thenReturn(Future.successful(Some(lot)))
      when(repo.countLogs(any[OperationLogSearch])).thenReturn(Future.successful(1))
      when(repo.searchLogs(any[OperationLogSearch]))
        .thenReturn(Future.successful(IndexedSeq(tampered)))

      val service = new PeoOperationLogService(repo, signer, lotSize, chunkSize)
      val result  = Await.result(service.checkLot(1L), timeout)

      result.isDefined mustBe true
      result.get.isLeft mustBe true
    }
  }
}
