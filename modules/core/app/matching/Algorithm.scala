package matching

import play.api.libs.json.{JsError, JsString, JsSuccess, Reads, Writes}

object Algorithm extends Enumeration {
  type Algorithm = Value
  val ENFSI, GENIS_MM = Value

  implicit val reads: Reads[Algorithm] = Reads {
    case JsString(s) => values.find(_.toString == s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown Algorithm: $s"))
    case _           => JsError("String expected")
  }
  implicit val writes: Writes[Algorithm] = Writes(v => JsString(v.toString))
}
