package profile

import play.api.libs.json.*

case class Label(id: String, caption: String)

object Label {
  implicit val format: Format[Label] = Json.format[Label]
}
