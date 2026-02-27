package configdata

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import types.AlphanumericId

// Modelo base para acceso a datos de BioMaterialType en core
trait BioMaterialTypeRepository {
  def list(): Future[Seq[BioMaterialType]]
  def insert(bmt: BioMaterialType): Future[Int]
  def update(bmt: BioMaterialType): Future[Int]
  def delete(bmtId: String): Future[Int]
}

// Implementación base (a completar con acceso real a datos)
@Singleton
class SlickBioMaterialTypeRepository @Inject() (
  // Inyectar dependencias necesarias para acceso a datos
)(implicit ec: ExecutionContext) extends BioMaterialTypeRepository {
  override def list(): Future[Seq[BioMaterialType]] = Future.successful(Seq.empty) // TODO: Implementar
  override def insert(bmt: BioMaterialType): Future[Int] = Future.successful(1) // TODO: Implementar
  override def update(bmt: BioMaterialType): Future[Int] = Future.successful(1) // TODO: Implementar
  override def delete(bmtId: String): Future[Int] = Future.successful(1) // TODO: Implementar
}
