package configdata

import scala.concurrent.Future
import scala.language.postfixOps
import javax.inject.Inject
import javax.inject.Singleton
import services.CacheService
import services.Keys
import types.AlphanumericId
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.postgresql.util.PSQLException
import types.DataAccessException

import play.api.i18n.Messages

abstract class BioMaterialTypeService {
  def list(): Future[Seq[BioMaterialType]]
  def insert(bmt: BioMaterialType): Future[Int]
  def update(bmt: BioMaterialType): Future[Int]
  def delete(bmtId: String): Future[Int]
}

@Singleton
class CachedBioMaterialTypeService @Inject() (cache: CacheService, bioMatTypeRepo: BioMaterialTypeRepository, implicit val messages: Messages) extends BioMaterialTypeService {

  val errorPf: PartialFunction[Throwable, Int] = {
    case psql: PSQLException => {
      psql.getSQLState match {
        case "23505" => throw DataAccessException(messages("error.E0901"), psql)
        case _       => throw DataAccessException(messages("error.E0630"), psql)
      }
    }
  }
  
  private def cleanCache = {
    cache.pop(Keys.biomaterialType)
  }

  override def list(): Future[Seq[BioMaterialType]] = {
    cache.asyncGetOrElse(Keys.biomaterialType)(bioMatTypeRepo.list())
  }

  override def insert(bmt: BioMaterialType): Future[Int] = {
    bioMatTypeRepo.insert(BioMaterialType(bmt.id, bmt.name, bmt.description)).map { res =>
      this.cleanCache
      res
    }.recover(errorPf)
  }

  override def update(bmt: BioMaterialType): Future[Int] = {
    bioMatTypeRepo.update(bmt).map { res =>
      this.cleanCache
      res
    }
  }

  override def delete(bmtId: String): Future[Int] = {
    bioMatTypeRepo.delete(bmtId).map { res =>
      this.cleanCache
      res
    }.recover(errorPf)
  }

}
