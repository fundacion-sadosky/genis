package types

import play.api.libs.json.*

case class StatOption(
  frequencyTable: String,
  probabilityModel: String,
  theta: Double,
  dropIn: Double,
  dropOut: Option[Double]
)

object StatOption {
  implicit val statsFormat: Format[StatOption] = Json.format[StatOption]
}
