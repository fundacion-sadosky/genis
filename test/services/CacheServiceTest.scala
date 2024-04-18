package services

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import javax.inject.Singleton
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import configdata.CategoryRepository
import specs.PdgSpec
import stubs.Stubs
import user.FullUser
import security.RequestToken
import security.AuthenticatedPair
import play.api.libs.Files.TemporaryFile

class CacheServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  implicit val sessionExpirationTime = 10;

  "CacheService" must {

    "retrive" in {

      import scala.concurrent.Await
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.Future
      import scala.concurrent.duration.Duration
      import scala.concurrent.duration.SECONDS
      implicit val sessionExpirationTime = 10;
      val duration = Duration(10, SECONDS)

      var functionCallsCount: Int = 0

      def futureCacheTest(s: String): FullUser = {
        functionCallsCount += 1
        Stubs.fullUser
      }

      val target = new PlayCacheService()
      val futureKey = new FullUserKey("futureKey", 0)

      val r1 = target.getOrElse(futureKey)(futureCacheTest("future cached value 1"))
      r1 mustBe Stubs.fullUser

      val r2 = target.getOrElse(futureKey)(futureCacheTest("future cached value 2"))
      r2 mustBe Stubs.fullUser

      val r3 = target.getOrElse(futureKey)(futureCacheTest("future cached value 3"))
      r3 mustBe Stubs.fullUser

      functionCallsCount mustBe 1
    }
    
    "retrieve stored values" in {
      val target = new PlayCacheService()
      val key = new FullUserKey("tokenKey", 0)
      val value = Stubs.fullUser
      target.set(key, value)
      target.get(key) map { cachedValue =>
        cachedValue mustBe value
      } getOrElse {
        fail(s"""missing entry "$key" in cache""")
      }
    }

    "retrive an else value when the required key doesn't exist and set it as the current value" in {
      val target = new PlayCacheService()
      val notFoundKey = new FullUserKey("notFoundKey", 0)

      var functionCallsCount: Int = 0
      def getOrElseCacheTest(s: String): FullUser = {
        functionCallsCount += 1
        Stubs.fullUser
      }

      target.getOrElse(notFoundKey)(getOrElseCacheTest("else value")) mustBe Stubs.fullUser

      target.getOrElse(notFoundKey)(getOrElseCacheTest("non expected else value")) mustBe Stubs.fullUser

      functionCallsCount mustBe 1
    }

    "keep the cached value during the specified expiration time" in {
      val target = new PlayCacheService()

      val key2Second = new FullUserKey("key2Seconds", 1)
      val value2Seconds = Stubs.fullUser
      target.set(key2Second, value2Seconds)

      Thread.sleep(200)
      target.get(key2Second) mustBe Some(value2Seconds)

      Thread.sleep(1500)
      target.get(key2Second) mustBe None
    }

    "clear the expiration time when specified" in {
      val target = new PlayCacheService()

      val key = new FullUserKey("clearTimeoutTestKey", 1)
      val value = Stubs.fullUser
      target.set(key, value)

      Thread.sleep(200)
      target.get(key) mustBe Some(value)

      Thread.sleep(200)
      target.get(key) mustBe Some(value)

      Thread.sleep(2000)
      target.get(key) mustBe None
    }

    "remove an item from the cache when a key is popped" in {
      val target = new PlayCacheService()

      val key = new FullUserKey("popTestKey", 0)
      val value = Stubs.fullUser
      target.set(key, value)

      target.pop(key) mustBe Some(value)
      target.get(key) mustBe None
    }

  }

  "TemporaryAssetsKey instances" must {
    "have same toString representation if they were constructude with same parameter" in {
      val token = "TemporaryAssetsKey token"
      val key1 = new TemporaryAssetKey(token)
      val key2 = new TemporaryAssetKey(token)
      key1.toString() mustBe key2.toString()
    }
  }

  "FullUserKey instances" must {
    "have same toString representation if they were constructude with same parameters" in {
      val userId = "testUserId"
      val key1 = new FullUserKey(userId, 0)
      val key2 = new FullUserKey(userId, 0)
      key1.toString() mustBe key2.toString()
    }
  }

  "UserCredentialsKey instances" must {
    "have same toString representation if they were constructude with same parameters" in {
      val userId = "testUserId"
      val key1 = new FullUserKey(userId, 0)
      val key2 = new FullUserKey(userId, 0)
      key1.toString() mustBe key2.toString()
    }
  }

}
