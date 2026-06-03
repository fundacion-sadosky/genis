package controllers.core

import java.util.Date

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}
import play.api.libs.functional.syntax.*
import play.api.libs.json.{Format, JsError, JsPath, Json, Reads, Writes, __}
import play.api.mvc.*

import security.{AuthService, AuthorisationOperation, FullUser, RequestToken, User}
import types.{Permission, TotpToken}

case class UserPassword(userName: String, password: String, otp: TotpToken)

case class AuthenticationRequest(requestToken: RequestToken, userName: String, otp: TotpToken)

object UserPassword {

  given userPasswordReads: Reads[UserPassword] = (
    (JsPath \ "userName").read[String] and
    (JsPath \ "password").read[String] and
    (JsPath \ "otp").read[TotpToken])(UserPassword.apply)

  given userPasswordWrites: Writes[UserPassword] = (
    (JsPath \ "userName").write[String] and
    (JsPath \ "password").write[String] and
    (JsPath \ "otp").write[TotpToken])(up => (up.userName, up.password, up.otp))

  given userPasswordFormat: Format[UserPassword] = Format(userPasswordReads, userPasswordWrites)
}

object AuthenticationRequest {

  given authenticationRequestReads: Reads[AuthenticationRequest] = (
    (JsPath \ "requestToken").read[RequestToken] and
    (JsPath \ "userName").read[String] and
    (JsPath \ "otp").read[TotpToken])(AuthenticationRequest.apply)

  given authenticationRequestWrites: Writes[AuthenticationRequest] = (
    (JsPath \ "requestToken").write[RequestToken] and
    (JsPath \ "userName").write[String] and
    (JsPath \ "otp").write[TotpToken])(ar => (ar.requestToken, ar.userName, ar.otp))

  given authenticationRequestFormat: Format[AuthenticationRequest] = Format(authenticationRequestReads, authenticationRequestWrites)
}

@Singleton
class Authentication @Inject() (
    authService: AuthService,
    val controllerComponents: ControllerComponents
)(using ec: ExecutionContext) extends BaseController {

  // Mismo contrato JSON que el legacy: userDetail (sin cryptoCredentials) + credentials.
  private given userWrites: Writes[User] = (
    (JsPath \ "id").write[String] and
    (JsPath \ "firstName").write[String] and
    (JsPath \ "lastName").write[String] and
    (JsPath \ "email").write[String] and
    (JsPath \ "permissions").write[Set[Permission]] and
    (JsPath \ "superuser").write[Boolean] and
    (JsPath \ "geneMapperId").write[String]
  )(u => (u.id, u.firstName, u.lastName, u.email, u.permissions, u.superuser, u.geneMapperId))

  private given fullUserWrites: Writes[FullUser] = (
    (JsPath \ "userDetail").write[User] and
    (JsPath \ "credentials").write[security.AuthenticatedPair]
  )(fu => (fu.userDetail, fu.credentials))

  def login = Action.async(parse.json) { request =>

    val input = request.body.validate[UserPassword]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toJson(errors)).withHeaders("Date" -> new Date().toString))
      },
      userPassword => {
        val result = authService.authenticate(userPassword.userName.toLowerCase, userPassword.password, userPassword.otp)
        result.map { userOpt =>
          userOpt.fold[Result](NotFound.withHeaders("Date" -> new Date().toString)) { user =>
            // Sólo se setea la sesión X-USER cuando la autenticación fue exitosa.
            Ok(Json.toJson(user))
              .withHeaders("Date" -> new Date().toString)
              .withSession("X-USER" -> userPassword.userName.toLowerCase)
          }
        }
      })
  }

  def getSensitiveOperations() = Action {
    given staticAuthorisationOperationWrites: Writes[AuthorisationOperation] = (
      (__ \ "resource").write[String] and
      (__ \ "action").write[String])((a: AuthorisationOperation) => (a.resource, a.action))

    val l = authService.getSensitiveOperations()
    Ok(Json.toJson(l))
  }

}
