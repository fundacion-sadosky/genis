package configdata

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import javax.inject.Inject
import javax.inject.Singleton
import org.postgresql.util.PSQLException
import services.{CacheService, BioMaterialTypeKey}
import types.AlphanumericId
import types.DataAccessException

trait BioMaterialTypeService {
  def list(): Future[Seq[BioMaterialType]]
  def insert(bmt: BioMaterialType): Future[Int]
  def update(bmt: BioMaterialType): Future[Int]
  def delete(bmtId: String): Future[Int]
}

@Singleton
class BioMaterialTypeServiceImpl @Inject() (
  cache: CacheService,
  repo: BioMaterialTypeRepository
)(implicit ec: ExecutionContext) extends BioMaterialTypeService {

  // Mapeo de errores de base de datos (mismo comportamiento que el legacy errorPf)
  private val errorPf: PartialFunction[Throwable, Int] = {
    case psql: PSQLException =>
      psql.getSQLState match {
        case "23505" => throw DataAccessException("E0901: Nombre ya utilizado.", psql)
        case _       => throw DataAccessException("E0630: Error inesperado en la base de datos.", psql)
      }
  }

  private def cleanCache(): Unit = cache.pop(BioMaterialTypeKey)

  override def list(): Future[Seq[BioMaterialType]] =
    cache.asyncGetOrElse(BioMaterialTypeKey)(repo.list())

  override def insert(bmt: BioMaterialType): Future[Int] =
    repo.insert(bmt).map { res => cleanCache(); res }.recover(errorPf)

  override def update(bmt: BioMaterialType): Future[Int] =
    repo.update(bmt).map { res => cleanCache(); res }

  override def delete(bmtId: String): Future[Int] =
    repo.delete(bmtId).map { res => cleanCache(); res }.recover(errorPf)
}
