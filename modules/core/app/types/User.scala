package types

import play.api.libs.json.Json

case class User(firstName: String, lastName: String, id: Long)

object User {
	implicit val userFormat: play.api.libs.json.OFormat[User] = Json.format[User]
}
