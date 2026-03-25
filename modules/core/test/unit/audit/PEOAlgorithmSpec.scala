package unit.audit

import java.security.SecureRandom

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.*

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import audit.{Key, PEOAlgorithm, Signature, Stringifiable}

class PEOAlgorithmSpec extends AnyWordSpec with Matchers {

  given ec: ExecutionContext = ExecutionContext.global
  private val timeout = 5.seconds
  private val rng     = new SecureRandom()

  /** Minimal Signature implementation for testing. */
  case class TestEntry(index: Int, override val signature: Key) extends Signature {
    override def stringify: String = s"entry-$index"
  }

  /** Build a valid chain starting from zeroKey. */
  private def buildChain(size: Int, zeroKey: Key): LazyList[TestEntry] = {
    LazyList.unfold((0, zeroKey)) { case (i, prevKey) =>
      if (i >= size) None
      else {
        val entry = TestEntry(i, signature = Key(""))
        val sig   = entry.computeSignature(prevKey)
        val signed = entry.copy(signature = sig)
        Some(signed -> (i + 1, sig))
      }
    }
  }

  "PEOAlgorithm.verifyEntries (sync)" must {

    "return Right(()) for a valid chain of 1 entry" in {
      val zeroKey = Key(rng)
      val chain   = buildChain(1, zeroKey)
      PEOAlgorithm.verifyEntries(chain, zeroKey) mustBe Right(())
    }

    "return Right(()) for a valid chain of 100 entries" in {
      val zeroKey = Key(rng)
      val chain   = buildChain(100, zeroKey)
      PEOAlgorithm.verifyEntries(chain, zeroKey) mustBe Right(())
    }

    "return Left with the corrupted entry when the chain is tampered" in {
      val zeroKey  = Key(rng)
      val chain    = buildChain(50, zeroKey)
      // Corrupt entry at index 17 by replacing its signature
      val corrupted = chain.updated(17, chain(17).copy(signature = Key(rng)))
      val result    = PEOAlgorithm.verifyEntries(corrupted, zeroKey)
      result.isLeft mustBe true
      result.swap.getOrElse(null)._1.index mustBe 17
    }

    "return Right(()) for an empty chain" in {
      PEOAlgorithm.verifyEntries(LazyList.empty[TestEntry], Key(rng)) mustBe Right(())
    }
  }

  "PEOAlgorithm.verifyEntries (async / paginated)" must {

    "return Right(()) for a valid 200-entry chain with page size 50" in {
      val zeroKey  = Key(rng)
      val chain    = buildChain(200, zeroKey).toIndexedSeq
      val pageSize = 50

      val fetchPage = (page: Int) =>
        scala.concurrent.Future.successful(
          chain.slice(page * pageSize, (page + 1) * pageSize)
        )

      val result = Await.result(
        PEOAlgorithm.verifyEntries(fetchPage, chain.size, pageSize, zeroKey),
        timeout
      )
      result mustBe Right(())
    }

    "return Left when an entry in the second page is corrupted" in {
      val zeroKey  = Key(rng)
      val chain    = buildChain(200, zeroKey).toIndexedSeq
      val pageSize = 50

      // Corrupt entry 75 (second page, index 25)
      val corrupted = chain.updated(75, chain(75).copy(signature = Key(rng)))

      val fetchPage = (page: Int) =>
        scala.concurrent.Future.successful(
          corrupted.slice(page * pageSize, (page + 1) * pageSize)
        )

      val result = Await.result(
        PEOAlgorithm.verifyEntries(fetchPage, chain.size, pageSize, zeroKey),
        timeout
      )
      result.isLeft mustBe true
    }

    "return Right(()) for an empty set (totalCount = 0)" in {
      val fetchPage = (_: Int) =>
        scala.concurrent.Future.successful(IndexedSeq.empty[TestEntry])

      val result = Await.result(
        PEOAlgorithm.verifyEntries(fetchPage, 0, 100, Key(rng)),
        timeout
      )
      result mustBe Right(())
    }
  }
}
