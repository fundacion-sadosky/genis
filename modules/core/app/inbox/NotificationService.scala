package inbox

import java.util.Date
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Source}
import play.api.Logger
import play.api.libs.json.Json

// PedigreeMatchingInfo y PedigreeLRInfo viven en inbox.Notification.scala (con sus Format y ruteo de serialización)

trait NotificationService:
  def search(notiSearch: NotificationSearch): Future[Seq[Notification]]
  def count(notiSearch: NotificationSearch): Future[Int]
  def push(userId: String, info: NotificationInfo): Unit
  def solve(userId: String, info: NotificationInfo): Unit
  def delete(id: Long): Future[Either[String, Long]]
  def changeFlag(id: Long, flag: Boolean): Future[Either[String, Long]]
  def getNotifications(userId: String): Source[Notification, ?]
  def changePending(id: Long, pending: Boolean): Future[Either[String, Long]]

@Singleton
class NotificationServiceImpl @Inject() (
  notificationRepository: NotificationRepository
)(using ec: ExecutionContext, mat: Materializer) extends NotificationService:

  private val logger = Logger(this.getClass)

  // Reemplaza Concurrent.broadcast[Notification] de Play 2 con Pekko BroadcastHub
  private val (broadcastSink, broadcastSource) =
    MergeHub.source[Notification](perProducerBufferSize = 16)
      .toMat(BroadcastHub.sink[Notification](bufferSize = 256))(Keep.both)
      .run()

  override def push(userId: String, info: NotificationInfo): Unit =
    val n = Notification(0, userId, new Date(), None, false, true, info)
    notificationRepository.add(n).foreach {
      case Right(id) =>
        val notification = n.copy(id = id)
        logger.trace(s"notif pushed: $notification")
        Source.single(notification).runWith(broadcastSink)
      case Left(msg) =>
        throw new RuntimeException(msg)
    }

  override def solve(userId: String, info: NotificationInfo): Unit =
    notificationRepository.get(userId, Json.toJson(info).toString, info.kind.toString).foreach { notis =>
      notis.foreach { original =>
        val notification = original.copy(updateDate = Some(new Date()), pending = false, info = info)
        notificationRepository.update(notification).foreach {
          case Right(_) =>
            logger.trace(s"notif solved: $notification")
            Source.single(notification).runWith(broadcastSink)
          case Left(msg) =>
            throw new RuntimeException(msg)
        }
      }
    }

  override def getNotifications(userId: String): Source[Notification, ?] =
    broadcastSource.filter(_.user == userId)

  override def search(notiSearch: NotificationSearch): Future[Seq[Notification]] =
    notificationRepository.search(notiSearch)

  override def count(notiSearch: NotificationSearch): Future[Int] =
    notificationRepository.count(notiSearch)

  override def delete(id: Long): Future[Either[String, Long]] =
    notificationRepository.getById(id).flatMap {
      case None => Future.successful(Left("No existe la notificación"))
      case Some(notification) =>
        if notification.pending then
          Source.single(notification.copy(pending = false)).runWith(broadcastSink)
        notificationRepository.delete(id)
    }

  override def changeFlag(id: Long, flag: Boolean): Future[Either[String, Long]] =
    notificationRepository.changeFlag(id, flag)

  override def changePending(id: Long, pending: Boolean): Future[Either[String, Long]] =
    notificationRepository.getById(id).flatMap {
      case None => Future.successful(Left("No existe la notificación"))
      case Some(notification) =>
        if notification.pending && !pending then
          Source.single(notification.copy(pending = false)).runWith(broadcastSink)
        else if !notification.pending && pending then
          Source.single(notification.copy(pending = true)).runWith(broadcastSink)
        notificationRepository.updatePending(id, pending)
    }
