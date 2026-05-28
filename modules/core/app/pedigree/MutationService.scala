package pedigree

import kits.FullLocus
import profile.Profile
import scala.concurrent.Future

// TODO: Migrar MutationService real desde pedigree.MutationService (legacy: pedigree/MutationService.scala)
//  Implementación completa en new-dev — pendiente de migrar infraestructura pedigree (Repository, Models, Actor)
trait MutationService:
  def addLocus(fullLocus: FullLocus): Future[Either[String, Unit]]
  def saveLocusAlleles(list: List[(String, Double)]): Future[Either[String, Int]]
  def refreshAllKis(): Future[Unit]
  def refreshAllKisSecuential(): Future[Unit]

@jakarta.inject.Singleton
class MutationServiceStub extends MutationService:
  override def addLocus(fullLocus: FullLocus): Future[Either[String, Unit]] =
    Future.successful(Right(()))
  override def saveLocusAlleles(list: List[(String, Double)]): Future[Either[String, Int]] =
    Future.successful(Right(0))
  override def refreshAllKis(): Future[Unit] =
    Future.successful(())
  override def refreshAllKisSecuential(): Future[Unit] =
    Future.successful(())
