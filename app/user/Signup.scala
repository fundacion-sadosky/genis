package user

import play.api.libs.json.Json
import types.TotpToken

case class SignupSolicitude(
  val firstName: String,
  val lastName: String,
  val password: String,
  val email: String,
  val roles: Seq[String],
  val geneMapperId: String,
  val phone1: String,
  val phone2: Option[String] = None,
  val superuser: Boolean)

object SignupSolicitude {
  implicit val signupRequestReads = Json.reads[SignupSolicitude]
}

case class ClearPassSolicitud(val userName: String, val newPassword: String )

object ClearPassSolicitud {
  implicit val clearPassRequestReads = Json.reads[ClearPassSolicitud]
}

case class SignupResponse(
  val signupRequestId: String,
  val totpSecret: String,
  val userNameCandidates: Seq[String])

object SignupResponse {
  implicit val signupResponseWrites = Json.writes[SignupResponse]
}

case class DisclaimerResponse(
                           val data: String)

object DisclaimerResponse {
  implicit val disclaimerResponseWrites = Json.writes[DisclaimerResponse]
}
case class ClearPassResponse(
                              val clearPasswordRequestId: String,
                              val totpSecret: String)

object ClearPassResponse {
  implicit val clearPassResponseWrites = Json.writes[ClearPassResponse]
}

case class SignupChallenge(
  val signupRequestId: String,
  val choosenUserName: Int,
  val challengeResponse: TotpToken)

object SignupChallenge {
  implicit val signupChallengeReads = Json.reads[SignupChallenge]
}

case class ClearPassChallenge(val clearPassRequestId: String,
                              val challengeResponse: TotpToken)

object ClearPassChallenge {
  implicit val clearPassChallengeReads = Json.reads[ClearPassChallenge]
}

