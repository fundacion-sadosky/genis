package models

import play.api.libs.json._

case class User(
    username: String,
    firstName: String,
    lastName: String,
    email: String,
    role: Option[String] = None
)

object User {
  implicit val format: Format[User] = Json.format[User]
}
