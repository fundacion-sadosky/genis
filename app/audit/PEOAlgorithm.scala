package audit

import scala.Left
import scala.Right
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.libs.iteratee.Enumeratee
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee

import scala.collection.immutable.IndexedSeq

object PEOAlgorithm {

  private val isValid: PartialFunction[(Signature, Key), Boolean] = {
    case (entry, computedSignature) => entry.signature == computedSignature
  }

  private val computeSignature = (key: Key, entry: Signature) => {
    entry.computeSignature(key)
  }

  private val tailElementSignature = (key: Key, entries: Seq[Signature]) => {
    entries.foldLeft(key)(computeSignature)
  }

  def verifyEntry[T <: Signature](entry: T, prevEntries: Stream[T], zeroKey: Key): Boolean = {
    val computedKey = prevEntries.foldLeft(zeroKey)(computeSignature)
    entry.signature == entry.computeSignature(computedKey)
  }

  def verifyEntry[T <: Signature](entry: T, prevEntries: Enumerator[Seq[T]], zeroKey: Key): Future[Boolean] = {
    val signatureCalculator = Iteratee.fold[Seq[T], Key](zeroKey)(tailElementSignature)
    val isValid = Iteratee.flatten(prevEntries |>> signatureCalculator)
      .run
      .map { computedKey =>
        entry.signature == entry.computeSignature(computedKey)
      }
    isValid
  }

  def verifyEntries[T <: Signature](entries: Stream[T], zeroKey: Key): Either[(T, Key), Unit] = {

    val signatures = (entries.scanLeft(zeroKey) {
      case (key, entry) => entry.computeSignature(key)
    }).tail

    val invalidEntries = entries.zip(signatures) dropWhile isValid

    invalidEntries.headOption
      .fold[Either[(T, Key), Unit]] {
        Right(())
      } {
        Left(_)
      }

  }

  def verifyEntries[T >: Null <: Signature](entries: Enumerator[IndexedSeq[T]], zeroKey: Key): Future[Either[(T, Key), Unit]] = {

    val zeroEntry: IndexedSeq[(T, Key)] = Vector((null, zeroKey))
    val signatureCalculator = Enumeratee.scanLeft[IndexedSeq[T]](zeroEntry) {
      case (prevEntry, entries) =>
        val prevKey = prevEntry.last._2
        val signatures = (entries.scanLeft(prevKey) {
          case (key, entry) => entry.computeSignature(key)
        }).tail
        entries.zip(signatures)
    }

    val signatureVerifier = Enumeratee.dropWhile { chunk: IndexedSeq[(T, Key)] =>
      val invalidEntries = chunk dropWhile isValid
      invalidEntries.isEmpty
    }

    val invalidEntries = entries &> signatureCalculator &> signatureVerifier

    val result = Iteratee.flatten(invalidEntries |>> Iteratee.head[IndexedSeq[(T, Key)]]).run

    result map { invalidChunkOpt =>
      invalidChunkOpt.fold[Either[(T, Key), Unit]] {
        Right(())
      } { invalidChunk =>
        (invalidChunk dropWhile isValid)
          .headOption
          .fold[Either[(T, Key), Unit]] {
            Right(())
          } {
            Left(_)
          }
      }

    }

  }
}