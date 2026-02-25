package types

import play.api.libs.json.Json

case class Laboratory(
  name: String,
  code: String,
  country: String,
  province: String,
  address: String,
  telephone: String,
  contactEmail: String,
  dropIn: Double,
  dropOut: Double,
  instance: Option[Boolean] = None
)

object Laboratory {
  implicit val labFormat: play.api.libs.json.OFormat[Laboratory] = Json.format[Laboratory]
}

case class Geneticist(
  name: String,
  laboratory: String,
  lastname: String,
  email: String,
  telephone: String,
  id: Option[Long]
)

object Geneticist {
  implicit val geneticistFormat: play.api.libs.json.OFormat[Geneticist] = Json.format[Geneticist]
}
