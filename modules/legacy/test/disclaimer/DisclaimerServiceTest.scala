package disclaimer

import connections.ConnectionRepository
import org.scalatest.mock.MockitoSugar
import specs.PdgSpec
import org.mockito.Mockito.when

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, SECONDS}
import scala.language.postfixOps

class DisclaimerServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  "DisclaimerServiceTest" must {

    "get disclaimer ok" in {
      val repo = mock[SlickDisclaimerRepository]
      val service = new DisclaimerServiceImpl(repo)
      when(repo.get()).thenReturn(Future.successful(Disclaimer(Some("hola"))))

      val disclaimer = Await.result(service.get(), duration)
      disclaimer.text mustBe Some("hola")
    }

  }

}
