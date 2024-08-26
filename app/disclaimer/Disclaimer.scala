package disclaimer
import play.api.libs.json.{Format, Json}

case class Disclaimer(text: Option[String])

object Disclaimer {
  implicit val disclaimerFormat: Format[Disclaimer] = Json.format
}