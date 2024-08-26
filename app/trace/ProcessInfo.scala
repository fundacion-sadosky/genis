package trace

import configdata.MatchingRule

trait ProcessInfo {
  val matchingRules: Seq[MatchingRule]
}

case class MatchProcessInfo(
  matchingRules: Seq[MatchingRule]) extends TraceInfo with ProcessInfo {
  override val kind = TraceType.matchProcess
  override val description = s"Launch of the match process."
}

case class PedigreeMatchProcessInfo(
  matchingRules: Seq[MatchingRule]) extends TraceInfo with ProcessInfo {
  override val kind = TraceType.pedigreeMatchProcess
  override val description = s"Launch of the pedigree matching process."
}