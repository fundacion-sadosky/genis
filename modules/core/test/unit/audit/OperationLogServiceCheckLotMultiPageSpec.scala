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
import fixtures.AuditFixtures

/**
 * Gap G1 (#214 review): verifies that `checkLot` correctly verifies the PEO signature
 * chain across MULTIPLE pages — the forensic correctness fix vs legacy (A-N1), which
 * verified in id-DESC order and broke pagination. Existing OperationLogServiceSpec only
 * covered the single-page case.
 */
class OperationLogServiceCheckLotMultiPageSpec
    extends AnyWordSpec with Matchers with MockitoSugar {

  given ec: ExecutionContext = ExecutionContext.global
  private val timeout = 5.seconds

  private val lotSize   = 10000
  private val chunkSize = 2 // small so 5 entries span 3 pages

  /** Sign entries sequentially starting from zeroKey (insertion order = id asc). */
  private def signChain(entries: Seq[SignedOperationLogEntry], zeroKey: Key): IndexedSeq[SignedOperationLogEntry] = {
    var key = zeroKey
    entries.map { e =>
      val sig = e.computeSignature(key)
      key = sig
      e.copy(signature = sig)
    }.toIndexedSeq
  }

  private def pageOf(all: IndexedSeq[SignedOperationLogEntry], search: OperationLogSearch): IndexedSeq[SignedOperationLogEntry] = {
    val from = search.page * chunkSize
    all.slice(from, from + chunkSize)
  }

  "PeoOperationLogService.checkLot (multi-page)" must {

    "return Some(Right(())) for a valid chain spanning several pages" in {
      val repo   = mock[OperationLogRepository]
      val signer = mock[PEOSignerService]
      val lot    = OperationLogLot(1L, new Date(), AuditFixtures.zeroKey32)

      val chain = signChain((0 until 5).map(i => AuditFixtures.makeSigned(index = i.toLong, lotId = 1L)), lot.kZero)

      when(repo.getLot(1L)).thenReturn(Future.successful(Some(lot)))
      when(repo.countLogs(any[OperationLogSearch])).thenReturn(Future.successful(chain.size))
      when(repo.searchLogs(any[OperationLogSearch])).thenAnswer { inv =>
        Future.successful(pageOf(chain, inv.getArgument[OperationLogSearch](0)))
      }

      val service = new PeoOperationLogService(repo, signer, lotSize, chunkSize)
      Await.result(service.checkLot(1L), timeout) mustBe Some(Right(()))
    }

    "return Some(Left(..)) when an entry on a later page is tampered" in {
      val repo   = mock[OperationLogRepository]
      val signer = mock[PEOSignerService]
      val lot    = OperationLogLot(1L, new Date(), AuditFixtures.zeroKey32)

      val chain    = signChain((0 until 5).map(i => AuditFixtures.makeSigned(index = i.toLong, lotId = 1L)), lot.kZero)
      // Tamper an entry on page 2 (index 4) — its stored signature no longer matches the chain.
      val tampered = chain.updated(4, chain(4).copy(userId = "attacker"))

      when(repo.getLot(1L)).thenReturn(Future.successful(Some(lot)))
      when(repo.countLogs(any[OperationLogSearch])).thenReturn(Future.successful(tampered.size))
      when(repo.searchLogs(any[OperationLogSearch])).thenAnswer { inv =>
        Future.successful(pageOf(tampered, inv.getArgument[OperationLogSearch](0)))
      }

      val service = new PeoOperationLogService(repo, signer, lotSize, chunkSize)
      val result  = Await.result(service.checkLot(1L), timeout)

      result.isDefined mustBe true
      result.get.isLeft mustBe true
    }
  }
}
