package disclaimer

import javax.inject.{Inject, Singleton}

import models.Tables
import play.api.Application
import play.api.db.slick.Config.driver.simple.{TableQuery, queryToAppliedQueryInvoker}
import play.api.db.slick.DB
import util.{DefaultDb, Transaction}

import scala.concurrent.Future
import scala.language.postfixOps
import slick.jdbc.{StaticQuery => Q}

abstract class DisclaimerService  extends DefaultDb with Transaction {
  def get(): Future[Disclaimer]
}

@Singleton
class DisclaimerServiceImpl @Inject()(disclaimerRepository: DisclaimerRepository) extends DisclaimerService {

  override def get(): Future[Disclaimer] = {
    disclaimerRepository.get()
  }

}