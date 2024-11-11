package inbox

import java.sql.Timestamp
import java.text.ParseException
import javax.inject.{Inject, Singleton}

import models.Tables
import play.api.Application
import play.api.db.slick.Config.driver.simple.{ Column, Compiled, TableQuery, booleanColumnExtensionMethods, booleanColumnType, columnExtensionMethods, queryToInsertInvoker, runnableCompiledToAppliedQueryInvoker, slickDriver, stringColumnType}
import play.api.db.slick.DB
import play.api.libs.json.{JsError, Json}
import util.DefaultDb

import scala.concurrent.Future
import scala.language.postfixOps
import scala.slick.driver.PostgresDriver.simple._


abstract class NotificationRepository {
  def add(notif: Notification): Future[Either[String, Long]]
  def get(userId: String, info: String, kind: String): Future[Seq[Notification]]
  def update(notification: Notification): Future[Either[String, Long]]
  def delete(id: Long): Future[Either[String, Long]]
  def getById(id: Long): Future[Option[Notification]]
  def search(notiSearch: NotificationSearch): Future[IndexedSeq[Notification]]
  def count(notiSearch: NotificationSearch): Future[Int]
  def changeFlag(id: Long, flag: Boolean): Future[Either[String, Long]]
}

@Singleton
class SlickNotificationRepository @Inject() (implicit app: Application) extends NotificationRepository with DefaultDb {

  val notifications: TableQuery[Tables.Notification] = Tables.Notification

  implicit private def date2timestamp(date: java.util.Date) = new java.sql.Timestamp(date.getTime)

  implicit private def timestamp2date(timestamp: java.sql.Timestamp) = new java.util.Date(timestamp.getTime)

  val queryGet = Compiled { id: Column[Long] =>
    for{
      notif <- notifications if notif.id === id
    } yield notif
  }

  val queryGetFlag = Compiled { id: Column[Long] =>
    for{
      notif <- notifications if notif.id === id
    } yield (notif.id, notif.flagged)
  }

  val queryGetByUser = Compiled { userId: Column[String] =>
    for{
      notif <- notifications if notif.user === userId
    } yield notif
  }

  private def queryDefineGetByUserAndInfo(userId: Column[String], info: Column[String], kind: Column[String]) = {
    notifications.filter(notif => notif.pending && (notif.user === userId && notif.info === info) && (notif.kind === kind))
  }

  val queryGetByUserAndInfo = Compiled(queryDefineGetByUserAndInfo _)

  private def entityToRow(notif: Notification): Tables.NotificationRow = {
    Tables.NotificationRow(notif.id, notif.user, notif.kind.toString,
      date2timestamp(notif.creationDate), notif.updateDate.map(d => date2timestamp(d)),
      notif.flagged, notif.pending, Json.toJson(notif.info).toString)
  }

  private def rowToEntity(notif: Tables.NotificationRow): Notification = {
    val kind = NotificationType.withName(notif.kind.toString)
    Notification(notif.id, notif.user, notif.creationDate, notif.updateDate.map(d => timestamp2date(d)),
      notif.flagged, notif.pending, NotificationInfo(kind, Json.parse(notif.info)))
  }

  override def add(notif: Notification): Future[Either[String, Long]] = Future {
    DB.withTransaction { implicit session =>
      try {
        val notifRow = entityToRow(notif)
        Right(notifications returning notifications.map(_.id) += notifRow)
      } catch {
        case e: Exception => {
          e.printStackTrace()
          Left(e.getMessage)
        }
      }
    }
  }

  override def get(userId: String, info: String, kind: String): Future[Seq[Notification]] = Future {
    DB.withSession { implicit session =>
      queryGetByUserAndInfo(userId, info, kind).list.map(notif => rowToEntity(notif))
    }
  }

  override def update(notification: Notification): Future[Either[String, Long]] = Future {
    DB.withTransaction { implicit session =>
      try {
        queryGet(notification.id).update(entityToRow(notification))
        Right(notification.id)
      } catch {
        case e: Exception => {
          e.printStackTrace()
          Left(e.getMessage)
        }
      }
    }
  }

  override def delete(id: Long): Future[Either[String, Long]] = Future {
    DB.withTransaction { implicit session =>
      try {
        queryGet(id).delete
        Right(id)
      } catch {
        case e: Exception => {
          e.printStackTrace()
          Left(e.getMessage)
        }
      }
    }
  }

  override def getById(id: Long): Future[Option[Notification]] = Future {
    DB.withTransaction { implicit session =>
      try {
        queryGet(id).firstOption.map(notif => rowToEntity(notif)
        )

      } catch {
        case e: Exception => {
          e.printStackTrace()
          None
        }
      }
    }
  }

  override def count(notiSearch: NotificationSearch) = Future {
    DB.withSession { implicit session =>
      createSearchQuery(notiSearch).length.run
    }
  }

  override def search(notiSearch: NotificationSearch): Future[IndexedSeq[Notification]] = Future {
    DB.withSession { implicit session =>

      val searchQuery = createSearchQuery(notiSearch)
      val sortedQuery = sortQuery(searchQuery, notiSearch)

      sortedQuery.drop(notiSearch.page * notiSearch.pageSize).take(notiSearch.pageSize).iterator.toVector map {
        notif => rowToEntity(notif)
      }
    }
  }

  private def createSearchQuery(notiSearch: NotificationSearch) = {
    var query = for {
      notif <- notifications
    } yield notif

    query = query.withFilter(record => record.user === notiSearch.user)

    if (notiSearch.kind.isDefined) {
      query = query.withFilter(record => record.kind === notiSearch.kind.get.toString)
    }
    if (notiSearch.flagged.isDefined) {
      query = query.withFilter(record => record.flagged === notiSearch.flagged.get)
    }
    if (notiSearch.pending.isDefined) {
      query = query.withFilter(record => record.pending === notiSearch.pending.get)
    }
    if (notiSearch.hourFrom.isDefined) {
      val timestamp: Timestamp = notiSearch.hourFrom.get
      query = query.withFilter(record => record.creationDate >= timestamp)
    }
    if (notiSearch.hourUntil.isDefined) {
      val timestamp: Timestamp = notiSearch.hourUntil.get
      query = query.withFilter(record => record.creationDate <= timestamp)
    }

    query
  }

  private def sortQuery(query: Query[Tables.Notification, Tables.NotificationRow, scala.Seq], notiSearch: NotificationSearch) = {
    if (notiSearch.sortField.isDefined) {
      if (notiSearch.ascending.isDefined && notiSearch.ascending.get) {
        notiSearch.sortField.get match {
          case "date" => query.sortBy(_.creationDate.asc)
        }
      } else {
        notiSearch.sortField.get match {
          case "date" => query.sortBy(_.creationDate.desc)
        }
      }
    } else {
      query.sortBy(_.creationDate.desc)
    }
  }

  override def changeFlag(id: Long, flag: Boolean): Future[Either[String, Long]] = Future {
    DB.withTransaction { implicit session =>
      try {
        queryGetFlag(id).update((id, flag))
        Right(id)
      } catch {
        case e: Exception => {
          e.printStackTrace()
          Left(e.getMessage)
        }
      }
    }
  }


}
