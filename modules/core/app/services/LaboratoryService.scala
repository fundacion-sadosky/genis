package services

import scala.concurrent.Future
import types.Laboratory


trait LaboratoryService {
  def list(): Future[Seq[Laboratory]]
  def listDescriptive(): Future[Seq[Laboratory]]
  def add(lab: Laboratory): Future[Either[String, String]]
  def get(id: String): Future[Option[Laboratory]]
  def update(lab: Laboratory): Future[Either[String, String]]
}

import javax.inject.Singleton

@Singleton
class LaboratoryServiceImpl extends LaboratoryService {
  override def list(): Future[Seq[Laboratory]] = Future.successful(Seq.empty)
  override def listDescriptive(): Future[Seq[Laboratory]] = Future.successful(Seq.empty)
  override def add(lab: Laboratory): Future[Either[String, String]] = Future.successful(Right(""))
  override def get(id: String): Future[Option[Laboratory]] = Future.successful(None)
  override def update(lab: Laboratory): Future[Either[String, String]] = Future.successful(Right(""))
}
