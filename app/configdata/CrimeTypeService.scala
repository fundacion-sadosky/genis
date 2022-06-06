package configdata

import scala.concurrent.Future
import scala.language.postfixOps
import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import types.AlphanumericId
import services.CacheService
import services.Keys

abstract class CrimeTypeService {

  def list(): Future[Map[String, CrimeType]]
}

@Singleton
class CachedCrimeTypeService @Inject() (cache: CacheService, crimeTypeRepository: CrimeTypeRepository) extends CrimeTypeService {

  private val mapById: Seq[CrimeType] => Map[String, CrimeType] = { list =>
    list map (ct => ct.id -> ct) toMap
  }

  override def list(): Future[Map[String, CrimeType]] = {
    val crimeTypeList = cache.asyncGetOrElse(Keys.crimeType)(crimeTypeRepository.list())
    crimeTypeList map { mapById(_) }
  }

}
