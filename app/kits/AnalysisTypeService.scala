package kits

import javax.inject.{Inject, Singleton}

import services.{CacheService, Keys}

import scala.concurrent.Future

abstract class AnalysisTypeService {
  def list(): Future[Seq[AnalysisType]]
  def getByName(name: String): Future[Option[AnalysisType]]
  def getById(id: Int): Future[Option[AnalysisType]]
}

@Singleton
class AnalysisTypeServiceImpl @Inject() (cache: CacheService, repository: AnalysisTypeRepository) extends AnalysisTypeService {

  override def list(): Future[Seq[AnalysisType]] = {
    cache.asyncGetOrElse(Keys.analysisTypes)(repository.list())
  }

  override def getByName(name: String): Future[Option[AnalysisType]] = {
    repository.getByName(name)
  }

  override def getById(id: Int): Future[Option[AnalysisType]] = {
    repository.getById(id)
  }

}
