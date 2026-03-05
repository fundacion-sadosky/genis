package configdata

import models.Tables.laboratories
import types.Laboratory
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}

trait LaboratoryRepository {
  def add(lab: Laboratory): Future[Int]
  def getAll(): Future[Seq[Laboratory]]
  def get(id: String): Future[Option[Laboratory]]
  def update(lab: Laboratory): Future[Int]
}

@Singleton
class SlickLaboratoryRepository @Inject() (
  db: slick.jdbc.JdbcBackend.Database
)(implicit ec: ExecutionContext) extends LaboratoryRepository {

  private def toRow(lab: Laboratory): models.Tables.LaboratoryRow =
    models.Tables.LaboratoryRow(
      lab.code,
      lab.name,
      lab.country,
      lab.province,
      lab.address,
      lab.telephone,
      lab.contactEmail,
      lab.dropIn,
      lab.dropOut
    )

  private def fromRow(row: models.Tables.LaboratoryRow): Laboratory =
    Laboratory(
      row.name,
      row.codeName,
      row.country,
      row.province,
      row.address,
      row.telephone,
      row.contactEmail,
      row.dropIn,
      row.dropOut
    )

  override def add(lab: Laboratory): Future[Int] = {
    db.run(laboratories += toRow(lab))
  }

  override def getAll(): Future[Seq[Laboratory]] = {
    db.run(laboratories.result).map(_.map(fromRow))
  }

  override def get(id: String): Future[Option[Laboratory]] = {
    db.run(laboratories.filter(_.codeName === id).result.headOption).map(_.map(fromRow))
  }

  override def update(lab: Laboratory): Future[Int] = {
    db.run(laboratories.filter(_.codeName === lab.code).update(toRow(lab)))
  }
}
