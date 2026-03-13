package services

import scala.concurrent.{ExecutionContext, Future}
import types.Laboratory
import configdata.LaboratoryRepository


trait LaboratoryService {
  def list(): Future[Seq[Laboratory]]
  def add(lab: Laboratory): Future[Int]
  def get(id: String): Future[Option[Laboratory]]
  def update(lab: Laboratory): Future[Int]
}

import javax.inject.{Inject, Singleton}

@Singleton
class LaboratoryServiceImpl @Inject() (
  labRepository: LaboratoryRepository
)(implicit ec: ExecutionContext) extends LaboratoryService {
  override def list(): Future[Seq[Laboratory]] = labRepository.getAll()
  override def add(lab: Laboratory): Future[Int] = labRepository.add(lab)
  override def get(id: String): Future[Option[Laboratory]] = labRepository.get(id)
  override def update(lab: Laboratory): Future[Int] = labRepository.update(lab)
}
