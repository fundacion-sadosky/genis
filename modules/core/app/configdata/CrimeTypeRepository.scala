package configdata

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import slick.jdbc.PostgresProfile.api._
import configdata.{CrimeType, Crime}
import models.Tables

abstract class CrimeTypeRepository {
  def list(): Future[Seq[CrimeType]]
}

@Singleton
class SlickCrimeTypeRepository @Inject() (
  db: slick.jdbc.JdbcBackend.Database
)(implicit ec: ExecutionContext) extends CrimeTypeRepository {

  private val crimeTypes = Tables.crimeTypes
  private val crimes = Tables.crimeInvolved

  override def list(): Future[Seq[CrimeType]] = {
    val joinQuery = for {
      (ct, c) <- crimeTypes join crimes on (_.id === _.crimeType)
    } yield (ct.id, ct.name, ct.description, c.id, c.name, c.description)

    db.run(joinQuery.result).map { rows =>
      val grouped = rows.groupBy(r => (r._1, r._2, r._3))
      grouped.map { case ((ctId, ctName, ctDesc), crimeRows) =>
        val crimeList = crimeRows.map { case (_, _, _, cId, cName, cDesc) =>
          Crime(cId, cName, cDesc)
        }
        CrimeType(ctId, ctName, ctDesc, crimeList)
      }.toSeq
    }
  }
}
