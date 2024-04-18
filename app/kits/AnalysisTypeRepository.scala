package kits

import javax.inject.{Inject, Singleton}

import models.Tables
import play.api.Application
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import util.DefaultDb

import scala.concurrent.Future

abstract class AnalysisTypeRepository extends DefaultDb {
  def list(): Future[List[AnalysisType]]
  def getByName(name: String): Future[Option[AnalysisType]]
  def getById(id: Int): Future[Option[AnalysisType]]
}

@Singleton
class SlickAnalysisTypeRepository @Inject() (implicit app: Application) extends AnalysisTypeRepository {

  val types: TableQuery[Tables.AnalysisType] = Tables.AnalysisType

  val queryGetAllCompiled = Compiled(for (analysis <- types) yield analysis)

  val queryGetByNameCompiled = Compiled { name: Column[String] =>
    for{
      analysis <- types if analysis.name === name
    } yield analysis
  }

  val queryGetByIdCompiled = Compiled { id: Column[Int] =>
    for{
      analysis <- types if analysis.id === id
    } yield analysis
  }

  override def list(): Future[List[AnalysisType]] = Future {
    DB.withSession { implicit session =>
      queryGetAllCompiled.list.map(f => AnalysisType(f.id, f.name, f.mitochondrial))
    }
  }

  override def getByName(name: String): Future[Option[AnalysisType]] = Future {
    DB.withSession { implicit session =>
      queryGetByNameCompiled(name).list.map(f => AnalysisType(f.id, f.name, f.mitochondrial)).headOption
    }
  }

  override def getById(id: Int): Future[Option[AnalysisType]] = Future {
    DB.withSession { implicit session =>
      queryGetByIdCompiled(id).list.map(f => AnalysisType(f.id, f.name, f.mitochondrial)).headOption
    }
  }

}
