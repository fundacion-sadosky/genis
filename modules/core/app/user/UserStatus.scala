package user

import play.api.libs.json.*

enum UserStatus:
  case active, pending, blocked, inactive, pending_reset

object UserStatus:
  given Format[UserStatus] = new Format[UserStatus]:
    def reads(json: JsValue): JsResult[UserStatus] = json match
      case JsString(s) =>
        UserStatus.values.find(_.toString == s)
          .map(JsSuccess(_))
          .getOrElse(JsError(s"Enumeration expected of type: 'UserStatus', but it does not contain the value: '$s'"))
      case _ => JsError("String value expected")
    def writes(status: UserStatus): JsValue = JsString(status.toString)
