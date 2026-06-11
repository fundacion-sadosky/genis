package inbox

import scala.concurrent.Future
import org.apache.pekko.stream.scaladsl.Source

/** No-op stub for unit tests that inject NotificationService.
 *  Declared in package `inbox` so it resolves as `inbox.NoOpNotificationService`.
 *  All methods return empty / successful no-op results.
 */
class NoOpNotificationService extends NotificationService:
  override def search(notiSearch: NotificationSearch): Future[Seq[Notification]] =
    Future.successful(Seq.empty)
  override def count(notiSearch: NotificationSearch): Future[Int] =
    Future.successful(0)
  override def push(userId: String, info: NotificationInfo): Unit = ()
  override def solve(userId: String, info: NotificationInfo): Unit = ()
  override def delete(id: Long): Future[Either[String, Long]] =
    Future.successful(Right(id))
  override def changeFlag(id: Long, flag: Boolean): Future[Either[String, Long]] =
    Future.successful(Right(id))
  override def getNotifications(userId: String): Source[Notification, ?] =
    Source.empty
  override def changePending(id: Long, pending: Boolean): Future[Either[String, Long]] =
    Future.successful(Right(id))