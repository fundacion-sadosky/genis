package unit.disclaimer

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.{verify, when}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

import disclaimer.{Disclaimer, DisclaimerRepository, DisclaimerServiceImpl}

class DisclaimerServiceImplTest extends AnyWordSpec with Matchers with MockitoSugar:

  private val timeout = 2.seconds

  "DisclaimerServiceImpl" must {
    "delegate get() to repository" in {
      val repo = mock[DisclaimerRepository]
      val disclaimer = Disclaimer(Some("Legal text"))
      when(repo.get()).thenReturn(Future.successful(disclaimer))
      val service = new DisclaimerServiceImpl(repo)

      Await.result(service.get(), timeout) mustBe disclaimer
      verify(repo).get()
    }
  }
