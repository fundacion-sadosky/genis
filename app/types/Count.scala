package types

import play.api.libs.json.Json

case class Count(_id: String, count: Int)

object Count {
  implicit val format = Json.format[Count]
}