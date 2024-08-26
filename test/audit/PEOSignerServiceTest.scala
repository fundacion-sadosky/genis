package audit

import scala.collection.mutable.Buffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.blocking
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import scala.util.Random

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar

import play.api.libs.concurrent.Akka
import specs.PdgSpec

class PEOSignerServiceTest extends PdgSpec with MockitoSugar {

  implicit val duration = Duration(100, SECONDS)

  "PEOSigner" must {
    "must ensure entries are signed and persisted in the same order as they are received no matter how long takes persist task" in {

      val qty = 100
      val logRepo = mock[OperationLogRepository]
      when(logRepo.createLot(any[Key])).thenReturn(Future.successful(0L))
      val peoSigner = new AkkaPEOSignerService(Akka.system, logRepo, "SHA1PRNG", 10000)

      case class SerialiseInt(i: Int) extends Serializable {
        override def stringify: String = i.toString()

      }

      case class SignedInt(i: SerialiseInt, key: Key) extends Signature {
        override def stringify: String = i.stringify
        override val signature = computeSignature(key)
      }

      val build = (i: SerialiseInt, key: Key, lotId: Long) => SignedInt(i, key)

      val buff = Buffer[SignedInt]()
      val persist = {

        val rnd = Random

        (s: SignedInt) => Future {
          blocking {
            val t = (5 + rnd.nextInt(20))
            Thread.sleep(t)
            buff += s
            s.toString()
          };
        }
      }

      def feedSigner(signer: PEOSignerService, qty: Int): Future[Seq[Any]] = {
        val x = for { i <- 0 until qty } yield {
          val entry = PEOEntry(SerialiseInt(i), build, persist)
          peoSigner.addEntry(entry)
        }
        Future.sequence(x)
      }

      val feedsPromise = feedSigner(peoSigner, qty)

      val result = Await.result(feedsPromise, duration)

      Stream.from(0).take(qty).zip(buff).foreach {
        case (i, s) =>
          s.i.i mustBe i
      }

    }
  }

}