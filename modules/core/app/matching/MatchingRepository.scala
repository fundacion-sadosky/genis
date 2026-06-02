package matching

import profile.Profile
import profiledata.ProfileData
import types.SampleCode

import java.util.Date
import scala.concurrent.Future

trait MatchingRepository:
  def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]]
  def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]]
  def removeMatchesByProfile(globalCode: SampleCode): Future[Either[String, String]]
  def findSuperiorProfile(globalCode: SampleCode): Future[Option[Profile]]
  def findSuperiorProfileData(globalCode: SampleCode): Future[Option[ProfileData]]
  def numberOfMatches(globalCode: String): Future[Int]
  def getByDateBetween(from: Option[Date], to: Option[Date]): Future[Seq[MatchResult]]
  def discardScreeningMatches(matchIds: List[String]): Unit
  def matchesByGlobalCode(globalCode: SampleCode): Future[Seq[MatchResult]]
  def getByMatchingProfileId(matchingId: String, isCollapsing: Option[Boolean] = None, isScreening: Option[Boolean] = None): Future[Option[MatchResult]]
  def convertStatus(matchId: String, firingCode: SampleCode, status: String): Future[Seq[SampleCode]]
  def getGlobalMatchStatus(left: MatchStatus.Value, right: MatchStatus.Value): MatchGlobalStatus.Value
  def getMatches(search: MatchCardSearch): Future[Seq[MatchCardForense]]
  def getTotalMatches(search: MatchCardSearch): Future[Int]
  def getMatchesByGroup(search: MatchGroupSearch): Seq[MatchingResult]
  def getTotalMatchesByGroup(search: MatchGroupSearch): Int
  def numberOfMatchesHit(globalCode: String): Future[Int]
  def numberOfMatchesPending(globalCode: String): Future[Int]
  def numberOfMatchesDescarte(globalCode: String): Future[Int]
  def numberOfMatchesConflic(globalCode: String): Future[Int]
  def numberOfMt(globalCode: String): Future[Boolean]
  def getProfileLr(globalCode: SampleCode, isCollapsing: Boolean): Future[MatchCardMejorLr]
  def discardCollapsingByLeftProfile(id: String, courtCaseId: Long): Future[Unit]
  def discardCollapsingByLeftAndRightProfile(id: String, courtCaseId: Long): Future[Unit]
  def discardCollapsingByRightProfile(id: String, courtCaseId: Long): Future[Unit]
  def discardCollapsingMatches(ids: List[String], courtCaseId: Long): Future[Unit]

@javax.inject.Singleton
class MatchingRepositoryStub extends MatchingRepository:
  override def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]] = Future.successful(Seq.empty)
  override def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]] = Future.successful(Seq.empty)
  override def removeMatchesByProfile(globalCode: SampleCode): Future[Either[String, String]] = Future.successful(Right(""))
  override def findSuperiorProfile(globalCode: SampleCode): Future[Option[Profile]] = Future.successful(None)
  override def findSuperiorProfileData(globalCode: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def numberOfMatches(globalCode: String): Future[Int] = Future.successful(0)
  override def getByDateBetween(from: Option[Date], to: Option[Date]): Future[Seq[MatchResult]] = Future.successful(Seq.empty)
  override def discardScreeningMatches(matchIds: List[String]): Unit = ()
  override def matchesByGlobalCode(globalCode: SampleCode): Future[Seq[MatchResult]] = Future.successful(Seq.empty)
  override def getByMatchingProfileId(matchingId: String, isCollapsing: Option[Boolean] = None, isScreening: Option[Boolean] = None): Future[Option[MatchResult]] = Future.successful(None)
  override def convertStatus(matchId: String, firingCode: SampleCode, status: String): Future[Seq[SampleCode]] = Future.successful(Nil)
  override def getGlobalMatchStatus(left: MatchStatus.Value, right: MatchStatus.Value): MatchGlobalStatus.Value = MatchGlobalStatus.pending
  override def getMatches(search: MatchCardSearch): Future[Seq[MatchCardForense]] = Future.successful(Seq.empty)
  override def getTotalMatches(search: MatchCardSearch): Future[Int] = Future.successful(0)
  override def getMatchesByGroup(search: MatchGroupSearch): Seq[MatchingResult] = Seq.empty
  override def getTotalMatchesByGroup(search: MatchGroupSearch): Int = 0
  override def numberOfMatchesHit(globalCode: String): Future[Int] = Future.successful(0)
  override def numberOfMatchesPending(globalCode: String): Future[Int] = Future.successful(0)
  override def numberOfMatchesDescarte(globalCode: String): Future[Int] = Future.successful(0)
  override def numberOfMatchesConflic(globalCode: String): Future[Int] = Future.successful(0)
  override def numberOfMt(globalCode: String): Future[Boolean] = Future.successful(false)
  override def getProfileLr(globalCode: SampleCode, isCollapsing: Boolean): Future[MatchCardMejorLr] =
    Future.failed(new NoSuchElementException("stub"))
  override def discardCollapsingByLeftProfile(id: String, courtCaseId: Long): Future[Unit] = Future.successful(())
  override def discardCollapsingByLeftAndRightProfile(id: String, courtCaseId: Long): Future[Unit] = Future.successful(())
  override def discardCollapsingByRightProfile(id: String, courtCaseId: Long): Future[Unit] = Future.successful(())
  override def discardCollapsingMatches(ids: List[String], courtCaseId: Long): Future[Unit] = Future.successful(())
