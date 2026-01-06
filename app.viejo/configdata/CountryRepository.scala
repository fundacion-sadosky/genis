package configdata

import scala.concurrent.Future
import scala.language.postfixOps
import javax.inject.Inject
import javax.inject.Singleton
import models.Tables
import play.api.Application
import play.api.db.slick.Config.driver.simple.Compiled
import play.api.db.slick.Config.driver.simple.Column
import play.api.db.slick.Config.driver.simple.TableQuery
import play.api.db.slick.Config.driver.simple.columnExtensionMethods
import play.api.db.slick.Config.driver.simple.runnableCompiledToAppliedQueryInvoker
import play.api.db.slick.Config.driver.simple.slickDriver
import play.api.db.slick.Config.driver.simple.stringColumnType
import play.api.db.slick.DB
import play.api.db.slick.DBAction
import util.DefaultDb

abstract class CountryRepository extends DefaultDb {
  def getCountries(): Future[Seq[Country]]
  def getProvinces(country: String): Future[Seq[Province]]
}

@Singleton
class SlickCountryRepository @Inject() (implicit app: Application) extends CountryRepository {

  val countries: TableQuery[Tables.Country] = Tables.Country
  val provinces: TableQuery[Tables.Province] = Tables.Province

  private def queryDefineQueryGetProvinces(country: Column[String]) = for (
    pr <- provinces if pr.country === country
  ) yield (pr)

  val queryGetProvinces = Compiled(queryDefineQueryGetProvinces _)

  val queryGetCountries = Compiled(for (cn <- countries) yield cn)

  override def getCountries(): Future[Seq[Country]] = Future {
    DB.withSession { implicit session =>
      queryGetCountries.list map { cn => Country(cn.code, cn.name) }
    }
  }

  override def getProvinces(country: String): Future[Seq[Province]] = Future {
    DB.withSession { implicit session =>
      queryGetProvinces(country).list map { pr => Province(pr.code, pr.name) }
    }
  }
}