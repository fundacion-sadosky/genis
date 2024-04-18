package matching

import java.util.Date

import configdata.{CachedCategoryService, CategoryService, FullCategory, SlickCategoryRepository}
import matching.Stringency._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.libs.concurrent.Akka
import profile._
import profiledata.{ProfileDataRepository, SlickProfileDataRepository}
import scenarios.{MongoScenarioRepository, ScenarioRepository}
import services.PlayCacheService
import specs.PdgSpec
import stubs.Stubs
import types.{AlphanumericId, MongoDate, MongoId, SampleCode}

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
import scalaz.effect.IO
import play.api.test.FakeApplication
import inbox.NotificationService
import org.scalatest.Ignore

/*@Ignore
class Spark2MatcherTest extends PdgSpec with MockitoSugar {

  implicit override lazy val app: FakeApplication = FakeApplication()
  
  val duration = Duration(100, SECONDS)

  "Spark2Matcher" must {

    "run a match over profile collection" in {

      val mockNotiService = mock[NotificationService]

      val matchingService = new Spark2Matcher(
        Akka.system,
        new MongoProfileRepository,
        mockNotiService,
        new CachedCategoryService(new PlayCacheService, new SlickCategoryRepository),
        new MatchingProcessStatusImpl,
        "mongodb://localhost:27017/pdgdb")

      for { i <- 1 to 1 } {
        matchingService.findMatchesInBackGround(SampleCode("AR-B-IMBICE-639"))
      }

      Thread.sleep(30000)
    }
  }
}*/
