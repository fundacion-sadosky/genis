package motive
import javax.inject.Singleton

import models.Tables
import play.api.Logger
import play.api.db.slick.Config.driver.simple.TableQuery
import util.{DefaultDb, Transaction}

import play.api.i18n.Messages

import scala.concurrent.Future
import scala.slick.driver.PostgresDriver.simple._
import scala.slick.lifted.Compiled
abstract class MotiveRepository extends DefaultDb with Transaction {
  def getMotives(motiveType: Long, editable:Boolean): Future[List[Motive]]
  def getMotivesTypes(): Future[List[MotiveType]]
  def deleteMotiveById(id: Long): Future[Either[String, Unit]]
  def deleteLogicalMotiveById(id: Long): Future[Either[String, Unit]]
  def insert(row: Motive): Future[Either[String, Long]]
  def update(row: Motive): Future[Either[String, Unit]]
//  def insertMotiveType(row: MotiveType): Future[Either[String, Long]]
}

@Singleton
class SlickMotiveRepository extends MotiveRepository {
  val logger: Logger = Logger(this.getClass())
  val motiveTable: TableQuery[Tables.Motive] = Tables.Motive
  val motiveTypeTable: TableQuery[Tables.MotiveType] = Tables.MotiveType

  private def queryGetMotiveById(id: Column[Long]) = motiveTable.filter(_.id === id)

  val getMotiveById = Compiled(queryGetMotiveById _)

  private def queryGetByMotiveType(id: Column[Long]) = motiveTable.filter(_.motiveType === id).filter(_.deleted === false).filter(_.freeText === false).sortBy(_.id)
  private def queryGetAllByMotiveType(id: Column[Long]) = motiveTable.filter(_.motiveType === id).sortBy(_.id).filter(_.deleted === false)

  val getByMotiveType = Compiled(queryGetByMotiveType _)
  val getAllByMotiveType = Compiled(queryGetAllByMotiveType _)

  val getAllMotiveTypes = Compiled(for ((mt) <- motiveTypeTable.sortBy(_.id))
    yield (mt.id, mt.description))

  override def getMotives(motiveType: Long, abm:Boolean): Future[List[Motive]] =  {
    if(abm){
      // No le devuelvo el Motivo 'Otro'
      getMotivesForABM(motiveType)
    }else{
      getAllMotives(motiveType)
    }
  }

  def getMotivesForABM(motiveType: Long): Future[List[Motive]] =  {
    this.runInTransactionAsync  { implicit session =>
      getByMotiveType(motiveType).list.map(x => Motive(x.id, x.motiveType, x.description, x.freeText))
    }
  }
  def getAllMotives(motiveType: Long): Future[List[Motive]] =  {
    this.runInTransactionAsync  { implicit session =>
      getAllByMotiveType(motiveType).list.map(x => Motive(x.id, x.motiveType, x.description, x.freeText))
    }
  }
  override def getMotivesTypes(): Future[List[MotiveType]] =  {
    this.runInTransactionAsync { implicit session =>
      getAllMotiveTypes.list.map(x => MotiveType(x._1, x._2))
    }
  }

  override def deleteMotiveById(id: Long): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getMotiveById(id).delete
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Left(e.getMessage)
        }
      }
    }
    }
  }
  override def deleteLogicalMotiveById(id: Long): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        motiveTable.filter(_.id === id).map(x => x.deleted).update(true)
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Left(e.getMessage)
        }
      }
    }
    }
  }
  override def insert(row: Motive): Future[Either[String, Long]] = {

    this.runInTransactionAsync { implicit session => {
      try {
        val id = (motiveTable returning motiveTable.map(_.id)) += models.Tables.MotiveRow(id = 0, motiveType = row.motiveType, description = row.description, freeText = row.freeText)
        Right(id)
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Left(e.getMessage)
        }
      }
    }
    }
  }

  override def update(row: Motive): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        motiveTable.filter(_.id === row.id).map(x => x.description).update(row.description)
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage,e)
          Left(Messages("error.E0500"))
        }
      }

    }
    }
  }

//  override def insertMotiveType(row: MotiveType): Future[Either[String, Long]] = {
//
//    this.runInTransactionAsync { implicit session => {
//      try {
//        val id = (motiveTypeTable returning motiveTypeTable.map(_.id)) += models.Tables.MotiveTypeRow(id = row.id, description = row.description)
//        Right(id)
//      } catch {
//        case e: Exception => {
//          Left(e.getMessage)
//        }
//      }
//    }
//    }
//  }

}
