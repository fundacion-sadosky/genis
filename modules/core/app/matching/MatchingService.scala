package matching

import profile.Profile
import types.SampleCode

import scala.concurrent.Future

case class MatchingResultMin(internalSampleCode: String)
case class MatchingResults(matchingId: String, searchedProfileGlobalCode: SampleCode, userId: String, results: List[MatchingResultMin])

trait MatchingService {
  def findMatches(globalCode: SampleCode, matchType: Option[String]): Unit
  def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]]
  def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]]
  def validProfilesAssociated(labels: Option[Profile.LabeledGenotypification]): Seq[String]
  // TODO: migrate matching — full implementation in MatchingServiceSpark
  def findMatchingResults(globalCode: SampleCode): Future[Option[MatchingResults]]
}

@javax.inject.Singleton
class MatchingServiceStub extends MatchingService {
  override def findMatches(globalCode: SampleCode, matchType: Option[String]): Unit = ()
  override def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]] = Future.successful(Seq.empty)
  override def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]] = Future.successful(Seq.empty)
  override def validProfilesAssociated(labels: Option[Profile.LabeledGenotypification]): Seq[String] = Seq.empty
  override def findMatchingResults(globalCode: SampleCode): Future[Option[MatchingResults]] = Future.successful(None)
}
