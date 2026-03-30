package user

import play.api.libs.json.{Json, Reads, Writes}
import types.TotpToken

case class SignupSolicitude(
  firstName: String,
  lastName: String,
  password: String,
  email: String,
  roles: Seq[String],
  geneMapperId: String,
  phone1: String,
  phone2: Option[String] = None,
  superuser: Boolean
)

object SignupSolicitude:
  given Reads[SignupSolicitude] = Json.reads[SignupSolicitude]

case class ClearPassSolicitud(userName: String, newPassword: String)

object ClearPassSolicitud:
  given Reads[ClearPassSolicitud] = Json.reads[ClearPassSolicitud]

case class SignupResponse(
  signupRequestId: String,
  totpSecret: String,
  userNameCandidates: Seq[String]
)

object SignupResponse:
  given Writes[SignupResponse] = Json.writes[SignupResponse]

case class ClearPassResponse(
  clearPasswordRequestId: String,
  totpSecret: String
)

object ClearPassResponse:
  given Writes[ClearPassResponse] = Json.writes[ClearPassResponse]

case class SignupChallenge(
  signupRequestId: String,
  choosenUserName: Int,
  challengeResponse: TotpToken
)

object SignupChallenge:
  given Reads[SignupChallenge] = Json.reads[SignupChallenge]

case class ClearPassChallenge(
  clearPassRequestId: String,
  challengeResponse: TotpToken
)

object ClearPassChallenge:
  given Reads[ClearPassChallenge] = Json.reads[ClearPassChallenge]
