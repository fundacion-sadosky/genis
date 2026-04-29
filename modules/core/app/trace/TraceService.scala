package trace

import java.util.Date
import types.SampleCode
import configdata.{CategoryAssociation, MatchingRule}
import _root_.util.PlayEnumUtils

import scala.concurrent.Future

// --- Match type enums ---

object MatchTypeInfo extends Enumeration:
  type MatchTypeInfo = Value
  val Insert, Update, Delete = Value
  implicit val enumTypeFormat: play.api.libs.json.Format[MatchTypeInfo.Value] = PlayEnumUtils.enumFormat(MatchTypeInfo)

// --- Trace types for pedigree matching ---

trait PedigreeMatchActionInfo:
  val pedigree: Long
  val analysisType: Int

case class PedigreeMatchInfo(
  matchId: String,
  pedigree: Long,
  analysisType: Int,
  matchType: MatchTypeInfo.Value,
  courtCase: Option[String] = None,
  pedigreeName: Option[String] = None
) extends TraceInfo with PedigreeMatchActionInfo:
  val matchTypeDescription = matchType match
    case MatchTypeInfo.Insert => "Generación"
    case MatchTypeInfo.Update => "Actualización"
    case MatchTypeInfo.Delete => "Baja"
  val description =
    if courtCase.isDefined && pedigreeName.isDefined then
      s"$matchTypeDescription de coincidencia con el pedigrí ${pedigreeName.get} del caso ${courtCase.get}"
    else
      s"$matchTypeDescription de coincidencia con el pedigrí $pedigree"

case class PedigreeMatchInfo2(
  matchId: String,
  pedigree: Long,
  profile: String,
  analysisType: Int,
  matchType: MatchTypeInfo.Value
) extends TraceInfo with PedigreeMatchActionInfo:
  val matchTypeDescription = matchType match
    case MatchTypeInfo.Insert => "Generación"
    case MatchTypeInfo.Update => "Actualización"
    case MatchTypeInfo.Delete => "Baja"
  val description = s"$matchTypeDescription de coincidencia con el perfil $profile."

// --- Core trace types ---

trait TraceService:
  def add(trace: Trace): Future[Unit]
  def addTracePedigree(trace: TracePedigree): Future[Unit]

@jakarta.inject.Singleton
class TraceServiceStub extends TraceService:
  override def add(trace: Trace): Future[Unit] = Future.successful(())
  override def addTracePedigree(trace: TracePedigree): Future[Unit] = Future.successful(())

case class Trace(
  globalCode: SampleCode,
  userId: String,
  date: Date,
  info: TraceInfo
)

case class TracePedigree(
  pedigree: Long,
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

case class PedigreeMatchProcessInfo(matchingRules: Seq[MatchingRule]) extends TraceInfo

case class PedigreeDiscardInfo(
  matchId: String,
  pedigree: Long,
  user: String,
  analysisType: Int,
  courtCase: Option[String] = None,
  pedigreeName: Option[String] = None
) extends TraceInfo with PedigreeMatchActionInfo

case class PedigreeConfirmInfo(
  matchId: String,
  pedigree: Long,
  user: String,
  analysisType: Int,
  courtCase: Option[String] = None,
  pedigreeName: Option[String] = None
) extends TraceInfo with PedigreeMatchActionInfo

case class PedigreeStatusChangeInfo(status: String) extends TraceInfo
case class PedigreeEditInfo(pedigreeId: Long) extends TraceInfo
case class PedigreeCopyInfo(pedigreeId: Long, name: String) extends TraceInfo
case class PedigreeNewScenarioInfo(id: String, nombre: String) extends TraceInfo

case class PedigreeDiscardInfo2(
  matchId: String,
  pedigree: Long,
  profile: String,
  user: String,
  analysisType: Int
) extends TraceInfo with PedigreeMatchActionInfo

case class PedigreeConfirmInfo2(
  matchId: String,
  pedigree: Long,
  profile: String,
  user: String,
  analysisType: Int
) extends TraceInfo with PedigreeMatchActionInfo
