package matching

import configdata.MatchingRule
import profile.Profile
import types.SampleCode

import scala.concurrent.Future

trait MatchingAlgorithmService {
  def profileMatch(
    p: Profile,
    q: Profile,
    matchingRule: MatchingRule,
    id: Option[MongoId] = None,
    n: Long,
    locusRangeMap: NewMatchingResult.AlleleMatchRange = Map.empty,
    idCourtCase: Option[Long] = None
  ): Option[MatchResult]
}

@javax.inject.Singleton
class MatchingAlgorithmServiceStub extends MatchingAlgorithmService {
  override def profileMatch(
    p: Profile,
    q: Profile,
    matchingRule: MatchingRule,
    id: Option[MongoId],
    n: Long,
    locusRangeMap: NewMatchingResult.AlleleMatchRange,
    idCourtCase: Option[Long]
  ): Option[MatchResult] = None
}
