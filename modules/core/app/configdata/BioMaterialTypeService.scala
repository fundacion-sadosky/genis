package configdata

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import javax.inject.Inject
import javax.inject.Singleton
import services.CacheService
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
  repo: BioMaterialTypeRepository
)(implicit ec: ExecutionContext) extends BioMaterialTypeService {
  override def list(): Future[Seq[BioMaterialType]] = repo.list()
  override def insert(bmt: BioMaterialType): Future[Int] = repo.insert(bmt)
  override def update(bmt: BioMaterialType): Future[Int] = repo.update(bmt)
  override def delete(bmtId: String): Future[Int] = repo.delete(bmtId)
}
