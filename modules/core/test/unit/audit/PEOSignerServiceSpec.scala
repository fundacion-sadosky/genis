package unit.audit

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*
import scala.util.Random

import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import audit.*

class PEOSignerServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterAll:

  given ec: ExecutionContext = ExecutionContext.global
  private val timeout = 30.seconds

  // ActorSystem loads application.conf, which defines audit-dispatcher and bounded-mailbox.
  private val system: ActorSystem = ActorSystem("peo-signer-test")

  override protected def afterAll(): Unit =
    Await.result(system.terminate(), 10.seconds)
    super.afterAll()

  // Minimal Stringifiable entry for these tests.
  private case class TestEntry(i: Int) extends Stringifiable:
    override def stringify: String = s"entry-$i"

  private case class SignedTestEntry(i: Int, key: Key) extends Signature:
    override def stringify: String = s"entry-$i"
    override val signature: Key = computeSignature(key)

  // (entry, prevKey, lotId) -> SignedTestEntry ; records (entry.i, prevKey, lotId) per call.
  private def recordingBuild(calls: ArrayBuffer[(Int, Key, Long)])
      : (TestEntry, Key, Long) => SignedTestEntry =
    (e, k, lotId) =>
      calls.synchronized { calls += ((e.i, k, lotId)) }
      SignedTestEntry(e.i, k)

  "AkkaPEOSignerService" must {

    "preserve insertion order under random persist delays" in {
      val repo = mock[OperationLogRepository]
      when(repo.createLot(any[Key])).thenReturn(Future.successful(1L))

      val signer = new AkkaPEOSignerService(system, repo, "SHA1PRNG", lotSize = 10000)

      val qty   = 100
      val order = ArrayBuffer.empty[Int]
      val rnd   = new Random()

      val build: (TestEntry, Key, Long) => SignedTestEntry = (e, k, _) => SignedTestEntry(e.i, k)
      val persist: SignedTestEntry => Future[Int] = s => Future {
        scala.concurrent.blocking {
          Thread.sleep(5L + rnd.nextInt(20))
        }
        order.synchronized { order += s.i }
        s.i
      }

      val done = Future.sequence {
        (0 until qty).map(i => signer.addEntry(PEOEntry(TestEntry(i), build, persist)))
      }
      Await.result(done, timeout)

      order.toList mustBe (0 until qty).toList
    }

    "return the persist result to the caller" in {
      val repo = mock[OperationLogRepository]
      when(repo.createLot(any[Key])).thenReturn(Future.successful(42L))

      val signer = new AkkaPEOSignerService(system, repo, "SHA1PRNG", lotSize = 10000)

      val build: (TestEntry, Key, Long) => SignedTestEntry = (e, k, _) => SignedTestEntry(e.i, k)
      val persist: SignedTestEntry => Future[String] = s => Future.successful(s"persisted-${s.i}")

      val result = Await.result(signer.addEntry(PEOEntry(TestEntry(7), build, persist)), timeout)
      result mustBe "persisted-7"
    }

    "invoke the build callback with the lotId returned by createLot" in {
      val repo = mock[OperationLogRepository]
      when(repo.createLot(any[Key])).thenReturn(Future.successful(99L))

      val signer = new AkkaPEOSignerService(system, repo, "SHA1PRNG", lotSize = 10000)

      val calls = ArrayBuffer.empty[(Int, Key, Long)]
      val persist: SignedTestEntry => Future[Unit] = _ => Future.successful(())

      val done = Future.sequence(
        (0 until 3).map(i => signer.addEntry(PEOEntry(TestEntry(i), recordingBuild(calls), persist)))
      )
      Await.result(done, timeout)

      calls.map(_._1).toList mustBe List(0, 1, 2)
      calls.map(_._3).toSet  mustBe Set(99L)
    }

    "chain signatures: each entry's prevKey equals the previous entry's signature" in {
      val repo = mock[OperationLogRepository]
      when(repo.createLot(any[Key])).thenReturn(Future.successful(1L))

      val signer = new AkkaPEOSignerService(system, repo, "SHA1PRNG", lotSize = 10000)

      val calls = ArrayBuffer.empty[(Int, Key, Long)]
      val persist: SignedTestEntry => Future[Unit] = _ => Future.successful(())

      val done = Future.sequence(
        (0 until 5).map(i => signer.addEntry(PEOEntry(TestEntry(i), recordingBuild(calls), persist)))
      )
      Await.result(done, timeout)

      // The prevKey of entry N+1 must equal the signature produced for entry N.
      val ordered = calls.sortBy(_._1).toList
      ordered.sliding(2).foreach { case List((prevI, _, _), (_, nextPrevKey, _)) =>
        val expected = SignedTestEntry(prevI, ordered.find(_._1 == prevI).get._2).signature
        nextPrevKey mustBe expected
      }
    }

    "create a new lot after lotSize entries are written" in {
      val repo = mock[OperationLogRepository]
      when(repo.createLot(any[Key])).thenReturn(Future.successful(1L), Future.successful(2L))

      val signer = new AkkaPEOSignerService(system, repo, "SHA1PRNG", lotSize = 3)

      val calls = ArrayBuffer.empty[(Int, Key, Long)]
      val persist: SignedTestEntry => Future[Unit] = _ => Future.successful(())

      val done = Future.sequence(
        (0 until 4).map(i => signer.addEntry(PEOEntry(TestEntry(i), recordingBuild(calls), persist)))
      )
      Await.result(done, timeout)

      // Initial createLot + one rollover after the 3rd entry (lotSize = 3).
      verify(repo, times(2)).createLot(any[Key])
      // First three entries belong to lotId=1, fourth entry belongs to lotId=2.
      val byIndex = calls.toList.sortBy(_._1)
      byIndex.map(_._3) mustBe List(1L, 1L, 1L, 2L)
    }
  }
