package connections

import play.api.libs.json.*

case class FileInterconnection(
  id: String,
  profileId: String,
  analysisId: String,
  name: Option[String],
  typeFile: String,
  content: String
)

object FileInterconnection {
  implicit val format: Format[FileInterconnection] = Json.format[FileInterconnection]
}
