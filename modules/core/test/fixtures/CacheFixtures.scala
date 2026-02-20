package fixtures

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

import services.CacheKey
import services.CacheService

class StubCacheService extends CacheService:
  private val store = scala.collection.mutable.HashMap.empty[String, Any]

  override def get[T](key: CacheKey[T])(using ct: ClassTag[T]): Option[T] =
    store.get(key.cacheKey).map(_.asInstanceOf[T])

  override def getOrElse[T](key: CacheKey[T])(orElse: => T)(using ct: ClassTag[T]): T =
    get(key).getOrElse {
      val v = orElse
      set(key, v)
      v
    }

  override def asyncGetOrElse[T](key: CacheKey[T])(orElse: => Future[T])(using ct: ClassTag[T], ec: ExecutionContext): Future[T] =
    get(key).fold {
      val f = orElse
      f.map { v => set(key, v); v }(ec)
    }(v => Future.successful(v))

  override def pop[T](key: CacheKey[T])(using ct: ClassTag[T]): Option[T] =
    val v = get(key)
    store.remove(key.cacheKey)
    v

  override def set[T](key: CacheKey[T], value: T): Unit =
    store.put(key.cacheKey, value)

  def clear(): Unit = store.clear()
