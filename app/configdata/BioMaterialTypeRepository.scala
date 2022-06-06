package configdata

import scala.concurrent.Future
import javax.inject.Inject
import javax.inject.Singleton
import models.Tables
import models.Tables.BioMaterialTypeRow
import play.api.Application
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.Config.driver.simple.TableQuery
import play.api.db.slick.Config.driver.simple.columnExtensionMethods
import play.api.db.slick.Config.driver.simple.queryToAppliedQueryInvoker
import play.api.db.slick.Config.driver.simple.queryToInsertInvoker
import play.api.db.slick.Config.driver.simple.runnableCompiledToAppliedQueryInvoker
import play.api.db.slick.Config.driver.simple.slickDriver
import play.api.db.slick.Config.driver.simple.stringColumnType
import play.api.db.slick.DB
import play.api.db.slick.DBAction
import types.AlphanumericId
import util.DefaultDb
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import types.DataAccessException

abstract class BioMaterialTypeRepository extends DefaultDb {
  def list(): Future[Seq[BioMaterialType]]
  def insert(bmt: BioMaterialType): Future[Int]
  def update(bmt: BioMaterialType): Future[Int]
  def delete(bmtId: String): Future[Int]
}

@Singleton
class SlickBioMaterialTypeRepository @Inject() (implicit app: Application) extends BioMaterialTypeRepository {

  val bioMaterialTypes: TableQuery[Tables.BioMaterialType] = Tables.BioMaterialType

  val queryGetBioMaterialType = Compiled(for (bmt <- bioMaterialTypes) yield (bmt))

  private def queryDefineUpdateBmt(id: Column[String]) = for {
    bmt <- bioMaterialTypes if id === bmt.id
  } yield ( bmt.name, bmt.description)
  
  val queryUpdateBmt = Compiled(queryDefineUpdateBmt _)
  
  val queryGetBmt = Compiled{ id: Column[String] =>
    for ( bmt <- bioMaterialTypes if id === bmt.id ) yield bmt
  }
  
  override def list(): Future[Seq[BioMaterialType]] = Future {
    DB.withSession { implicit session =>
      queryGetBioMaterialType.list.map(bmtr => BioMaterialType(AlphanumericId(bmtr.id), bmtr.name, bmtr.description))
    }
  }
  
  override def insert(bmt: BioMaterialType): Future[Int] = Future {
    DB.withTransaction { implicit session => 
      val row = BioMaterialTypeRow(bmt.id.text,bmt.name,bmt.description)
      bioMaterialTypes += row
    }
  }
  
  override def update(bmt: BioMaterialType): Future[Int] = Future {
    DB.withTransaction { implicit session => 
        queryUpdateBmt(bmt.id.text).update((bmt.name,bmt.description))
    }
  }
  
  override def delete(bmtId: String): Future[Int] = Future {
    DB.withTransaction { implicit session => 
        queryGetBmt(bmtId).delete
    }
  }
}
