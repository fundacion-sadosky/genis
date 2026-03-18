package pedigree

import kits.FullLocus
import scala.concurrent.Future

// TODO: Migrar MutationService real desde pedigree.MutationService (legacy: pedigree/MutationService.scala)
//  Métodos pendientes: saveLocusAlleles, refreshAllKisSecuential, y los usados por LocusService
trait MutationService:
  def addLocus(fullLocus: FullLocus): Future[Either[String, Unit]]

// TODO: Reemplazar por implementación real cuando se migre el módulo pedigree
class NoOpMutationService extends MutationService:
  override def addLocus(fullLocus: FullLocus): Future[Either[String, Unit]] =
    Future.successful(Right(()))
