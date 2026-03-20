package matching

import profile.Profile
import profiledata.ProfileData
import types.SampleCode

import java.util.Date
import scala.concurrent.Future

trait MatchingRepository {
  def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]]
  def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]]
  def removeMatchesByProfile(globalCode: SampleCode): Future[Either[String, String]]
  def findSuperiorProfile(globalCode: SampleCode): Future[Option[Profile]]
  def findSuperiorProfileData(globalCode: SampleCode): Future[Option[ProfileData]]
  def numberOfMatches(globalCode: String): Future[Int]
  def getByDateBetween(from: Option[Date], to: Option[Date]): Future[Seq[MatchResult]]
}

@javax.inject.Singleton
class MatchingRepositoryStub extends MatchingRepository {
  override def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]] = Future.successful(Seq.empty)
  override def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]] = Future.successful(Seq.empty)
  override def removeMatchesByProfile(globalCode: SampleCode): Future[Either[String, String]] = Future.successful(Right(""))
  override def findSuperiorProfile(globalCode: SampleCode): Future[Option[Profile]] = Future.successful(None)
  override def findSuperiorProfileData(globalCode: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def numberOfMatches(globalCode: String): Future[Int] = Future.successful(0)
  override def getByDateBetween(from: Option[Date], to: Option[Date]): Future[Seq[MatchResult]] = Future.successful(Seq.empty)
}
