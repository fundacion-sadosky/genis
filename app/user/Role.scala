package user

import types.Permission
import play.api.libs.json.Json

case class Role(
  val id: String,
  val roleName: String,
  val permissions: Set[Permission])

object Role {
  implicit val f = Json.format[Role]
}
