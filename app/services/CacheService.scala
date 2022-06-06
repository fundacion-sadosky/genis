package services

import scala.concurrent.Future
import scala.language.postfixOps
import scala.reflect.ClassTag
import configdata.BioMaterialType
import configdata.Category
import configdata.Country
import configdata.CrimeType
import configdata.FullCategory
import configdata.Province
import javax.inject.Inject
import javax.inject.Singleton

import kits.{AnalysisType, FullLocus, FullStrKit, StrKit}
import laboratories.Laboratory
import pedigree.CaseType
import play.api.Application
import play.api.Logger
import play.api.cache.Cache
import play.api.libs.Files.TemporaryFile
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import profile.NewAnalysis
import stats.PopulationBaseFrequency
import types.AlphanumericId
import user.{ClearPassSolicitud, FullUser, Role, SignupSolicitude}
import user.UserCredentials.SignupCredentials
import types.Permission
import types.TotpToken

abstract class CacheService {

  def get[T](key: CacheKey[T])(implicit ct: ClassTag[T]): Option[T]
  def getOrElse[T](key: CacheKey[T])(orElse: => T)(implicit ct: ClassTag[T]): T
  def asyncGetOrElse[T](key: CacheKey[T])(orElse: => Future[T])(implicit ct: ClassTag[T]): Future[T]
  def pop[T](key: CacheKey[T])(implicit ct: ClassTag[T]): Option[T]
  def set[T](key: CacheKey[T], value: T)
}

@Singleton
class PlayCacheService @Inject() (implicit app: Application) extends CacheService {

  val logger: Logger = Logger(this.getClass())

  override def get[T](key: CacheKey[T])(implicit ct: ClassTag[T]): Option[T] = {
    val opt = Cache.getAs[T](key.toString())(app, ct)
    if (key.clearTimeout) {
      opt.foreach(set(key, _))
    }
    opt
  }

  override def getOrElse[T](key: CacheKey[T])(orElse: => T)(implicit ct: ClassTag[T]): T = {
    get(key).getOrElse({
      val elseValue = orElse
      logger.info("Cache entry not found: " + key.toString() + ". Setting else value.")
      set(key, elseValue)
      elseValue
    })
  }

  override def asyncGetOrElse[T](key: CacheKey[T])(orElse: => Future[T])(implicit ct: ClassTag[T]): Future[T] = {
    get(key).fold({
      val elseValue = orElse
      logger.info("Cache entry not found: " + key.toString() + ". Getting else value.")
      elseValue.onSuccess {
        case value => {
          set(key, value)
        }
      }
      elseValue
    })(d => Future.successful(d))
  }

  override def pop[T](key: CacheKey[T])(implicit ct: ClassTag[T]): Option[T] = {
    val opt = get(key)
    opt.foreach(value => {
      Cache.remove(key.toString())
      logger.info("Remove cache entry: " + key.toString())
    })
    opt
  }

  override def set[T](key: CacheKey[T], value: T) = {
    logger.trace("Add entry into cache: " + key.toString() + " -> " + value)
    Cache.set(key.toString(), value, key.expiration)
  }

}

sealed abstract class CacheKey[T] protected (val expiration: Int = 0, val clearTimeout: Boolean = false) {

}

case class TemporaryFreqDbKey(token: String) extends CacheKey[PopulationBaseFrequency](60 * 10) {
  override def toString(): String = token
}

case class TemporaryAssetKey(token: String) extends CacheKey[List[TemporaryFile]](60 * 10) {
  override def toString(): String = "TemporaryAsset." + token
}

case class UploadedAnalysisKey(token: String) extends CacheKey[NewAnalysis](60 * 10) {
  override def toString(): String = "UploadedAnalysis." + token
}

case class FullUserKey(userId: String, val exp: Int) extends CacheKey[FullUser](exp, true) {
  override def toString(): String = "FullUser." + userId
}
case class ProfileLabKey(globalCode: String) extends CacheKey[String](90) {
  override def toString(): String = "ProfileLab." + globalCode
}
case class LoggedUserKey(userId: String) extends CacheKey[TotpToken](90) {
  override def toString(): String = "LoggedUser." + userId
}

case class SignupRequestKey(token: String) extends CacheKey[(SignupSolicitude, SignupCredentials, Seq[String])](60 * 15) {
  override def toString(): String = "SignupRequestKey." + token
}

case class ClearPassRequestKey(token: String) extends CacheKey[(ClearPassSolicitud, SignupCredentials, String)](60 * 15) {
  override def toString(): String = "ClearPassRequestKey." + token
}

object Keys {
  val biomaterialType = new CacheKey[Seq[BioMaterialType]] {
    override def toString(): String = "Keys.biomaterialType"
  }
  val crimeType = new CacheKey[Seq[CrimeType]] {
    override def toString(): String = "Keys.crimeType"
  }
  val categoryTree = new CacheKey[Category.CategoryTree] {
    override def toString(): String = "Keys.categoryTree"
  }
  val categoryTreeManualLoading = new CacheKey[Category.CategoryTree] {
    override def toString(): String = "Keys.categoryTreeManualLoading"
  }
  val categories = new CacheKey[Map[AlphanumericId, FullCategory]] {
    override def toString(): String = "Keys.categories"
  }
  val laboratories = new CacheKey[Seq[Laboratory]] {
    override def toString(): String = "Keys.laboratories"
  }
  val countries = new CacheKey[Seq[Country]] {
    override def toString(): String = "Keys.countries"
  }
  val provinces = new CacheKey[Seq[Province]] {
    override def toString(): String = "Keys.provinces"
  }
  val strKits = new CacheKey[Seq[FullStrKit]] {
    override def toString(): String = "Keys.strKits"
  }
  val locus = new CacheKey[Seq[FullLocus]] {
    override def toString(): String = "Keys.locus"
  }
  val analysisTypes = new CacheKey[Seq[AnalysisType]] {
    override def toString(): String = "Keys.analysisTypes"
  }

  val caseTypes = new CacheKey[Seq[CaseType]] {
    override def toString(): String = "Keys.caseTypes"
  }
  val roles = new CacheKey[Seq[Role]] {
    override def toString(): String = "Keys.roles"
  }
  val rolePermissionMap = new CacheKey[Map[String, Set[Permission]]] {
    override def toString(): String = "Keys.rolePermissionMap"
  }
}