package types

import play.api.libs.json._

case class StatOption(
                       frequencyTable: String,
                       probabilityModel: String,
                       theta: Double,
                       dropIn: Double,
                       dropOut: Option[Double])

object StatOption {
  implicit val statsFormat = Json.format[StatOption]
}
