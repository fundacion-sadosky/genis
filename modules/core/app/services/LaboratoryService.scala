package services

import scala.concurrent.{ExecutionContext, Future}
import types.Laboratory
import configdata.LaboratoryRepository


trait LaboratoryService {
  def list(): Future[Seq[Laboratory]]
  def add(lab: Laboratory): Future[Either[String, String]]
  def get(id: String): Future[Option[Laboratory]]
  def update(lab: Laboratory): Future[Either[String, String]]
}

import javax.inject.{Inject, Named, Singleton}

@Singleton
class LaboratoryServiceImpl @Inject() (
  labRepository: LaboratoryRepository,
  @Named("labCode") labCode: String
)(implicit ec: ExecutionContext) extends LaboratoryService {
  override def list(): Future[Seq[Laboratory]] = labRepository.getAll()
  override def add(lab: Laboratory): Future[Either[String, String]] = labRepository.add(lab)

  // instance indica si el laboratorio es la instancia local (su code coincide con laboratory.code).
  override def get(id: String): Future[Option[Laboratory]] =
    labRepository.get(id).map(_.map(lab => lab.copy(instance = Some(lab.code == labCode))))

  override def update(lab: Laboratory): Future[Either[String, String]] = labRepository.update(lab)
}
