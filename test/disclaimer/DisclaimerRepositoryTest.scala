package disclaimer

import org.scalatest.mock.MockitoSugar
import specs.PdgSpec

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import scala.language.postfixOps

class DisclaimerRepositoryTest extends PdgSpec with MockitoSugar {

  lazy val repo: DisclaimerRepository = new SlickDisclaimerRepository()
  val duration = Duration(10, SECONDS)

  "DisclaimerRepositoryTest" must {

    "get disclaimer no ok" in {

      val result = Await.result(repo.get(), duration)
      result mustBe Disclaimer(None)
    }

  }

}
