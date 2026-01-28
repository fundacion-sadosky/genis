package profile

import play.api.libs.json.Json

case class Label (id:String, caption: String)

object Label {
  implicit val f = Json.format[Label]
}
