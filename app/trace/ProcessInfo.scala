package trace

import configdata.MatchingRule

trait ProcessInfo {
  val matchingRules: Seq[MatchingRule]
}

case class MatchProcessInfo(
  matchingRules: Seq[MatchingRule]) extends TraceInfo with ProcessInfo {
  override val kind = TraceType.matchProcess
  override val description = s"Lanzamiento del proceso de match."
}

case class PedigreeMatchProcessInfo(
  matchingRules: Seq[MatchingRule]) extends TraceInfo with ProcessInfo {
  override val kind = TraceType.pedigreeMatchProcess
  override val description = s"Lanzamiento del proceso de match de pedigrí."
}