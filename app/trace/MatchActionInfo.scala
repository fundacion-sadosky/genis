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
    case MatchTypeInfo.Insert => "Generation"
    case MatchTypeInfo.Update => "Update"
    case MatchTypeInfo.Delete => "Delete"
  }
  override val description = s"$matchTypeDescription of match $matchId with profile ${profile.text}."
}

case class HitInfo(
  matchId: String,
  profile: SampleCode,
  user: String,
  analysisType: Int) extends TraceInfo with MatchActionInfo {
  override val kind = TraceType.hit
  override val description = s"Match validation $matchId of profile ${profile.text} whose responsible is $user."
}

case class DiscardInfo(
  matchId: String,
  profile: SampleCode,
  user: String,
  analysisType: Int) extends TraceInfo with MatchActionInfo {
  override val kind = TraceType.discard
  override val description = s"Discarding the match $matchId of profile ${profile.text} whose responsible is $user."
}