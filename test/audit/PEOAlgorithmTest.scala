package audit

import scala.Left
import scala.Right
import scala.collection.immutable.Stream.consWrapper
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import org.scalatest.AppendedClues
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import scala.collection.immutable.IndexedSeq

class PEOAlgorithmTest extends FlatSpec with MustMatchers with AppendedClues {

  case class Signed(index: Int, prev: Signature) extends Signature {
    override def stringify = index.toString()
    override val signature = computeSignature(prev.signature)
  }

  val zeroEntry = new Signature {
    override def stringify = "zeroentry"
    override val signature = new Key(List.fill(32)(0.toByte))
  }

  val zeroKey = zeroEntry.signature

  private def generateEntryStream(prev: Signature, qty: Int, from: Int = 0): Stream[Signature] = {
    Stream.range(from, from + qty)
      .scanLeft((prev)) {
        case (prevEntry, index) => Signed(index, prevEntry)
      }
      .tail
      .take(qty)
  }

  private def generateCorruptedEntryStream(prev: Signature, qty: Int, failAt: Int): Stream[Signature] = {
    val good = generateEntryStream(zeroEntry, failAt)
    val bad = Signed(failAt, zeroEntry)
    val remainder = generateEntryStream(bad, Integer.MAX_VALUE - failAt, failAt + 1)
    good #::: (bad #:: remainder)
  }

  private def generateEntryStreamEnumerator(prev: Signature, chuncks: Int, chunckSize: Int): Enumerator[IndexedSeq[Signature]] = {

    val index = chuncks * chunckSize

    val entryEnumerator: Enumerator[IndexedSeq[Signature]] = Enumerator.unfold {
      (zeroEntry -> 0)
    } {
      case (prev, offset) => {
        if (offset < index) {
          val list = generateEntryStream(prev, chunckSize, offset).toIndexedSeq
          Some((list.last, offset + chunckSize) -> list)
        } else {
          None
        }
      }
    }

    entryEnumerator

  }

  private def generateCorruptedEntryStreamEnumerator(prev: Signature, qty: Int, failAt: Int, chunckSize: Int): Enumerator[IndexedSeq[Signature]] = {

    val entryEnumerator: Enumerator[IndexedSeq[Signature]] = Enumerator.unfold {
      (zeroEntry -> 0)
    } {
      case (prev, offset) => {
        if (offset < qty) {
          val list = {
            val doFail = offset <= failAt && failAt < (offset + chunckSize)
            val stream = if (doFail) {
              val failPos = failAt - offset
              val good = generateEntryStream(prev, failPos, offset)
              val bad = Signed(failAt, zeroEntry)
              val remainder = generateEntryStream(bad, chunckSize - failPos, failAt + 1)
              good #::: (bad #:: remainder)
            } else {
              generateEntryStream(prev, chunckSize, offset)
            }
            stream.toIndexedSeq
          }
          Some((list.last, offset + chunckSize) -> list)
        } else {
          None
        }
      }
    }

    entryEnumerator

  }

  "PEOAlgorithm" must
    "verify an entry with perevious entries stream and a zero key" in {

      val entries = generateEntryStream(zeroEntry, 10)
      val entry = entries.last
      val previous = entries.take(entries.size - 1)
      val result = PEOAlgorithm.verifyEntry(entry, previous, zeroKey)

      result mustBe true

    }

  "PEOAlgorithm" must
    "verify a 100 elements entries stream" in {

      val result = PEOAlgorithm.verifyEntries(generateEntryStream(zeroEntry, 100), zeroKey)

      result mustBe (Right(()))

    }

  "PEOAlgorithm" must
    "fail at 17th entry of very large stream and stop at that point" in {

      val n = 17

      val entries = generateCorruptedEntryStream(zeroEntry, Integer.MAX_VALUE, n)

      val good = entries(n - 1)
      val bad = entries(n)

      val result = PEOAlgorithm.verifyEntries(entries, zeroKey)

      result mustBe (Left(
        bad -> bad.computeSignature(good.signature)))

    }

  "PEOAlgorithm" must
    "check enumerator generation" in {
      val stream = generateEntryStream(zeroEntry, 8)
      val enumerator = generateEntryStreamEnumerator(zeroEntry, 4, 2)

      val promise = Iteratee.flatten(enumerator |>> Iteratee.consume()).run
      val result = Await.result(promise, Duration(10, SECONDS))

      stream mustBe result

    }

  "PEOAlgorithm" must
    "verify a 10000 entries Enumerator" in {

      val entries = generateEntryStreamEnumerator(zeroEntry, 50, 200)

      val promise = PEOAlgorithm.verifyEntries(entries, zeroKey)

      val result = Await.result(promise, Duration(10, SECONDS))

      result mustBe (Right(()))

    }

  "PEOAlgorithm" must
    "check corrupted enumerator generation" in {
      val stream = generateEntryStream(zeroEntry, 50)
      val enumerator = generateCorruptedEntryStreamEnumerator(zeroEntry, 50, 26, 5)

      val promise = Iteratee.flatten(enumerator |>> Iteratee.consume()).run
      val result = Await.result(promise, Duration(10, SECONDS))

      stream.take(26) mustBe result.take(26)

    }

  "PEOAlgorithm" must
    "fail at 319th entry of very large Enumerator and stop at that point" in {

      val failAt = 319

      val entries = generateCorruptedEntryStreamEnumerator(zeroEntry, Integer.MAX_VALUE, failAt, 80)
      val (bad, good) = {
        val entries = generateCorruptedEntryStream(zeroEntry, failAt + 1, failAt)
        (entries(failAt), entries(failAt - 1))
      }

      val promise = PEOAlgorithm.verifyEntries(entries, zeroKey)

      val result = Await.result(promise, Duration(10, SECONDS))

      result mustBe (Left(
        bad -> bad.computeSignature(good.signature)))

    }

}