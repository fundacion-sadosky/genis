package audit

import scala.collection.immutable.IndexedSeq
import scala.concurrent.{ExecutionContext, Future}

object PEOAlgorithm {

  private val isValid: PartialFunction[(Signature, Key), Boolean] = {
    case (entry, computedSignature) => entry.signature == computedSignature
  }

  private val computeSignatureOf: (Key, Signature) => Key = (key, entry) => entry.computeSignature(key)

  /** Synchronous verification over a lazy Stream. */
  def verifyEntries[T <: Signature](entries: LazyList[T], zeroKey: Key): Either[(T, Key), Unit] = {
    val signatures = entries.scanLeft(zeroKey)(computeSignatureOf).tail
    val invalid    = entries.zip(signatures).dropWhile(isValid)
    invalid.headOption.fold[Either[(T, Key), Unit]](Right(()))(Left(_))
  }

  /**
   * Async verification over paginated DB chunks.
   *
   * @param fetchPage  function returning records for a given 0-based page number
   * @param totalCount total number of records to verify (used to compute page count)
   * @param pageSize   records per page (must match the pageSize used in fetchPage)
   * @param zeroKey    the kZero seed for the lot
   */
  def verifyEntries[T >: Null <: Signature](
    fetchPage: Int => Future[IndexedSeq[T]],
    totalCount: Int,
    pageSize: Int,
    zeroKey: Key
  )(implicit ec: ExecutionContext): Future[Either[(T, Key), Unit]] = {

    if (totalCount == 0) return Future.successful(Right(()))

    val totalPages = (totalCount + pageSize - 1) / pageSize

    def loop(page: Int, prevKey: Key): Future[Either[(T, Key), Unit]] =
      if (page >= totalPages) Future.successful(Right(()))
      else fetchPage(page).flatMap { chunk =>
        if (chunk.isEmpty) Future.successful(Right(()))
        else verifyChunk(chunk, prevKey) match {
          case Left(err)      => Future.successful(Left(err))
          case Right(lastKey) => loop(page + 1, lastKey)
        }
      }

    loop(0, zeroKey)
  }

  /** Verifies a chunk in order, returning the last key on success or the failing entry + expected key. */
  private def verifyChunk[T <: Signature](chunk: IndexedSeq[T], startKey: Key): Either[(T, Key), Key] =
    chunk.foldLeft[Either[(T, Key), Key]](Right(startKey)) {
      case (Left(err), _) => Left(err)
      case (Right(prevKey), entry) =>
        val computed = entry.computeSignature(prevKey)
        if (entry.signature == computed) Right(computed)
        else Left((entry, computed))
    }
}
