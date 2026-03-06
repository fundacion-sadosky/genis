package disclaimer

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import models.Tables

abstract class DisclaimerRepository {
  def get(): Future[Disclaimer]
}

class SlickDisclaimerRepository @Inject() (implicit ec: ExecutionContext) extends DisclaimerRepository {
  private val db = Database.forConfig("slick.dbs.default.db")
  private val disclaimerTable = Tables.Disclaimer

  override def get(): Future[Disclaimer] = {
    db.run(disclaimerTable.result.headOption).map(textOpt => Disclaimer(textOpt.flatten))
  }
}