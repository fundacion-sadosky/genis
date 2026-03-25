package kits

import javax.inject.Singleton
import scala.concurrent.Future

trait AnalysisTypeService {
  def list(): Future[Seq[AnalysisType]]
  def getByName(name: String): Future[Option[AnalysisType]]
  def getById(id: Int): Future[Option[AnalysisType]]
}

@Singleton
class AnalysisTypeServiceStub extends AnalysisTypeService {
  override def list(): Future[Seq[AnalysisType]] = Future.successful(Seq.empty)
  override def getByName(name: String): Future[Option[AnalysisType]] = Future.successful(None)
  override def getById(id: Int): Future[Option[AnalysisType]] = Future.successful(None)
}
