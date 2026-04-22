package matching

import play.api.libs.json.Json
import profile.Profile
import types.SampleCode

import scala.concurrent.Future

case class MatchResultScreening(globalCode: String, matchId: String, deleteable: Boolean = true)
object MatchResultScreening:
  implicit val format: play.api.libs.json.OFormat[MatchResultScreening] = Json.format[MatchResultScreening]

trait MatchingService:
  def findMatches(globalCode: SampleCode, matchType: Option[String]): Unit
  def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]]
  def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]]
  def validProfilesAssociated(labels: Option[Profile.LabeledGenotypification]): Seq[String]
  def collapse(idCourtCase: Long, user: String): Unit
  def discardCollapsingByLeftAndRightProfile(globalCode: String, courtCaseId: Long): Future[Unit]
  def discardCollapsingByRightProfile(globalCode: String, courtCaseId: Long): Future[Unit]
  def findScreeningMatches(
    profile: Profile,
    queryProfiles: List[String],
    numberOfMismatches: Option[Int]
  ): Future[(Set[MatchResultScreening], Set[MatchResultScreening])]

@javax.inject.Singleton
class MatchingServiceStub extends MatchingService:
  override def findMatches(globalCode: SampleCode, matchType: Option[String]): Unit = ()
  override def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]] = Future.successful(Seq.empty)
  override def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]] = Future.successful(Seq.empty)
  override def validProfilesAssociated(labels: Option[Profile.LabeledGenotypification]): Seq[String] = Seq.empty
  override def findScreeningMatches(
    profile: Profile,
    queryProfiles: List[String],
    numberOfMismatches: Option[Int]
  ): Future[(Set[MatchResultScreening], Set[MatchResultScreening])] =
    Future.successful((Set.empty, Set.empty))
  override def collapse(idCourtCase: Long, user: String): Unit = ()
  override def discardCollapsingByLeftAndRightProfile(globalCode: String, courtCaseId: Long): Future[Unit] = Future.successful(())
  override def discardCollapsingByRightProfile(globalCode: String, courtCaseId: Long): Future[Unit] = Future.successful(())
