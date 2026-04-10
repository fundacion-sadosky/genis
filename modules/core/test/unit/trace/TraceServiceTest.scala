package unit.trace

import configdata.{CategoryAssociation, CategoryConfiguration, CategoryService}
import fixtures.{CategoryFixtures, TraceFixtures}
import kits.{AnalysisType, AnalysisTypeService}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import pedigree.{PedigreeDataCreation, PedigreeDataRepository, PedigreeMetaData}
import play.api.i18n.DefaultMessagesApi
import profile.ProfileRepository
import trace.*
import types.{AlphanumericId, SampleCode}

import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

class TraceServiceTest extends PlaySpec with MockitoSugar:

  import TraceFixtures.*

  private val duration    = Duration(10, SECONDS)
  private val messagesApi = new DefaultMessagesApi()

  private def makeService(
    repo:       TraceRepository      = mock[TraceRepository],
    catSvc:     CategoryService      = null,
    atSvc:      AnalysisTypeService  = null,
    profileRepo: ProfileRepository   = null,
    pedigreeRepo: PedigreeDataRepository = null
  ): TraceServiceImpl =
    new TraceServiceImpl(repo, catSvc, atSvc, profileRepo, pedigreeRepo, messagesApi)

  // ─── search ──────────────────────────────────────────────────────────────

  "TraceServiceImpl.search" should:
    "delegate to repository and return results" in:
      val repo = mock[TraceRepository]
      when(repo.search(any[TraceSearch])).thenReturn(Future.successful(Seq(hitTrace)))
      val result = Await.result(makeService(repo).search(traceSearch), duration)
      result mustBe Seq(hitTrace)

  // ─── count ───────────────────────────────────────────────────────────────

  "TraceServiceImpl.count" should:
    "delegate to repository and return total" in:
      val repo = mock[TraceRepository]
      when(repo.count(any[TraceSearch])).thenReturn(Future.successful(5))
      val result = Await.result(makeService(repo).count(traceSearch), duration)
      result mustBe 5

  // ─── add ─────────────────────────────────────────────────────────────────

  "TraceServiceImpl.add" should:
    "return Right(id) on success" in:
      val repo = mock[TraceRepository]
      when(repo.add(any[Trace])).thenReturn(Future.successful(Right(42L)))
      val result = Await.result(makeService(repo).add(hitTrace), duration)
      result mustBe Right(42L)

    "return Left on repository exception" in:
      val repo = mock[TraceRepository]
      when(repo.add(any[Trace])).thenReturn(Future.failed(new RuntimeException("DB error")))
      val result = Await.result(makeService(repo).add(hitTrace), duration)
      result.isLeft mustBe true

  // ─── searchPedigree / countPedigree ──────────────────────────────────────

  "TraceServiceImpl.searchPedigree" should:
    "delegate to repository and return results" in:
      val repo = mock[TraceRepository]
      when(repo.searchPedigree(any[TraceSearchPedigree])).thenReturn(Future.successful(Seq(pedigreeTrace)))
      val result = Await.result(makeService(repo).searchPedigree(traceSearchPedigree), duration)
      result mustBe Seq(pedigreeTrace)

  "TraceServiceImpl.countPedigree" should:
    "delegate to repository and return total" in:
      val repo = mock[TraceRepository]
      when(repo.countPedigree(any[TraceSearchPedigree])).thenReturn(Future.successful(3))
      val result = Await.result(makeService(repo).countPedigree(traceSearchPedigree), duration)
      result mustBe 3

  // ─── addTracePedigree ────────────────────────────────────────────────────

  "TraceServiceImpl.addTracePedigree" should:
    "return Right(id) on success" in:
      val repo = mock[TraceRepository]
      when(repo.addTracePedigree(any[TracePedigree])).thenReturn(Future.successful(Right(99L)))
      val result = Await.result(makeService(repo).addTracePedigree(pedigreeTrace), duration)
      result mustBe Right(99L)

    "return Left on repository exception" in:
      val repo = mock[TraceRepository]
      when(repo.addTracePedigree(any[TracePedigree])).thenReturn(Future.failed(new RuntimeException("DB error")))
      val result = Await.result(makeService(repo).addTracePedigree(pedigreeTrace), duration)
      result.isLeft mustBe true

  // ─── getFullDescription ──────────────────────────────────────────────────

  "TraceServiceImpl.getFullDescription" should:

    "return non-empty string for AnalysisInfo" in:
      val atSvc = mock[AnalysisTypeService]
      when(atSvc.getById(any[Int])).thenReturn(Future.successful(Some(analysisType)))
      val repo  = mock[TraceRepository]
      val trace = Trace(sampleCode, "userId", new Date(),
        AnalysisInfo(Seq.empty, None, Some(1), CategoryConfiguration()))
      when(repo.getById(any[Long])).thenReturn(Future.successful(Some(trace)))
      val result = Await.result(makeService(repo, atSvc = atSvc).getFullDescription(1L), duration)
      result.length must be > 0
      verify(atSvc).getById(1)

    "return non-empty string for MatchProcessInfo" in:
      val atSvc  = mock[AnalysisTypeService]
      when(atSvc.getById(any[Int])).thenReturn(Future.successful(Some(analysisType)))
      val catSvc = mock[CategoryService]
      when(catSvc.getCategory(any[AlphanumericId])).thenReturn(Future.successful(Some(CategoryFixtures.fcA)))
      val repo   = mock[TraceRepository]
      val trace  = Trace(sampleCode, "userId", new Date(), MatchProcessInfo(Seq(matchingRule)))
      when(repo.getById(any[Long])).thenReturn(Future.successful(Some(trace)))
      val result = Await.result(makeService(repo, catSvc, atSvc).getFullDescription(1L), duration)
      result.length must be > 0
      verify(atSvc).getById(matchingRule.`type`)
      verify(catSvc).getCategory(matchingRule.categoryRelated)

    "return non-empty string for PedigreeMatchProcessInfo" in:
      val atSvc  = mock[AnalysisTypeService]
      when(atSvc.getById(any[Int])).thenReturn(Future.successful(Some(analysisType)))
      val catSvc = mock[CategoryService]
      when(catSvc.getCategory(any[AlphanumericId])).thenReturn(Future.successful(Some(CategoryFixtures.fcA)))
      val repo   = mock[TraceRepository]
      val trace  = Trace(sampleCode, "userId", new Date(), PedigreeMatchProcessInfo(Seq(matchingRule)))
      when(repo.getById(any[Long])).thenReturn(Future.successful(Some(trace)))
      val result = Await.result(makeService(repo, catSvc, atSvc).getFullDescription(1L), duration)
      result.length must be > 0

    "return non-empty string for HitInfo (MatchActionInfo)" in:
      val atSvc      = mock[AnalysisTypeService]
      when(atSvc.getById(any[Int])).thenReturn(Future.successful(Some(analysisType)))
      val catSvc     = mock[CategoryService]
      when(catSvc.getCategory(any[AlphanumericId])).thenReturn(Future.successful(Some(CategoryFixtures.fcA)))
      val profileRepo = mock[ProfileRepository]
      when(profileRepo.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(sampleProfile)))
      val repo       = mock[TraceRepository]
      when(repo.getById(any[Long])).thenReturn(Future.successful(Some(hitTrace)))
      val result = Await.result(makeService(repo, catSvc, atSvc, profileRepo).getFullDescription(1L), duration)
      result.length must be > 0
      verify(atSvc).getById(hitInfo.analysisType)
      verify(catSvc).getCategory(sampleProfile.categoryId)
      verify(profileRepo).findByCode(hitInfo.profile)

    "return non-empty string for PedigreeDiscardInfo (PedigreeMatchActionInfo) with stub pedigree" in:
      val atSvc       = mock[AnalysisTypeService]
      when(atSvc.getById(any[Int])).thenReturn(Future.successful(Some(analysisType)))
      val pedigreeRepo = mock[PedigreeDataRepository]
      when(pedigreeRepo.getPedigreeMetaData(any[Long]))
        .thenReturn(Future.successful(Some(pedigreeData)))
      val repo        = mock[TraceRepository]
      when(repo.getById(any[Long])).thenReturn(Future.successful(Some(
        Trace(sampleCode, "userId", new Date(), pedigreeDiscardInfo)
      )))
      val result = Await.result(makeService(repo, atSvc = atSvc, pedigreeRepo = pedigreeRepo).getFullDescription(1L), duration)
      result.length must be > 0
      verify(atSvc).getById(pedigreeDiscardInfo.analysisType)
      verify(pedigreeRepo).getPedigreeMetaData(pedigreeDiscardInfo.pedigree)

    "return partial description for PedigreeMatchActionInfo when pedigree repo returns None" in:
      val atSvc       = mock[AnalysisTypeService]
      when(atSvc.getById(any[Int])).thenReturn(Future.successful(Some(analysisType)))
      val pedigreeRepo = mock[PedigreeDataRepository]
      when(pedigreeRepo.getPedigreeMetaData(any[Long])).thenReturn(Future.successful(None))
      val repo        = mock[TraceRepository]
      when(repo.getById(any[Long])).thenReturn(Future.successful(Some(
        Trace(sampleCode, "userId", new Date(), pedigreeDiscardInfo)
      )))
      val result = Await.result(makeService(repo, atSvc = atSvc, pedigreeRepo = pedigreeRepo).getFullDescription(1L), duration)
      result must include(s"id=${pedigreeDiscardInfo.pedigree}")

    "return non-empty string for AssociationInfo" in:
      val catSvc      = mock[CategoryService]
      when(catSvc.getCategory(any[AlphanumericId])).thenReturn(Future.successful(Some(CategoryFixtures.fcA)))
      val profileRepo = mock[ProfileRepository]
      when(profileRepo.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(sampleProfile)))
      val assocInfo   = AssociationInfo(sampleCode2, "userId",
        Seq(CategoryAssociation(1, AlphanumericId("VICTIMA"), 0)))
      val repo        = mock[TraceRepository]
      when(repo.getById(any[Long])).thenReturn(Future.successful(Some(
        Trace(sampleCode, "userId", new Date(), assocInfo)
      )))
      val result = Await.result(makeService(repo, catSvc, profileRepo = profileRepo).getFullDescription(1L), duration)
      result.length must be > 0
      verify(catSvc).getCategory(AlphanumericId("VICTIMA"))
      verify(profileRepo).findByCode(sampleCode2)

    "return empty string for unhandled TraceInfo variant" in:
      val repo = mock[TraceRepository]
      when(repo.getById(any[Long])).thenReturn(Future.successful(Some(profileDataTrace)))
      val result = Await.result(makeService(repo).getFullDescription(1L), duration)
      result.length mustBe 0

    "return fixed string for PedigreeStatusChangeInfo" in:
      val repo  = mock[TraceRepository]
      val trace = Trace(sampleCode, "userId", new Date(), PedigreeStatusChangeInfo("Active"))
      when(repo.getById(any[Long])).thenReturn(Future.successful(Some(trace)))
      val result = Await.result(makeService(repo).getFullDescription(1L), duration)
      result mustBe "PedigreeStatusChange"

    "return fixed string for PedigreeCopyInfo" in:
      val repo  = mock[TraceRepository]
      val trace = Trace(sampleCode, "userId", new Date(), PedigreeCopyInfo(1L, "Copia Test"))
      when(repo.getById(any[Long])).thenReturn(Future.successful(Some(trace)))
      val result = Await.result(makeService(repo).getFullDescription(1L), duration)
      result mustBe "PedigreeCopy"

    "return fixed string for PedigreeEditInfo" in:
      val repo  = mock[TraceRepository]
      val trace = Trace(sampleCode, "userId", new Date(), PedigreeEditInfo(1L))
      when(repo.getById(any[Long])).thenReturn(Future.successful(Some(trace)))
      val result = Await.result(makeService(repo).getFullDescription(1L), duration)
      result mustBe "PedigreeEdit"

    "return fixed string for PedigreeNewScenarioInfo" in:
      val repo  = mock[TraceRepository]
      val trace = Trace(sampleCode, "userId", new Date(), PedigreeNewScenarioInfo("id-1", "Escenario A"))
      when(repo.getById(any[Long])).thenReturn(Future.successful(Some(trace)))
      val result = Await.result(makeService(repo).getFullDescription(1L), duration)
      result mustBe "PedigreeNewScenario"
