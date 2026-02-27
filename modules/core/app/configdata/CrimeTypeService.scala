package configdata

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import configdata.CrimeType
import services.CacheService

abstract class CrimeTypeService {
  def list(): Future[Map[String, CrimeType]]
}

@Singleton
class CachedCrimeTypeService @Inject() (
  cache: CacheService,
  crimeTypeRepository: CrimeTypeRepository
)(implicit ec: ExecutionContext) extends CrimeTypeService {

  private val mapById: Seq[CrimeType] => Map[String, CrimeType] = { list =>
    list.map(ct => ct.id -> ct).toMap
  }

  override def list(): Future[Map[String, CrimeType]] = {
    crimeTypeRepository.list().map(mapById)
  }
}
