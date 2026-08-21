package kits

import play.api.libs.json.Json

case class StrKit(
  id: String,
  name: String,
  `type`: Int,
  locy_quantity: Int,
  representative_parameter: Int)

object StrKit {
  implicit val viewFormat = Json.format[StrKit]
}