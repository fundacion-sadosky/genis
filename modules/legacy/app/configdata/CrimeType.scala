package configdata

import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.Format
import play.api.libs.json.__
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Json

case class CrimeType(
  id: String,
  name: String,
  description: Option[String],
  crimes: Seq[Crime])

case class Crime(
  id: String,
  name: String,
  description: Option[String])

object Crime {

  implicit val crimeFormat = Json.format[Crime]
}

object CrimeType {

  implicit val crimeTypeFormat = Json.format[CrimeType]
}
