package motive

import javax.inject.{Inject, Singleton}

import scala.concurrent.Future

abstract class MotiveService {
  def getMotives(motiveType: Long,editable:Boolean): Future[List[Motive]]
  def getMotivesTypes(): Future[List[MotiveType]]
  def deleteMotiveById(id: Long): Future[Either[String, Unit]]
  def insert(row: Motive): Future[Either[String, Long]]
  def update(row: Motive): Future[Either[String, Unit]]
}
@Singleton
class MotiveServiceImpl @Inject()(motiveRepository: MotiveRepository) extends MotiveService {

  override def getMotives(motiveType: Long,editable:Boolean) = {
    motiveRepository.getMotives(motiveType,editable)
  }

  override def getMotivesTypes() = {
    motiveRepository.getMotivesTypes()
  }

  override def deleteMotiveById(id: Long) = {
    motiveRepository.deleteLogicalMotiveById(id)
  }

  override def insert(row: Motive) = {
    motiveRepository.insert(row)
  }

  override def update(row: Motive) = {
    motiveRepository.update(row)
  }

}