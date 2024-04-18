package user

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsPath
import play.api.libs.json.Json
import play.api.libs.json.Writes
import security.AuthenticatedPair
import types.Permission
import user.UserStatus.UserStatus

case class LdapUser(
    val userName: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val roles: Seq[String],
    val geneMapperId: String,
    val phone1: String,
    val phone2: Option[String] = None,
    val status: UserStatus = UserStatus.pending,
    val encryptedPublicKey: Array[Byte],
    val encryptedPrivateKey: Array[Byte],
    val encryptrdTotpSecret: Array[Byte],
    val superuser: Boolean = false) {

  val fullName = lastName + " " + firstName

}

object LdapUser {
  val toUserView = (user: LdapUser) =>
    UserView(
      user.userName,
      user.firstName,
      user.lastName,
      user.email,
      user.roles,
      user.status,
      user.geneMapperId,
      user.phone1,
      user.phone2,
      user.superuser)

  val fromUserView = (user: UserView) =>
    LdapUser(user.userName,
      user.firstName,
      user.lastName,
      user.email,
      user.roles,
      user.geneMapperId,
      user.phone1,
      user.phone2,
      user.status,
      null,
      null,
      null,
      user.superuser)

  val toUser = (user: LdapUser, mapRolePermission: Map[String, Set[Permission]]) => {

    val permissions = (user.roles flatMap { role =>
      mapRolePermission(role)
    }).toSet

    User(
      user.userName,
      user.firstName,
      user.lastName,
      user.email,
      user.geneMapperId,
      user.roles,
      permissions,
      user.status,
      user.phone1,
      user.phone2,
      user.superuser)
  }

}

case class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val geneMapperId: String,
    val roles: Seq[String],
    val permissions: Set[Permission],
    val status: UserStatus,
    val phone1: String,
    val phone2: Option[String] = None,
    val superuser: Boolean = false) {
  val fullName = lastName + " " + firstName
  val canLogin = status == UserStatus.active
  val isAsignable = status == UserStatus.active || status == UserStatus.blocked
}

object User {

  import play.api.libs.json.Json
  import play.api.data._
  import play.api.data.Forms._

  implicit val userWrites: Writes[User] = (
    (JsPath \ "id").write[String] ~
    (JsPath \ "firstName").write[String] ~
    (JsPath \ "lastName").write[String] ~
    (JsPath \ "email").write[String] ~
    (JsPath \ "permissions").write[Set[Permission]] ~
    (JsPath \ "superuser").write[Boolean] ~
    (JsPath \ "geneMapperId").write[String])((user: User) => (
      user.id,
      user.firstName,
      user.lastName,
      user.email,
      user.permissions,
      user.superuser,
      user.geneMapperId))
}

case class FullUser(
  userDetail: User,
  cryptoCredentials: UserCredentials,
  credentials: AuthenticatedPair)

object FullUser {

  implicit val fullUserWrites: Writes[FullUser] = (
    (JsPath \ "userDetail").write[User] ~
    (JsPath \ "credentials").write[AuthenticatedPair])((fullUser: FullUser) => (
      fullUser.userDetail,
      fullUser.credentials))
}

case class UserView(
  val userName: String,
  val firstName: String,
  val lastName: String,
  val email: String,
  val roles: Seq[String],
  val status: UserStatus,
  val geneMapperId: String,
  val phone1: String,
  val phone2: Option[String] = None,
  val superuser: Boolean = false)

object UserView {
  implicit val userViewFormat = Json.format[UserView]
}

case class UserCredentials(
  val publicKey: Array[Byte],
  val privateKey: Array[Byte],
  val totpSecret: String)

object UserCredentials {
  type SignupCredentials = UserCredentials
}
