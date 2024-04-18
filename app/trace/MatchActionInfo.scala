package trace

import types.SampleCode
import util.PlayEnumUtils

object MatchTypeInfo extends Enumeration{
  type MatchTypeInfo = Value
  val Insert, Update, Delete = Value

  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(MatchTypeInfo)
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
  override val description = s"Validación del match $matchId del perfil ${profile.text} cuyo responsable es $user."
}

case class DiscardInfo(
  matchId: String,
  profile: SampleCode,
  user: String,
  analysisType: Int) extends TraceInfo with MatchActionInfo {
  override val kind = TraceType.discard
  override val description = s"Descarte del match $matchId del perfil ${profile.text} cuyo responsable es $user."
}