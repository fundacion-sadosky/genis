package disclaimer

import javax.inject.{Inject, Singleton}

import models.Tables
import play.api.Application
import play.api.db.slick.Config.driver.simple.{TableQuery, queryToAppliedQueryInvoker}
import play.api.db.slick.DB
import util.{DefaultDb, Transaction}

import scala.concurrent.Future
import scala.language.postfixOps

abstract class DisclaimerRepository  extends DefaultDb with Transaction {
  def get(): Future[Disclaimer]
}
@Singleton
class SlickDisclaimerRepository @Inject() (implicit app: Application) extends DisclaimerRepository {

  val disclaimerData: TableQuery[Tables.Disclaimer] = Tables.Disclaimer

  override def get(): Future[Disclaimer] = Future {

    DB.withTransaction { implicit session =>

      Disclaimer(disclaimerData.map(_.text).firstOption)
    }
  }

}