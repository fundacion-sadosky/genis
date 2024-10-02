package audit

import java.sql.Timestamp
import java.util.Date

import scala.concurrent.Future
import slick.driver.PostgresDriver.simple._
import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.rowset.serial.SerialBlob

import models.Tables
import models.Tables._
import play.api.Application
import play.api.db.slick.Config.driver.simple.TableQuery
import play.api.db.slick.Config.driver.simple.columnExtensionMethods
import play.api.db.slick.Config.driver.simple.longColumnType
import play.api.db.slick.Config.driver.simple.queryToAppliedQueryInvoker
import play.api.db.slick.Config.driver.simple.queryToInsertInvoker
import play.api.db.slick.Config.driver.simple.stringColumnType
import play.api.db.slick.Config.driver.simple.valueToConstColumn
import play.api.db.slick.DB
import play.api.db.slick.DBAction
import java.text.SimpleDateFormat

import scala.language.implicitConversions
import play.api.Logger
import play.api.libs.concurrent.Akka
import util.LogDb

import scala.collection.immutable.IndexedSeq
import types.TotpToken

abstract class OperationLogRepository extends LogDb {
  def add(entry: SignedOperationLogEntry): Future[Unit]
  def createLot(kZero: Key): Future[Long]
  def listLots(limit: Int, offset: Int): Future[Seq[OperationLogLot]]
  def getLot(id: Long): Future[OperationLogLot]
  def countLots(): Future[Int]
  def countLogs(operationLogSearch: OperationLogSearch): Future[Int]
  def searchLogs(operationLogSearch: OperationLogSearch): Future[IndexedSeq[SignedOperationLogEntry]]
}

@Singleton
class SlickOperationLogRepository @Inject() (implicit app: Application) extends OperationLogRepository {

  val logger: Logger = Logger(this.getClass)

  implicit private def date2timestamp(date: java.util.Date) = new java.sql.Timestamp(date.getTime)

  implicit private def timestamp2date(timestamp: java.sql.Timestamp) = new java.util.Date(timestamp.getTime)

  implicit private def timestamp2string(timestamp: java.sql.Timestamp) = {
    val time = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSXXX")
    time.format(timestamp)
  }

  val operationLogLots: TableQuery[Tables.OperationLogLot] = Tables.OperationLogLot
  val operationLogRecords: TableQuery[Tables.OperationLogRecord] = Tables.OperationLogRecord

  override def countLogs(operationLogSearch: OperationLogSearch) = Future {
    DB("logDb").withSession { implicit session =>
      createSearchQuery(operationLogSearch).length.run
    }
  }

  override def countLots(): Future[Int] = Future {
    DB("logDb").withSession { implicit session =>
      operationLogLots.length.run
    }
  }

  override def getLot(id: Long): Future[OperationLogLot] = Future {
    DB("logDb").withSession { implicit session =>
      val query = for { oll <- operationLogLots if oll.id === id } yield (oll)

      query.list.map { o =>
        new OperationLogLot(o.id, o.initTime, Key(o.keyZero))
      }.head
    }
  }

  override def add(entry: SignedOperationLogEntry): Future[Unit] = Future {

    DB("logDb").withTransaction { implicit session =>
      val operationLogRecord = new OperationLogRecordRow(0, entry.userId, entry.otp.map(_.text), entry.timestamp, entry.method, entry.path, entry.action, entry.buildNo, entry.result, entry.status, entry.signature.asHexaString, entry.lotId, entry.description)
      operationLogRecords += operationLogRecord
      ()
    }
  }

  override def listLots(limit: Int, offset: Int): Future[Seq[OperationLogLot]] = Future {
    DB("logDb").withSession { implicit session =>

      val query = (for (oll <- operationLogLots) yield (oll)).sortBy(_.id.desc)

      query.drop(offset).take(limit).list map {
        o =>
          val kzero: Key = Key(o.keyZero)
          new OperationLogLot(o.id, o.initTime, kzero)
      }
    }
  }

  override def createLot(kZero: Key): Future[Long] = Future {

    DB("logDb").withTransaction { implicit session =>

      val now = new Date()
      val operationLogLotRow = new OperationLogLotRow(0, kZero.asHexaString, now)

      val idOpLogLot = (operationLogLots returning operationLogLots.map(_.id)) += operationLogLotRow

      logger.info(s"new lot was created with id $idOpLogLot")
      idOpLogLot
    }
  }

  override def searchLogs(operationLogSearch: OperationLogSearch): Future[IndexedSeq[SignedOperationLogEntry]] = Future {
    DB("logDb").withSession { implicit session =>

      val searchQuery = createSearchQuery(operationLogSearch)
      val sortedQuery = sortQuery(searchQuery, operationLogSearch)

      sortedQuery.drop(operationLogSearch.page * operationLogSearch.pageSize).take(operationLogSearch.pageSize).iterator.toVector map {
        opr => SignedOperationLogEntry(opr.id, opr.userId, opr.otp.map(TotpToken(_)), opr.timestamp, opr.method, opr.path, opr.action, opr.buildNo, opr.result, opr.status, opr.lot, Key(opr.signature), opr.description)
      }
    }
  }

  private def createSearchQuery(operationLogSearch: OperationLogSearch) = {
    var query = for {
      record <- operationLogRecords
      if record.lot === operationLogSearch.lotId
    } yield record

    if (operationLogSearch.user.isDefined) {
      query = query.withFilter(record => record.userId === operationLogSearch.user.get)
    }
    if (operationLogSearch.operations.isDefined) {
      query = query.withFilter(record => record.description inSet operationLogSearch.operations.get)
    }
    if (operationLogSearch.hourFrom.isDefined) {
      val timestamp: Timestamp = operationLogSearch.hourFrom.get
      query = query.withFilter(record => record.timestamp >= timestamp)
    }
    if (operationLogSearch.hourUntil.isDefined) {
      val timestamp: Timestamp = operationLogSearch.hourUntil.get
      query = query.withFilter(record => record.timestamp <= timestamp)
    }
    if (operationLogSearch.result.isDefined) {
      if (operationLogSearch.result.get) {
        query = query.withFilter(record => record.status === 200)
      } else {
        query = query.withFilter(record => record.status =!= 200)
      }
    }
    query
  }

  private def sortQuery(query: Query[Tables.OperationLogRecord, Tables.OperationLogRecordRow, scala.Seq], operationLogSearch: OperationLogSearch) = {
    if (operationLogSearch.sortField.isDefined) {
      if (operationLogSearch.ascending.isDefined && operationLogSearch.ascending.get) {
        operationLogSearch.sortField.get match {
          case "id" => query.sortBy(_.id.asc)
          case "timestamp" => query.sortBy(_.timestamp.asc)
          case "userId" => query.sortBy(_.userId.asc)
        }
      } else {
        operationLogSearch.sortField.get match {
          case "id" => query.sortBy(_.id.desc)
          case "timestamp" => query.sortBy(_.timestamp.desc)
          case "userId" => query.sortBy(_.userId.desc)
        }
      }
    } else {
      query.sortBy(_.id.desc)
    }
  }

}
