package configdata

import matching.{Algorithm, Stringency}
import play.api.libs.functional.syntax._
import play.api.libs.json.{OFormat, OWrites, Reads, __}
import types.AlphanumericId

case class MatchingRule(
  `type`: Int,
  categoryRelated: AlphanumericId,
  minimumStringency: Stringency.Value,
  failOnMatch: Boolean,
  forwardToUpper: Boolean,
  matchingAlgorithm: Algorithm.Value,
  minLocusMatch: Int,
  mismatchsAllowed: Int,
  considerForN: Boolean,
  mitochondrial: Boolean = false
)

object MatchingRule {
  implicit val mrReads: Reads[MatchingRule] = (
    (__ \ "type").read[Int] and
    (__ \ "categoryRelated").read[AlphanumericId] and
    (__ \ "minimumStringency").read[Stringency.Value] and
    (__ \ "failOnMatch").read[Boolean] and
    (__ \ "forwardToUpper").read[Boolean] and
    (__ \ "matchingAlgorithm").read[Algorithm.Value] and
    (__ \ "minLocusMatch").read[Int] and
    (__ \ "mismatchsAllowed").read[Int] and
    (__ \ "considerForN").read[Boolean] and
    (__ \ "mitochondrial").read[Boolean].orElse(Reads.pure(false))
  )(MatchingRule.apply _)

  implicit val mrWrites: OWrites[MatchingRule] = (
    (__ \ "type").write[Int] and
    (__ \ "categoryRelated").write[AlphanumericId] and
    (__ \ "minimumStringency").write[Stringency.Value] and
    (__ \ "failOnMatch").write[Boolean] and
    (__ \ "forwardToUpper").write[Boolean] and
    (__ \ "matchingAlgorithm").write[Algorithm.Value] and
    (__ \ "minLocusMatch").write[Int] and
    (__ \ "mismatchsAllowed").write[Int] and
    (__ \ "considerForN").write[Boolean] and
    (__ \ "mitochondrial").write[Boolean]
  )((mr: MatchingRule) => (
    mr.`type`, mr.categoryRelated, mr.minimumStringency, mr.failOnMatch, mr.forwardToUpper,
    mr.matchingAlgorithm, mr.minLocusMatch, mr.mismatchsAllowed, mr.considerForN, mr.mitochondrial
  ))

  implicit val mrFormat: OFormat[MatchingRule] = OFormat(mrReads, mrWrites)
}
