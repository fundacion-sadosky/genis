package laboratories

import play.api.libs.json.Json
import types.Email

case class Laboratory(
		name: String,
		code: String,
		country: String,
		province: String,
		address: String,
		telephone: String,
		contactEmail: Email,
		dropIn: Double,
		dropOut: Double,
		instance: Option[Boolean] = None
)

object Laboratory {
  implicit val labFormat = Json.format[Laboratory]
}

case class Geneticist(
		name: String,
		laboratory: String,
    lastname: String,
    email: Email,
    telephone: String,
    id: Option[Long]
)
    
object Geneticist {
  implicit val geneticistFormat = Json.format[Geneticist]
}