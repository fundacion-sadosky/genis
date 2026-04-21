package pedigree

import types.SampleCode

import scala.concurrent.Future

trait PedigreeService {
  def getAllCourtCases(search: PedigreeSearch): Future[Seq[CourtCaseSummary]]
  // TODO: migrate pedigree — used by ProfileDataService.canDeleteProfile
  def getTotalProfilesPedigreeMatches(sampleCode: SampleCode): Future[Int]
  def getTotalProfileNumberOfMatches(sampleCode: SampleCode): Future[Int]
  def getTotalProfilesOccurenceInCase(sampleCode: SampleCode): Future[Int]
  def countPendingScenariosByProfile(profileCode: String): Future[Int]
  def countActivePedigreesByProfile(profileCode: String): Future[Int]
}

@javax.inject.Singleton
class PedigreeServiceStub extends PedigreeService {
  override def getAllCourtCases(search: PedigreeSearch): Future[Seq[CourtCaseSummary]] = Future.successful(Seq.empty)
  override def getTotalProfilesPedigreeMatches(sampleCode: SampleCode): Future[Int] = Future.successful(0)
  override def getTotalProfileNumberOfMatches(sampleCode: SampleCode): Future[Int] = Future.successful(0)
  override def getTotalProfilesOccurenceInCase(sampleCode: SampleCode): Future[Int] = Future.successful(0)
  override def countPendingScenariosByProfile(profileCode: String): Future[Int] = Future.successful(0)
  override def countActivePedigreesByProfile(profileCode: String): Future[Int] = Future.successful(0)
}

case class PedigreeSearch(
  page: Int,
  pageSize: Int,
  input: String,
  isOwnCases: Boolean,
  idCourtCase: Option[Long],
  profile: Option[String]
)

case class CourtCaseSummary(
  id: Long,
  internalSampleCode: String,
  status: PedigreeStatus.Value
)

object PedigreeStatus extends Enumeration {
  val UnderConstruction, Active, Validated, Closed, Deleted = Value
}
