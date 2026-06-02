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
  def discardCollapsingByLeftProfile(globalCode: String, courtCaseId: Long): Future[Unit]
  def findScreeningMatches(
    profile: Profile,
    queryProfiles: List[String],
    numberOfMismatches: Option[Int]
  ): Future[(Set[MatchResultScreening], Set[MatchResultScreening])]
  def findMatchingResults(globalCode: SampleCode): Future[Option[MatchingResults]]
  def getMatches(search: MatchCardSearch): Future[Seq[MatchCardForense]]
  def getTotalMatches(search: MatchCardSearch): Future[Int]
  def getMatchesByGroup(search: MatchGroupSearch): Future[Seq[MatchingResult]]
  def getTotalMatchesByGroup(search: MatchGroupSearch): Future[Int]
  def searchMatchesProfile(globalCode: String): Future[Seq[MatchCard]]
  def getByMatchedProfileId(matchingId: String, isCollapsing: Option[Boolean], isScreening: Option[Boolean]): Future[Option[play.api.libs.json.JsValue]]
  def getComparedMixtureGenotypification(globalCodes: Seq[SampleCode], matchId: String, isCollapsing: Option[Boolean], isScreening: Option[Boolean]): Future[Seq[CompareMixtureGenotypification]]
  def convertHit(matchId: String, firingCode: SampleCode, replicate: Boolean, userName: String): Future[Either[String, Seq[SampleCode]]]
  def convertDiscard(matchId: String, firingCode: SampleCode, isSuperUser: Boolean, replicate: Boolean, userName: String): Future[Either[String, Seq[SampleCode]]]
  def uploadStatus(matchId: String, firingCode: SampleCode, isSuperUser: Boolean, userName: String): Future[String]
  def canUploadMatchStatus(matchId: String, isCollapsing: Option[Boolean], isScreening: Option[Boolean]): Future[Boolean]
  def masiveGroupDiscardByGlobalCode(firingCode: SampleCode, isSuperUser: Boolean, replicate: Boolean, userName: String): Future[Either[String, Seq[SampleCode]]]
  def masiveGroupDiscardByMatchesList(firingCode: SampleCode, matchIds: List[String], isSuperUser: Boolean, replicate: Boolean, userName: String): Future[Either[String, Seq[SampleCode]]]
  def getMatchResultById(matchingId: Option[String]): Future[Option[MatchResult]]

@javax.inject.Singleton
class MatchingServiceStub extends MatchingService:
  override def findMatches(globalCode: SampleCode, matchType: Option[String]): Unit = ()
  override def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]] = Future.successful(Seq.empty)
  override def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]] = Future.successful(Seq.empty)
  override def validProfilesAssociated(labels: Option[Profile.LabeledGenotypification]): Seq[String] = Seq.empty
  override def findScreeningMatches(profile: Profile, queryProfiles: List[String], numberOfMismatches: Option[Int]): Future[(Set[MatchResultScreening], Set[MatchResultScreening])] =
    Future.successful((Set.empty, Set.empty))
  override def collapse(idCourtCase: Long, user: String): Unit = ()
  override def discardCollapsingByLeftAndRightProfile(globalCode: String, courtCaseId: Long): Future[Unit] = Future.successful(())
  override def discardCollapsingByRightProfile(globalCode: String, courtCaseId: Long): Future[Unit] = Future.successful(())
  override def discardCollapsingByLeftProfile(globalCode: String, courtCaseId: Long): Future[Unit] = Future.successful(())
  override def findMatchingResults(globalCode: SampleCode): Future[Option[MatchingResults]] = Future.successful(None)
  override def getMatches(search: MatchCardSearch): Future[Seq[MatchCardForense]] = Future.successful(Seq.empty)
  override def getTotalMatches(search: MatchCardSearch): Future[Int] = Future.successful(0)
  override def getMatchesByGroup(search: MatchGroupSearch): Future[Seq[MatchingResult]] = Future.successful(Seq.empty)
  override def getTotalMatchesByGroup(search: MatchGroupSearch): Future[Int] = Future.successful(0)
  override def searchMatchesProfile(globalCode: String): Future[Seq[MatchCard]] = Future.successful(Seq.empty)
  override def getByMatchedProfileId(matchingId: String, isCollapsing: Option[Boolean], isScreening: Option[Boolean]): Future[Option[play.api.libs.json.JsValue]] = Future.successful(None)
  override def getComparedMixtureGenotypification(globalCodes: Seq[SampleCode], matchId: String, isCollapsing: Option[Boolean], isScreening: Option[Boolean]): Future[Seq[CompareMixtureGenotypification]] = Future.successful(Seq.empty)
  override def convertHit(matchId: String, firingCode: SampleCode, replicate: Boolean, userName: String): Future[Either[String, Seq[SampleCode]]] = Future.successful(Right(Nil))
  override def convertDiscard(matchId: String, firingCode: SampleCode, isSuperUser: Boolean, replicate: Boolean, userName: String): Future[Either[String, Seq[SampleCode]]] = Future.successful(Right(Nil))
  override def uploadStatus(matchId: String, firingCode: SampleCode, isSuperUser: Boolean, userName: String): Future[String] = Future.successful("ok")
  override def canUploadMatchStatus(matchId: String, isCollapsing: Option[Boolean], isScreening: Option[Boolean]): Future[Boolean] = Future.successful(false)
  override def masiveGroupDiscardByGlobalCode(firingCode: SampleCode, isSuperUser: Boolean, replicate: Boolean, userName: String): Future[Either[String, Seq[SampleCode]]] = Future.successful(Right(Nil))
  override def masiveGroupDiscardByMatchesList(firingCode: SampleCode, matchIds: List[String], isSuperUser: Boolean, replicate: Boolean, userName: String): Future[Either[String, Seq[SampleCode]]] = Future.successful(Right(Nil))
  override def getMatchResultById(matchingId: Option[String]): Future[Option[MatchResult]] = Future.successful(None)
