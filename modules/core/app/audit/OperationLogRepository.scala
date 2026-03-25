package audit

import java.sql.Timestamp
import java.util.Date
import javax.inject.{Inject, Singleton}

import scala.collection.immutable.IndexedSeq
import scala.concurrent.{ExecutionContext, Future}

import play.api.Logger
import slick.jdbc.PostgresProfile.api._
import models.Tables
import models.Tables._
import types.TotpToken

trait OperationLogRepository {
  def add(entry: SignedOperationLogEntry): Future[Unit]
  def createLot(kZero: Key): Future[Long]
  def listLots(limit: Int, offset: Int): Future[Seq[OperationLogLot]]
  def getLot(id: Long): Future[OperationLogLot]
  def countLots(): Future[Int]
  def countLogs(search: OperationLogSearch): Future[Int]
  def searchLogs(search: OperationLogSearch): Future[IndexedSeq[SignedOperationLogEntry]]
}

@Singleton
class SlickOperationLogRepository @Inject()(
  db: slick.jdbc.JdbcBackend.Database
)(implicit ec: ExecutionContext) extends OperationLogRepository {

  private val logger: Logger = Logger(this.getClass)

  private val lots    = Tables.OperationLogLot
  private val records = Tables.OperationLogRecord

  private implicit def date2ts(d: Date): Timestamp   = new Timestamp(d.getTime)
  private implicit def ts2date(ts: Timestamp): Date   = new Date(ts.getTime)

  override def add(entry: SignedOperationLogEntry): Future[Unit] = {
    val row = OperationLogRecordRow(
      id        = 0L,
      userId    = entry.userId,
      otp       = entry.otp.map(_.text),
      timestamp = entry.timestamp,
      method    = entry.method,
      path      = entry.path,
      action    = entry.action,
      buildNo   = entry.buildNo,
      result    = entry.result,
      status    = entry.status,
      signature = entry.signature.asHexaString(),
      lot       = entry.lotId,
      description = entry.description
    )
    db.run((records += row).transactionally).map(_ => ())
  }

  override def createLot(kZero: Key): Future[Long] = {
    val row = OperationLogLotRow(id = 0L, keyZero = kZero.asHexaString(), initTime = date2ts(new Date()))
    val insert = (lots returning lots.map(_.id)) += row
    db.run(insert.transactionally).map { id =>
      logger.info(s"new lot created with id $id")
      id
    }
  }

  override def listLots(limit: Int, offset: Int): Future[Seq[OperationLogLot]] =
    db.run(lots.sortBy(_.id.desc).drop(offset).take(limit).result).map {
      _.map(r => audit.OperationLogLot(r.id, ts2date(r.initTime), Key(r.keyZero)))
    }

  override def getLot(id: Long): Future[OperationLogLot] =
    db.run(lots.filter(_.id === id).result.head).map(r =>
      audit.OperationLogLot(r.id, ts2date(r.initTime), Key(r.keyZero))
    )

  override def countLots(): Future[Int] =
    db.run(lots.length.result)

  override def countLogs(search: OperationLogSearch): Future[Int] =
    db.run(buildQuery(search).length.result)

  override def searchLogs(search: OperationLogSearch): Future[IndexedSeq[SignedOperationLogEntry]] = {
    val base   = buildQuery(search)
    val sorted = sortQuery(base, search)
    val paged  = sorted.drop(search.page * search.pageSize).take(search.pageSize)
    db.run(paged.result).map {
      _.map { r =>
        SignedOperationLogEntry(
          index       = r.id,
          userId      = r.userId,
          otp         = r.otp.map(TotpToken(_)),
          timestamp   = ts2date(r.timestamp),
          method      = r.method,
          path        = r.path,
          action      = r.action,
          buildNo     = r.buildNo,
          result      = r.result,
          status      = r.status,
          lotId       = r.lot,
          signature   = Key(r.signature),
          description = r.description
        )
      }.toIndexedSeq
    }
  }

  private def buildQuery(search: OperationLogSearch) = {
    var q = records.filter(_.lot === search.lotId)

    search.user.foreach(u => q = q.filter(_.userId === u))
    search.operations.foreach(ops => q = q.filter(_.description.inSet(ops)))
    search.hourFrom.foreach { d =>
      val ts: Timestamp = d
      q = q.filter(_.timestamp >= ts)
    }
    search.hourUntil.foreach { d =>
      val ts: Timestamp = d
      q = q.filter(_.timestamp <= ts)
    }
    search.result.foreach {
      case true  => q = q.filter(_.status === 200)
      case false => q = q.filter(_.status =!= 200)
    }
    q
  }

  private def sortQuery(
    q: Query[OperationLogRecordTable, OperationLogRecordRow, Seq],
    search: OperationLogSearch
  ) = {
    val asc = search.ascending.getOrElse(false)
    search.sortField match {
      case Some("id")        => if (asc) q.sortBy(_.id.asc)        else q.sortBy(_.id.desc)
      case Some("timestamp") => if (asc) q.sortBy(_.timestamp.asc) else q.sortBy(_.timestamp.desc)
      case Some("userId")    => if (asc) q.sortBy(_.userId.asc)    else q.sortBy(_.userId.desc)
      case _                 => q.sortBy(_.id.desc)
    }
  }
}
