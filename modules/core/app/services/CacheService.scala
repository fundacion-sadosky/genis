package services

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.cache.SyncCacheApi

import types.TotpToken

// Forward declaration para evitar dependencia circular
// TODO: Mover a types/ cuando se migre User.scala completo
trait FullUserLike

abstract class CacheService {
  def get[T](key: CacheKey[T])(using ct: ClassTag[T]): Option[T]
  def getOrElse[T](key: CacheKey[T])(orElse: => T)(using ct: ClassTag[T]): T
  def asyncGetOrElse[T](key: CacheKey[T])(orElse: => Future[T])(using ct: ClassTag[T], ec: ExecutionContext): Future[T]
  def pop[T](key: CacheKey[T])(using ct: ClassTag[T]): Option[T]
  def set[T](key: CacheKey[T], value: T): Unit
}

@Singleton
class PlayCacheService @Inject() (cache: SyncCacheApi) extends CacheService {

  private val logger: Logger = Logger(this.getClass)

  override def get[T](key: CacheKey[T])(using ct: ClassTag[T]): Option[T] = {
    val opt = cache.get[T](key.cacheKey)
    if (key.clearTimeout) {
      opt.foreach(v => set(key, v))
    }
    opt
  }

  override def getOrElse[T](key: CacheKey[T])(orElse: => T)(using ct: ClassTag[T]): T = {
    get(key).getOrElse {
      val elseValue = orElse
      logger.info(s"Cache entry not found: ${key.cacheKey}. Setting else value.")
      set(key, elseValue)
      elseValue
    }
  }

  override def asyncGetOrElse[T](key: CacheKey[T])(orElse: => Future[T])(using ct: ClassTag[T], ec: ExecutionContext): Future[T] = {
    get(key).fold {
      val elseValue = orElse
      logger.info(s"Cache entry not found: ${key.cacheKey}. Getting else value.")
      elseValue.foreach(value => set(key, value))
      elseValue
    }(d => Future.successful(d))
  }

  override def pop[T](key: CacheKey[T])(using ct: ClassTag[T]): Option[T] = {
    val opt = get(key)
    opt.foreach { _ =>
      cache.remove(key.cacheKey)
      logger.info(s"Remove cache entry: ${key.cacheKey}")
    }
    opt
  }

  override def set[T](key: CacheKey[T], value: T): Unit = {
    logger.trace(s"Add entry into cache: ${key.cacheKey} -> $value")
    if (key.expiration > 0) {
      cache.set(key.cacheKey, value, scala.concurrent.duration.Duration(key.expiration, "seconds"))
    } else {
      cache.set(key.cacheKey, value)
    }
  }
}

// Base trait para cache keys
sealed trait CacheKey[T] {
  def cacheKey: String
  def expiration: Int = 0
  def clearTimeout: Boolean = false
}

// Keys específicas para autenticación
// Nota: FullUser se define en security.AuthService por ahora
case class FullUserKey(userId: String, exp: Int) extends CacheKey[security.FullUser] {
  override def cacheKey: String = s"FullUser.$userId"
  override def expiration: Int = exp
  override def clearTimeout: Boolean = true
}

case class LoggedUserKey(userId: String) extends CacheKey[TotpToken] {
  override def cacheKey: String = s"LoggedUser.$userId"
  override def expiration: Int = 90
}

// TODO: Agregar más keys cuando se migren otros módulos
// - SignupRequestKey
// - ClearPassRequestKey
// - Keys.roles
// - Keys.rolePermissionMap
// etc.
