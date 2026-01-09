package repositories

import models.notification.{Notification, NotificationSearch, NotificationType}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime
import play.api.libs.json.JsObject

@Singleton
class NotificationRepository @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {
  
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig.profile.api._
  import dbConfig.db

  private class NotificationsTable(tag: Tag) 
    extends Table[Notification](tag, "notifications") {
    
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[String]("user_id")
    def createdAt = column[LocalDateTime]("created_at")
    def updatedAt = column[Option[LocalDateTime]]("updated_at")
    def flagged = column[Boolean]("flagged")
    def pending = column[Boolean]("pending")
    def notificationType = column[String]("notification_type")
    def title = column[String]("title")
    def description = column[String]("description")
    def url = column[String]("url")
    def metadata = column[Option[String]]("metadata")

    def * = (id, userId, createdAt, updatedAt, flagged, pending, notificationType, 
             title, description, url, metadata) <> 
             ((n) => Notification(
               n._1, n._2, n._3, n._4, n._5, n._6, 
               NotificationType.withName(n._7), n._8, n._9, n._10, n._11
             ),
              (n: Notification) => Some((
                n.id, n.userId, n.createdAt, n.updatedAt, n.flagged, n.pending, 
                n.notificationType.toString, n.title, n.description, n.url, n.metadata
              )))
  }

  private val notifications = TableQuery[NotificationsTable]

  def create(notification: Notification): Future[Notification] = db.run {
    (notifications returning notifications.map(_.id) 
      into ((notif, id) => notif.copy(id = id))) += notification
  }

  def findById(id: Long): Future[Option[Notification]] = db.run {
    notifications.filter(_.id === id).result.headOption
  }

  def findByUserId(userId: String): Future[Seq[Notification]] = db.run {
    notifications
      .filter(_.userId === userId)
      .sortBy(_.createdAt.desc)
      .result
  }

  def search(search: NotificationSearch): Future[Seq[Notification]] = db.run {
    var query = notifications.asInstanceOf[Query[NotificationsTable, Notification, Seq]]
    
    if (search.userId.isDefined) {
      query = query.filter(_.userId === search.userId.get)
    }
    if (search.notificationType.isDefined) {
      query = query.filter(_.notificationType === search.notificationType.get.toString)
    }
    if (search.pending.isDefined) {
      query = query.filter(_.pending === search.pending.get)
    }
    if (search.flagged.isDefined) {
      query = query.filter(_.flagged === search.flagged.get)
    }

    query
      .sortBy(_.createdAt.desc)
      .drop(search.page * search.pageSize)
      .take(search.pageSize)
      .result
  }

  def countPending(userId: String): Future[Int] = db.run {
    notifications
      .filter(_.userId === userId)
      .filter(_.pending === true)
      .length
      .result
  }

  def countSearch(search: NotificationSearch): Future[Int] = db.run {
    var query = notifications.asInstanceOf[Query[NotificationsTable, Notification, Seq]]
    
    if (search.userId.isDefined) {
      query = query.filter(_.userId === search.userId.get)
    }
    if (search.notificationType.isDefined) {
      query = query.filter(_.notificationType === search.notificationType.get.toString)
    }
    if (search.pending.isDefined) {
      query = query.filter(_.pending === search.pending.get)
    }
    if (search.flagged.isDefined) {
      query = query.filter(_.flagged === search.flagged.get)
    }

    query.length.result
  }

  def updateFlags(id: Long, flagged: Boolean, pending: Boolean): Future[Int] = db.run {
    notifications
      .filter(_.id === id)
      .map(n => (n.flagged, n.pending, n.updatedAt))
      .update((flagged, pending, Some(LocalDateTime.now())))
  }

  def delete(id: Long): Future[Int] = db.run {
    notifications.filter(_.id === id).delete
  }

  def deleteByUserId(userId: String): Future[Int] = db.run {
    notifications.filter(_.userId === userId).delete
  }
}
