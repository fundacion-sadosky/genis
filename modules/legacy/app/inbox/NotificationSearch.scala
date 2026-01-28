package inbox

import java.util.Date

import play.api.libs.json.Json

case class NotificationSearch(
   page: Int = 0,
   pageSize: Int = 30,
   user: String,
   flagged: Option[Boolean] = None,
   pending: Option[Boolean] = None,
   hourFrom: Option[Date] = None,
   hourUntil: Option[Date] = None,
   kind: Option[NotificationType.Value] = None,
   ascending: Option[Boolean] = None,
   sortField: Option[String] = None)

object NotificationSearch {
  implicit val searchFormat = Json.format[NotificationSearch]
}