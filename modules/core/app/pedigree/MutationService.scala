package pedigree

import kits.FullLocus
import scala.concurrent.Future

trait MutationService:
  def addLocus(fullLocus: FullLocus): Future[Either[String, Unit]]

class NoOpMutationService extends MutationService:
  override def addLocus(fullLocus: FullLocus): Future[Either[String, Unit]] =
    Future.successful(Right(()))
