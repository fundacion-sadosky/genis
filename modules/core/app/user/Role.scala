package user

import types.Permission
import play.api.libs.json.{Format, Json}

case class Role(
  id: String,
  roleName: String,
  permissions: Set[Permission]
)

object Role:
  given Format[Role] = Json.format[Role]
