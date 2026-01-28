package controllers

import java.util.Date

import scala.concurrent.Future

import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.Format
import play.api.libs.json.JsError
import play.api.libs.json.JsPath
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Reads.StringReads
import play.api.libs.json.Reads.functorReads
import play.api.libs.json.Writes
import play.api.libs.json.__
import play.api.mvc.Action
import play.api.mvc.BodyParsers
import play.api.mvc.Controller
import play.api.mvc.Result
import play.api.mvc.Results
import security.AuthService
import security.AuthorisationOperation
import security.RequestToken
import types.TotpToken

case class UserPassword(userName: String, password: String, otp: TotpToken)

case class AuthenticationRequest(requestToken: RequestToken, userName: String, otp: TotpToken)

object UserPassword {

  implicit val userPasswordReads: Reads[UserPassword] = (
    (JsPath \ "userName").read[String] and
    (JsPath \ "password").read[String] and
    (JsPath \ "otp").read[TotpToken])(UserPassword.apply _)

  implicit val userPasswordWrites: Writes[UserPassword] = (
    (JsPath \ "userName").write[String] ~
    (JsPath \ "password").write[String] ~
    (JsPath \ "otp").write[TotpToken])(unlift(UserPassword.unapply))

  implicit val userPasswordFormat: Format[UserPassword] = Format(userPasswordReads, userPasswordWrites)
}

object AuthenticationRequest {

  implicit val authenticationRequestReads: Reads[AuthenticationRequest] = (
    (JsPath \ "requestToken").read[RequestToken] and
    (JsPath \ "userName").read[String] and
    (JsPath \ "otp").read[TotpToken])(AuthenticationRequest.apply _)

  implicit val authenticationRequestWrites: Writes[AuthenticationRequest] = (
    (JsPath \ "requestToken").write[RequestToken] and
    (JsPath \ "userName").write[String] and
    (JsPath \ "otp").write[TotpToken])(unlift(AuthenticationRequest.unapply))

  implicit val authenticationRequestFormat: Format[AuthenticationRequest] = Format(authenticationRequestReads, authenticationRequestWrites)
}

@Singleton
class Authentication @Inject() (authService: AuthService) extends Controller with JsonActions {

  def login = Action.async(BodyParsers.parse.json) { request =>

    val input = request.body.validate[UserPassword]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)).withHeaders("Date" -> (new Date()).toString()))
      },
      userPassword => {
        val result = authService.authenticate(userPassword.userName.toLowerCase, userPassword.password, userPassword.otp)
        result map { userOpt =>
          val response = userOpt.fold[Result]({
            Results.NotFound
          })({ user =>
            Ok(Json.toJson(user))
          })

          response
            .withHeaders("Date" -> (new Date()).toString())
            .withSession(("X-USER", userPassword.userName.toLowerCase))
        }
      })
  }

  def getSensitiveOperations() = Action {

    implicit val staticAuthorisationOperationWrites: Writes[AuthorisationOperation] = (
      (__ \ "resource").write[String] ~
      (__ \ "action").write[String])((a: AuthorisationOperation) => (a.resource, a.action))

    val l = authService.getSensitiveOperations()
    Ok(Json.toJson(l))
  }

}
