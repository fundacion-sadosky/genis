package laboratories

import javax.inject.{Inject, Named, Singleton}

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

abstract class LaboratoryService {

  def list(): Future[Seq[Laboratory]]
  def listDescriptive(): Future[Seq[Laboratory]]
  def add(lab: Laboratory): Future[Either[String, String]]
  def get(id: String): Future[Option[Laboratory]]
  def update(lab: Laboratory): Future[Either[String, String]]
}

@Singleton
class LaboratoryServiceImpl @Inject() (
  labRepository: LaboratoryRepository,
  @Named("labCode") val labCode: String
  ) extends LaboratoryService {

  override def list(): Future[Seq[Laboratory]] = {
    labRepository.getAll()
  }

  override def listDescriptive(): Future[Seq[Laboratory]] = {
    labRepository.getAllDescriptive()
  }

  override def add(lab: Laboratory): Future[Either[String, String]] = {
    labRepository.add(lab)
  }

  override def get(id: String): Future[Option[Laboratory]] = {
    labRepository.get(id).map {
      case Some(lab) => Some(Laboratory(lab.name, lab.code, lab.country, lab.province, lab.address, lab.telephone, lab.contactEmail, lab.dropIn, lab.dropOut, Some(lab.code.equals(labCode))))
      case None => None
    }
  }

  override def update(lab: Laboratory): Future[Either[String, String]] = {
    labRepository.update(lab)
  }
}