package profile

import java.util.Date

import play.api.libs.json.Json

case class ExportProfileFilters(
  user: String,
  isSuperUser: Boolean,
  internalSampleCode:Option[String],
  categoryId: Option[String] = None,
  laboratory: Option[String] = None,
  hourFrom: Option[Date] = None,
  hourUntil: Option[Date] = None
)


object ExportProfileFilters {
  implicit val searchProfile = Json.format[ExportProfileFilters]
}
