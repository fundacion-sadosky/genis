package matching

import play.api.libs.json.*

object MatchStatus extends Enumeration {
  type MatchStatus = Value
  val hit, discarded, pending, deleted = Value

  implicit val reads: Reads[MatchStatus] = Reads {
    case JsString(s) => values.find(_.toString == s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown MatchStatus: $s"))
    case _ => JsError("String expected")
  }
  implicit val writes: Writes[MatchStatus] = Writes(v => JsString(v.toString))
}
