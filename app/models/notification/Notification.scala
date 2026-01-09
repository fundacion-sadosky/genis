package models.notification

import java.time.LocalDateTime
import play.api.libs.json._

case class Notification(
  id: Long,
  userId: String,
  createdAt: LocalDateTime,
  updatedAt: Option[LocalDateTime],
  flagged: Boolean,
  pending: Boolean,
  notificationType: NotificationType.Value,
  title: String,
  description: String,
  url: String,
  metadata: Option[String]  // JSON como String
)

object Notification {
  implicit val writes: Writes[Notification] = Json.writes[Notification]
  implicit val reads: Reads[Notification] = Json.reads[Notification]
}

case class NotificationSearch(
  userId: Option[String] = None,
  notificationType: Option[NotificationType.Value] = None,
  pending: Option[Boolean] = None,
  flagged: Option[Boolean] = None,
  page: Int = 0,
  pageSize: Int = 25
)
