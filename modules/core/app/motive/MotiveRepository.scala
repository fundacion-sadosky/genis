package motive

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import models.Tables

trait MotiveRepository {
  def getMotives(motiveType: Long, editable: Boolean): Future[List[Motive]]
  def getMotivesTypes(): Future[List[MotiveType]]
  def deleteMotiveById(id: Long): Future[Either[String, Unit]]
  def deleteLogicalMotiveById(id: Long): Future[Either[String, Unit]]
  def insert(row: Motive): Future[Either[String, Long]]
  def update(row: Motive): Future[Either[String, Unit]]
}

@Singleton
class SlickMotiveRepository @Inject()(implicit ec: ExecutionContext) extends MotiveRepository {

  private val db = Database.forConfig("slick.dbs.default.db")
  private val motiveTable     = Tables.Motive
  private val motiveTypeTable = Tables.MotiveType

  override def getMotives(motiveType: Long, editable: Boolean): Future[List[Motive]] = {
    val query = if (editable) {
      // For ABM: exclude freeText motives
      motiveTable
        .filter(m => m.motiveType === motiveType && !m.deleted && !m.freeText)
        .sortBy(_.id)
    } else {
      // For display: all non-deleted motives
      motiveTable
        .filter(m => m.motiveType === motiveType && !m.deleted)
        .sortBy(_.id)
    }
    db.run(query.result).map(_.map(r => Motive(r.id, r.motiveType, r.description, r.freeText)).toList)
  }

  override def getMotivesTypes(): Future[List[MotiveType]] = {
    db.run(motiveTypeTable.sortBy(_.id).result)
      .map(_.map(r => MotiveType(r.id, r.description)).toList)
  }

  override def deleteMotiveById(id: Long): Future[Either[String, Unit]] = {
    db.run(motiveTable.filter(_.id === id).delete)
      .map(_ => Right(()))
      .recover { case e: Exception => Left(e.getMessage) }
  }

  override def deleteLogicalMotiveById(id: Long): Future[Either[String, Unit]] = {
    db.run(motiveTable.filter(_.id === id).map(_.deleted).update(true))
      .map(_ => Right(()))
      .recover { case e: Exception => Left(e.getMessage) }
  }

  override def insert(row: Motive): Future[Either[String, Long]] = {
    val newRow = Tables.MotiveRow(id = 0L, motiveType = row.motiveType, description = row.description, freeText = row.freeText)
    db.run((motiveTable returning motiveTable.map(_.id)) += newRow)
      .map(id => Right(id))
      .recover { case e: Exception => Left(e.getMessage) }
  }

  override def update(row: Motive): Future[Either[String, Unit]] = {
    db.run(motiveTable.filter(_.id === row.id).map(_.description).update(row.description))
      .map(_ => Right(()))
      .recover { case e: Exception => Left(e.getMessage) }
  }
}
