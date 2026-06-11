package inbox

import java.sql.Timestamp
import java.util.Date
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.Logger
import play.api.libs.json.Json
import slick.jdbc.PostgresProfile.api.*
import slick.jdbc.JdbcBackend.Database

import models.Tables

trait NotificationRepository:
  def add(notif: Notification): Future[Either[String, Long]]
  def get(userId: String, info: String, kind: String): Future[Seq[Notification]]
  def update(notification: Notification): Future[Either[String, Long]]
  def delete(id: Long): Future[Either[String, Long]]
  def getById(id: Long): Future[Option[Notification]]
  def search(notiSearch: NotificationSearch): Future[Seq[Notification]]
  def count(notiSearch: NotificationSearch): Future[Int]
  def changeFlag(id: Long, flag: Boolean): Future[Either[String, Long]]
  def updatePending(id: Long, pending: Boolean): Future[Either[String, Long]]

@Singleton
class SlickNotificationRepository @Inject() (db: Database)(using ec: ExecutionContext)
    extends NotificationRepository:

  private val logger        = Logger(this.getClass)
  private val notifications = Tables.Notification

  private def toTimestamp(date: Date): Timestamp           = new Timestamp(date.getTime)
  private def toDate(ts: Timestamp): Date                  = new Date(ts.getTime)

  private def entityToRow(n: Notification): Tables.NotificationRow =
    Tables.NotificationRow(
      id           = n.id,
      user         = n.user,
      kind         = n.kind.toString,
      creationDate = toTimestamp(n.creationDate),
      updateDate   = n.updateDate.map(toTimestamp),
      flagged      = n.flagged,
      pending      = n.pending,
      info         = Json.toJson(n.info).toString
    )

  private def rowToEntity(row: Tables.NotificationRow): Notification =
    val kind = NotificationType.valueOf(row.kind)
    Notification(
      id           = row.id,
      user         = row.user,
      creationDate = toDate(row.creationDate),
      updateDate   = row.updateDate.map(toDate),
      flagged      = row.flagged,
      pending      = row.pending,
      info         = NotificationInfo(kind, Json.parse(row.info))
    )

  override def add(notif: Notification): Future[Either[String, Long]] =
    val insert = (notifications returning notifications.map(_.id)) += entityToRow(notif)
    db.run(insert).map(Right(_)).recover { case e: Exception =>
      logger.error(s"Error al agregar notificación: ${e.getMessage}", e)
      Left(e.getMessage)
    }

  override def get(userId: String, info: String, kind: String): Future[Seq[Notification]] =
    val q = notifications.filter(n =>
      n.pending && n.user === userId && n.info === info && n.kind === kind
    )
    db.run(q.result).map(_.map(rowToEntity))

  override def update(notification: Notification): Future[Either[String, Long]] =
    val q = notifications.filter(_.id === notification.id).update(entityToRow(notification))
    db.run(q).map(_ => Right(notification.id)).recover { case e: Exception =>
      logger.error(s"Error al actualizar notificación ${notification.id}: ${e.getMessage}", e)
      Left(e.getMessage)
    }

  override def delete(id: Long): Future[Either[String, Long]] =
    val q = notifications.filter(_.id === id).delete
    db.run(q).map(_ => Right(id)).recover { case e: Exception =>
      logger.error(s"Error al eliminar notificación $id: ${e.getMessage}", e)
      Left(e.getMessage)
    }

  override def getById(id: Long): Future[Option[Notification]] =
    db.run(notifications.filter(_.id === id).result.headOption).map(_.map(rowToEntity))

  override def count(notiSearch: NotificationSearch): Future[Int] =
    db.run(buildQuery(notiSearch).length.result)

  override def search(notiSearch: NotificationSearch): Future[Seq[Notification]] =
    val q = sortQuery(buildQuery(notiSearch), notiSearch)
      .drop(notiSearch.page * notiSearch.pageSize)
      .take(notiSearch.pageSize)
    db.run(q.result).map(_.map(rowToEntity))

  override def changeFlag(id: Long, flag: Boolean): Future[Either[String, Long]] =
    val q = notifications.filter(_.id === id).map(_.flagged).update(flag)
    db.run(q).map(_ => Right(id)).recover { case e: Exception => Left(e.getMessage) }

  override def updatePending(id: Long, pending: Boolean): Future[Either[String, Long]] =
    val q = notifications.filter(_.id === id).map(_.pending).update(pending)
    db.run(q).map(_ => Right(id)).recover { case e: Exception => Left(e.getMessage) }

  private def buildQuery(s: NotificationSearch) =
    val base      = notifications.filter(_.user === s.user)
    val withKind  = s.kind.fold(base)(k => base.filter(_.kind === k.toString))
    val withFlag  = s.flagged.fold(withKind)(f => withKind.filter(_.flagged === f))
    val withPend  = s.pending.fold(withFlag)(p => withFlag.filter(_.pending === p))
    val withFrom  = s.hourFrom.fold(withPend)(d => withPend.filter(_.creationDate >= toTimestamp(d)))
    val withUntil = s.hourUntil.fold(withFrom)(d => withFrom.filter(_.creationDate <= toTimestamp(d)))
    withUntil

  private def sortQuery(
    q: Query[Tables.NotificationTable, Tables.NotificationRow, Seq],
    s: NotificationSearch
  ) =
    if s.ascending.getOrElse(false) then q.sortBy(_.creationDate.asc)
    else q.sortBy(_.creationDate.desc)