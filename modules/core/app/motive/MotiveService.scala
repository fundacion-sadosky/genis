package motive

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

trait MotiveService {
  def getMotives(motiveType: Long, editable: Boolean): Future[List[Motive]]
  def getMotivesTypes(): Future[List[MotiveType]]
  def deleteMotiveById(id: Long): Future[Either[String, Unit]]
  def insert(row: Motive): Future[Either[String, Long]]
  def update(row: Motive): Future[Either[String, Unit]]
}

@Singleton
class MotiveServiceImpl @Inject()(motiveRepository: MotiveRepository) extends MotiveService {

  override def getMotives(motiveType: Long, editable: Boolean): Future[List[Motive]] =
    motiveRepository.getMotives(motiveType, editable)

  override def getMotivesTypes(): Future[List[MotiveType]] =
    motiveRepository.getMotivesTypes()

  override def deleteMotiveById(id: Long): Future[Either[String, Unit]] =
    motiveRepository.deleteLogicalMotiveById(id)

  override def insert(row: Motive): Future[Either[String, Long]] =
    motiveRepository.insert(row)

  override def update(row: Motive): Future[Either[String, Unit]] =
    motiveRepository.update(row)
}
