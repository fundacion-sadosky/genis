package profile

import java.util.Date
import play.api.libs.json.*

case class ExportLimsFilesFilter(
  user: String,
  isSuperUser: Boolean,
  tipo: String,
  hourFrom: Option[Date] = None,
  hourUntil: Option[Date] = None
)

object ExportLimsFilesFilter {
  implicit val format: Format[ExportLimsFilesFilter] = Json.format[ExportLimsFilesFilter]
}
