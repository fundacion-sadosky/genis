package pedigree

import types.SampleCode

import scala.concurrent.Future

trait PedigreeService {
  def getAllCourtCases(search: PedigreeSearch): Future[Seq[CourtCaseSummary]]
}

@javax.inject.Singleton
class PedigreeServiceStub extends PedigreeService {
  override def getAllCourtCases(search: PedigreeSearch): Future[Seq[CourtCaseSummary]] = Future.successful(Seq.empty)
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
