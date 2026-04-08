package trace

import play.api.libs.json.{JsError, JsString, JsSuccess, Reads, Writes}
import types.SampleCode

object MatchTypeInfo extends Enumeration {
  type MatchTypeInfo = Value
  val Insert, Update, Delete = Value

  implicit val reads: Reads[MatchTypeInfo] = Reads {
    case JsString(s) => values.find(_.toString == s).map(JsSuccess(_)).getOrElse(JsError(s"MatchTypeInfo desconocido: $s"))
    case _           => JsError("String expected")
  }
  implicit val writes: Writes[MatchTypeInfo] = Writes(v => JsString(v.toString))
}

trait MatchActionInfo {
  val profile: SampleCode
  val analysisType: Int
}

case class MatchInfo(
  matchId: String,
  profile: SampleCode,
  analysisType: Int,
  matchType: MatchTypeInfo.Value) extends TraceInfo with MatchActionInfo {
  override val kind = TraceType.`match`
  val matchTypeDescription = matchType match {
    case MatchTypeInfo.Insert => "Generación"
    case MatchTypeInfo.Update => "Actualización"
    case MatchTypeInfo.Delete => "Baja"
  }
  override val description = s"$matchTypeDescription de coincidencia $matchId con el perfil ${profile.text}."
}

case class HitInfo(
  matchId: String,
  profile: SampleCode,
  user: String,
  analysisType: Int) extends TraceInfo with MatchActionInfo {
  override val kind = TraceType.hit
  override val description = s"Validación del match $matchId con el perfil ${profile.text} realizado por el usuario $user."
}

case class DiscardInfo(
  matchId: String,
  profile: SampleCode,
  user: String,
  analysisType: Int) extends TraceInfo with MatchActionInfo {
  override val kind = TraceType.discard
  override val description = s"Descarte del match $matchId con el perfil ${profile.text} realizado por el usuario $user."
}
