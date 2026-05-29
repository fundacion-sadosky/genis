package unit.trace

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import configdata.{CategoryAssociation, CategoryConfiguration}
import fixtures.TraceFixtures
import trace.*
import types.{AlphanumericId, SampleCode}

/**
 * Gap G4 (#214 review): round-trip every persisted TraceInfo variant through
 * `TraceInfo.unapply` (serialize) -> `TraceInfo.apply(kind, json)` (deserialize).
 * Protects the audit/trace JSON persisted in TRACE.trace against silent drift:
 * if a Format or the kind dispatch breaks, the stored history becomes unreadable.
 * None of these variants carry a Date, so structural equality is stable.
 */
class TraceInfoSpec extends AnyWordSpec with Matchers {

  private val sc  = SampleCode("AR-B-SHDG-1234")
  private val sc2 = SampleCode("AR-B-SHDG-1235")
  private val mt  = MatchTypeInfo.Insert
  private val mr  = TraceFixtures.matchingRule
  private val cc  = CategoryConfiguration()
  private val ca  = CategoryAssociation(1, AlphanumericId("VICTIMA"), 0)

  // One representative instance per concrete TraceInfo with a registered Format.
  private val variants: Seq[TraceInfo] = Seq(
    AnalysisInfo(Seq.empty, Some("Identifiler"), Some(1), cc),
    MatchProcessInfo(Seq(mr)),
    MatchInfo("m-1", sc, 1, mt),
    HitInfo("m-1", sc, "user", 1),
    DiscardInfo("m-1", sc, "user", 1),
    AssociationInfo(sc, "user", Seq(ca)),
    ProfileDataInfo,
    ProfileInterconectionUploadInfo,
    DeleteInfo("solicitor", "motivo"),
    PedigreeMatchProcessInfo(Seq(mr)),
    PedigreeMatchInfo("m-1", 15L, 1, mt),
    PedigreeDiscardInfo("m-1", 15L, "user", 1),
    PedigreeConfirmInfo("m-1", 15L, "user", 1),
    PedigreeStatusChangeInfo("Active"),
    PedigreeCopyInfo(15L, "Copia"),
    PedigreeEditInfo(15L),
    PedigreeNewScenarioInfo("id-1", "Escenario"),
    PedigreeMatchInfo2("m-1", 15L, "AR-B-SHDG-9", 1, mt),
    PedigreeDiscardInfo2("m-1", 15L, "AR-B-SHDG-9", "user", 1),
    PedigreeConfirmInfo2("m-1", 15L, "AR-B-SHDG-9", "user", 1),
    ProfileAprovedInSuperiorInfo,
    ProfileCategoryChangeAprovedInSuperiorInfo,
    ProfileImportedFromInferiorInfo,
    ProfileRejectedInSuperiorInfo("motivo"),
    CategoryChangeRejectedInSupInfo,
    ProfileCategoryModificationInfo("A", "B", "user"),
    SuperiorInstanceCategoryModificationInfo("A", "B"),
    SuperiorCategoryChangeRejectedInfo,
    InterconnectionDeletedInInferiorInfo("solicitor, motivo", "instancia"),
    InterconnectionDeletedInSuperiorInfo("solicitor, motivo", "instancia")
  )

  "TraceInfo round-trip (unapply -> apply)" must {
    variants.foreach { info =>
      s"reconstruct ${info.getClass.getSimpleName} (kind=${info.kind}) identically" in {
        val (kind, json) = TraceInfo.unapply(info).getOrElse(
          fail(s"unapply returned None for ${info.getClass.getSimpleName}")
        )
        TraceInfo.apply(kind, json) mustBe info
      }
    }

    "have exactly 30 covered variants (guard against silent additions/removals)" in {
      variants.size mustBe 30
    }
  }
}
