package trace

import java.util.Date
import types.SampleCode
import configdata.{CategoryAssociation, MatchingRule}

import scala.concurrent.Future

trait TraceService {
  def add(trace: Trace): Future[Unit]
}

@javax.inject.Singleton
class TraceServiceStub extends TraceService {
  override def add(trace: Trace): Future[Unit] = Future.successful(())
}

case class Trace(
  globalCode: SampleCode,
  userId: String,
  date: Date,
  info: TraceInfo
)

trait TraceInfo

case class AnalysisInfo(
  loci: List[String],
  kit: Option[String],
  `type`: Option[Int],
  categoryConfiguration: configdata.CategoryConfiguration
) extends TraceInfo

case class AssociationInfo(
  associatedCode: SampleCode,
  assignee: String,
  associations: Seq[CategoryAssociation]
) extends TraceInfo
