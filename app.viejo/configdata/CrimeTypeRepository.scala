package configdata

import scala.concurrent.Future
import javax.inject.Inject
import javax.inject.Singleton
import models.Tables
import play.api.Application
import play.api.db.slick.Config.driver.simple.TableQuery
import play.api.db.slick.Config.driver.simple.columnExtensionMethods
import play.api.db.slick.Config.driver.simple.queryToAppliedQueryInvoker
import play.api.db.slick.Config.driver.simple.queryToInsertInvoker
import play.api.db.slick.Config.driver.simple.stringColumnType
import play.api.db.slick.DB
import play.api.db.slick.DBAction
import scala.slick.driver.PostgresDriver.simple.Compiled
import scala.slick.driver.PostgresDriver.simple.runnableCompiledToAppliedQueryInvoker
import scala.slick.driver.PostgresDriver.simple.slickDriver
import util.DefaultDb

abstract class CrimeTypeRepository extends DefaultDb {
  def list(): Future[Seq[CrimeType]]
}

@Singleton
class SlickCrimeTypeRepository @Inject() (implicit app: Application) extends CrimeTypeRepository {

  val crimeTypes: TableQuery[Tables.CrimeType] = Tables.CrimeType
  val crimes: TableQuery[Tables.CrimeInvolved] = Tables.CrimeInvolved

  val queryCrimeType = Compiled(for (
    (crimeType, crime) <- crimeTypes innerJoin crimes
      on (_.id === _.crimeType)
  ) yield (
    crimeType.id, crimeType.name, crimeType.description,
    crime.id, crime.name, crime.description))

  override def list(): Future[Seq[CrimeType]] = Future {
    DB.withSession { implicit session =>

      val crimeTypeFlatList = queryCrimeType.list

      val crimeTypeGrouped = crimeTypeFlatList groupBy { tuple => (tuple._1, tuple._2, tuple._3) }

      val crimeTypeTree = crimeTypeGrouped map (
        map => {
          val crimeTypeTuple = map._1
          val crimeTypeId = crimeTypeTuple._1
          val crimeTypeName = crimeTypeTuple._2
          val crimeTypeDescription = crimeTypeTuple._3
          val crimes: Seq[Crime] = map._2 map (crimeTup => Crime(crimeTup._4, crimeTup._5, crimeTup._6))
          CrimeType(crimeTypeId, crimeTypeName, crimeTypeDescription, crimes)
        })
      crimeTypeTree.toList
    }
  }

}
