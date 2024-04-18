package inbox

import java.util.Date
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import play.api.Logger

import scala.concurrent.duration.DurationInt
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Concurrent, Enumeratee, Enumerator}
import play.api.libs.json.Json
import user.UserService

import scala.concurrent.Future
import scala.language.postfixOps

abstract class NotificationService {
  def search(notiSearch: NotificationSearch): Future[Seq[Notification]]
  def count(notiSearch: NotificationSearch): Future[Int]
  def push(userId: String, info: NotificationInfo)
  def solve(userId: String, info: NotificationInfo)
  def delete(id: Long): Future[Either[String, Long]]
  def changeFlag(id: Long, flag: Boolean): Future[Either[String, Long]]
  def getNotifications(userId: String): Enumerator[Notification]
}

@Singleton
class NotificationServiceImpl @Inject() (akkaSystem: ActorSystem,
                                         notificationRepository: NotificationRepository) extends NotificationService {

  private val (out, in) = Concurrent.broadcast[Notification]

  val logger = Logger(this.getClass)

  override def push(userId: String, info: NotificationInfo) = {
    val n = Notification(0, userId, new Date(), None, false, true, info)
    notificationRepository.add(n) foreach {
      case Right(id) => {
        val notification = Notification(id, n.user, n.creationDate, n.updateDate, n.flagged, n.pending, n.info)
        logger.trace(s"notif pushed: $notification")
        in.push(notification)
      }
      case Left(msg) => throw new RuntimeException(msg)
    }
  }

  override def solve(userId: String, info: NotificationInfo) = {
    notificationRepository.get(userId, Json.toJson(info).toString, info.kind.toString) map { notis =>
      if (notis.nonEmpty) {
        notis.foreach(original => {
          val notification = Notification(original.id, userId, original.creationDate, Some(new Date()), original.flagged, false, info)
          notificationRepository.update(notification) foreach {
            case Right(id) => {
              logger.trace(s"notif solved: $notification")
              in.push(notification)
            }
            case Left(msg) => throw new RuntimeException(msg)
          }
        })
      }
    }
  }

  private def filter(user: String) = Enumeratee.filter[Notification] { _.user == user }

  override def getNotifications(userId: String): Enumerator[Notification] = {
    out &> filter(userId)
  }

  override def search(notiSearch: NotificationSearch): Future[Seq[Notification]] = {
    notificationRepository.search(notiSearch)
  }

  override def count(notiSearch: NotificationSearch): Future[Int] = {
    notificationRepository.count(notiSearch)
  }

  override def delete(id: Long): Future[Either[String, Long]] = {
    notificationRepository.getById(id).flatMap{
      case None => {Future.successful(Left("No existe la notificación"))}
      case Some(notification) => {
        if(notification.pending){
          in.push(notification.copy(pending = false))
        }
        notificationRepository.delete(id)
      }
    }
  }

  override def changeFlag(id: Long, flag: Boolean): Future[Either[String, Long]] = {
    notificationRepository.changeFlag(id, flag)
  }

}
