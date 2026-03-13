// test/unit/disclaimer/DisclaimerServiceTest.scala
package unit.disclaimer

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import disclaimer.{Disclaimer, DisclaimerRepository, DisclaimerServiceImpl}

class StubDisclaimerRepository(response: Future[Disclaimer]) extends DisclaimerRepository:
  override def get(): Future[Disclaimer] = response

class DisclaimerServiceTest extends AnyWordSpec with Matchers:

  private def await[T](f: Future[T]): T = Await.result(f, 3.seconds)

  "DisclaimerServiceImpl" must {

    "return Disclaimer with text when repository returns Some" in {
      val stub = StubDisclaimerRepository(Future.successful(Disclaimer(Some("terms"))))
      val service = DisclaimerServiceImpl(stub)
      await(service.get()) mustBe Disclaimer(Some("terms"))
    }

    "return Disclaimer with None when repository returns None" in {
      val stub = StubDisclaimerRepository(Future.successful(Disclaimer(None)))
      val service = DisclaimerServiceImpl(stub)
      await(service.get()) mustBe Disclaimer(None)
    }

    "propagate repository failure as failed Future" in {
      val error = RuntimeException("DB unavailable")
      val stub = StubDisclaimerRepository(Future.failed(error))
      val service = DisclaimerServiceImpl(stub)
      an[RuntimeException] must be thrownBy await(service.get())
    }

    "delegate exactly to the repository — no transformation" in {
      var callCount = 0
      val stub = new DisclaimerRepository:
        override def get(): Future[Disclaimer] =
          callCount += 1
          Future.successful(Disclaimer(Some("text")))
      val service = DisclaimerServiceImpl(stub)
      await(service.get())
      callCount mustBe 1
    }
  }