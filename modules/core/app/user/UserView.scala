package user

import play.api.libs.json.{Format, Json}

case class UserView(
  userName: String,
  firstName: String,
  lastName: String,
  email: String,
  roles: Seq[String],
  status: UserStatus,
  geneMapperId: String,
  phone1: String,
  phone2: Option[String] = None,
  superuser: Boolean = false
)

object UserView:
  given Format[UserView] = Json.format[UserView]
