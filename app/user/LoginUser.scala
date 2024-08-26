package user

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class LoginUser(
  username: String,
  passwd: String,
  token: String)

object LoginUser {
  import play.api.libs.json.Json
  import play.api.data._
  import play.api.data.Forms._

  implicit val loginUserReads: Reads[LoginUser] = (
    (JsPath \ "username").read[String] ~
    (JsPath \ "passwd").read[String] ~
    (JsPath \ "token").read[String])(LoginUser.apply _)

  implicit val loginUserWrites: Writes[LoginUser] = (
    (JsPath \ "username").write[String] ~
    (JsPath \ "passwd").write[String] ~
    (JsPath \ "token").write[String])((loginUser: LoginUser) => (
      loginUser.username,
      loginUser.passwd,
      loginUser.token))

  implicit val loginUserFormat: Format[LoginUser] = Format(loginUserReads, loginUserWrites)
}
