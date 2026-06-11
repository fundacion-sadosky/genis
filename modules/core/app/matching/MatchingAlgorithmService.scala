package matching

import javax.inject.{Inject, Named, Singleton}
import configdata.{MatchingRule, MtConfiguration}
import profile.{Profile, MtRCRS}
import types.SampleCode

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*

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

@Singleton
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

@Singleton
class MatchingAlgorithmServiceImpl @Inject()(
  @Named("mtConfig") mtConfig: MtConfiguration
) extends MatchingAlgorithmService {

  override def profileMatch(
    p: Profile,
    q: Profile,
    matchingRule: MatchingRule,
    id: Option[MongoId] = None,
    n: Long,
    locusRangeMap: NewMatchingResult.AlleleMatchRange = Map.empty,
    idCourtCase: Option[Long] = None
  ): Option[MatchResult] =
    MatchingAlgorithm.performMatch(
      mtConfig, p, q, matchingRule,
      MtRCRS(Map.empty), // MtRCRS not needed for autosomal; loaded separately for mt-DNA
      id, n, locusRangeMap, idCourtCase
    )
}
