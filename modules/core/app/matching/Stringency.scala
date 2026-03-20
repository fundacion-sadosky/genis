package matching

import play.api.libs.json.{JsError, JsString, JsSuccess, Reads, Writes}

object Stringency extends Enumeration {
  type Stringency = Value
  val ImpossibleMatch, HighStringency, ModerateStringency, LowStringency, Mismatch, NoMatch = Value

  implicit val reads: Reads[Stringency] = Reads {
    case JsString(s) => values.find(_.toString == s).map(JsSuccess(_)).getOrElse(JsError(s"Unknown Stringency: $s"))
    case _           => JsError("String expected")
  }
  implicit val writes: Writes[Stringency] = Writes(v => JsString(v.toString))
}
