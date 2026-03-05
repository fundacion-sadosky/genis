package configdata

import play.api.libs.json.{Format, Json}

case class Crime(
  id: String,
  name: String,
  description: Option[String]
)

object Crime {
  implicit val format: Format[Crime] = Json.format[Crime]
}

case class CrimeType(
  id: String,
  name: String,
  description: Option[String],
  crimes: Seq[Crime]
)

object CrimeType {
  implicit val format: Format[CrimeType] = Json.format[CrimeType]
}
