package disclaimer

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import models.Tables

abstract class DisclaimerRepository {
  def get(): Future[Disclaimer]
}

class SlickDisclaimerRepository @Inject() (db: slick.jdbc.JdbcBackend.Database)(implicit ec: ExecutionContext) extends DisclaimerRepository {
  private val disclaimerTable = Tables.Disclaimer

  override def get(): Future[Disclaimer] = {
    db.run(disclaimerTable.result.headOption).map(textOpt => Disclaimer(textOpt.flatten))
  }
}