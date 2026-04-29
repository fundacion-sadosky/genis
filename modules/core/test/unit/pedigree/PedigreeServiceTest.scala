package unit.pedigree

import fixtures.PedigreeFixtures._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import pedigree.*
import pedigree.PedigreeStatus.*
import trace.TraceService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class PedigreeServiceTest extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  val duration: Duration = 10.seconds

  def await[T](f: Future[T]): T = Await.result(f, duration)

  def buildService(
    dataRepo: PedigreeDataRepository = mock[PedigreeDataRepository],
    pedigreeRepo: PedigreeRepository = mock[PedigreeRepository],
    traceService: TraceService = mock[TraceService],
    profileService: profile.ProfileService = mock[profile.ProfileService],
    matchingService: matching.MatchingService = mock[matching.MatchingService],
    pedCheckService: PedCheckService = mock[PedCheckService]
  ): (PedigreeServiceImpl, PedigreeDataRepository, PedigreeRepository) = {
    val cache        = new fixtures.StubCacheService
    val fullText     = new _root_.search.FullTextSearchServiceStub
    val categoryService  = mock[configdata.CategoryService]
    val genoRepo     = mock[PedigreeGenotypificationRepository]
    val matchesRepo  = mock[PedigreeMatchesRepository]
    val scenarioRepo = mock[PedigreeScenarioRepository]
    val svc = new PedigreeServiceImpl(
      dataRepo, pedigreeRepo, cache, profileService, fullText,
      categoryService, genoRepo, matchesRepo, scenarioRepo,
      matchingService, pedCheckService, traceService
    )
    (svc, dataRepo, pedigreeRepo)
  }

  // ─── getPedigree ──────────────────────────────────────────────────────────

  "PedigreeServiceImpl.getPedigree" must {

    "return Some(PedigreeDataCreation) when pedigree exists" in {
      val (svc, dataRepo, pedigreeRepo) = buildService()
      when(dataRepo.getPedigreeMetaData(pedigreeId)).thenReturn(Future.successful(Some(pedigreeDataCreation())))
      when(pedigreeRepo.get(pedigreeId)).thenReturn(Future.successful(Some(pedigreeGenogram())))

      val result = await(svc.getPedigree(pedigreeId))

      result mustBe defined
      result.get.pedigreeMetaData.id mustBe pedigreeId
    }

    "return None when pedigree does not exist" in {
      val (svc, dataRepo, pedigreeRepo) = buildService()
      when(dataRepo.getPedigreeMetaData(pedigreeId)).thenReturn(Future.successful(None))
      when(pedigreeRepo.get(pedigreeId)).thenReturn(Future.successful(None))

      val result = await(svc.getPedigree(pedigreeId))
      result mustBe None
    }

    "return None when pedigreeId is 0" in {
      val (svc, _, _) = buildService()
      val result = await(svc.getPedigree(0L))
      result mustBe None
    }
  }

  // ─── changePedigreeStatus ─────────────────────────────────────────────────

  "PedigreeServiceImpl.changePedigreeStatus" must {

    "return Right(id) for valid transition by the assignee" in {
      val (svc, dataRepo, pedigreeRepo) = buildService()
      val ped = pedigreeDataCreation(status = PedigreeStatus.UnderConstruction, user = assignee)
      when(dataRepo.getPedigreeMetaData(pedigreeId)).thenReturn(Future.successful(Some(ped)))
      when(pedigreeRepo.changeStatus(eqTo(pedigreeId), any())).thenReturn(Future.successful(Right(pedigreeId)))
      when(dataRepo.changePedigreeStatus(eqTo(pedigreeId), any())).thenReturn(Future.successful(Right(pedigreeId)))
      val traceService = mock[TraceService]
      when(traceService.addTracePedigree(any())).thenReturn(Future.successful(()))
      val (svc2, dataRepo2, pedigreeRepo2) = buildService(dataRepo = dataRepo, pedigreeRepo = pedigreeRepo, traceService = traceService)

      val result = await(svc2.changePedigreeStatus(pedigreeId, PedigreeStatus.Active, assignee, isSuperUser = false))
      result mustBe Right(pedigreeId)
    }

    "return Left(error.E0930) for an invalid transition" in {
      val (svc, dataRepo, _) = buildService()
      val ped = pedigreeDataCreation(status = PedigreeStatus.UnderConstruction)
      when(dataRepo.getPedigreeMetaData(pedigreeId)).thenReturn(Future.successful(Some(ped)))

      val result = await(svc.changePedigreeStatus(pedigreeId, PedigreeStatus.UnderConstruction, assignee, isSuperUser = false))
      result mustBe Left("error.E0930")
    }

    "return Right(id) when called by superuser regardless of assignee" in {
      val (svc, dataRepo, pedigreeRepo) = buildService()
      val ped = pedigreeDataCreation(status = PedigreeStatus.UnderConstruction, user = "someone-else")
      when(dataRepo.getPedigreeMetaData(pedigreeId)).thenReturn(Future.successful(Some(ped)))
      when(pedigreeRepo.changeStatus(eqTo(pedigreeId), any())).thenReturn(Future.successful(Right(pedigreeId)))
      when(dataRepo.changePedigreeStatus(eqTo(pedigreeId), any())).thenReturn(Future.successful(Right(pedigreeId)))
      val traceService = mock[TraceService]
      when(traceService.addTracePedigree(any())).thenReturn(Future.successful(()))
      val (svc2, dataRepo2, pedigreeRepo2) = buildService(dataRepo = dataRepo, pedigreeRepo = pedigreeRepo, traceService = traceService)

      val result = await(svc2.changePedigreeStatus(pedigreeId, PedigreeStatus.Active, superUser, isSuperUser = true))
      result mustBe Right(pedigreeId)
    }

    "return Left(error.E0644) when user is not the assignee and not superuser" in {
      val (svc, dataRepo, _) = buildService()
      val ped = pedigreeDataCreation(user = "someone-else")
      when(dataRepo.getPedigreeMetaData(pedigreeId)).thenReturn(Future.successful(Some(ped)))

      val result = await(svc.changePedigreeStatus(pedigreeId, PedigreeStatus.Active, assignee, isSuperUser = false))
      result mustBe Left("error.E0644")
    }

    "revert mongo status if postgres update fails" in {
      val (svc, dataRepo, pedigreeRepo) = buildService()
      val originalStatus = PedigreeStatus.UnderConstruction
      val ped = pedigreeDataCreation(status = originalStatus, user = assignee)
      when(dataRepo.getPedigreeMetaData(pedigreeId)).thenReturn(Future.successful(Some(ped)))
      when(pedigreeRepo.changeStatus(eqTo(pedigreeId), any())).thenReturn(Future.successful(Right(pedigreeId)))
      when(dataRepo.changePedigreeStatus(eqTo(pedigreeId), any())).thenReturn(Future.successful(Left("db error")))
      // The revert call:
      when(pedigreeRepo.changeStatus(pedigreeId, originalStatus)).thenReturn(Future.successful(Right(pedigreeId)))
      val traceService = mock[TraceService]
      val (svc2, _, _) = buildService(dataRepo = dataRepo, pedigreeRepo = pedigreeRepo, traceService = traceService)

      val result = await(svc2.changePedigreeStatus(pedigreeId, PedigreeStatus.Active, assignee, isSuperUser = false))
      result mustBe Left("db error")
      verify(pedigreeRepo).changeStatus(pedigreeId, originalStatus)
    }
  }

  // ─── getAllCourtCases ─────────────────────────────────────────────────────

  "PedigreeServiceImpl.getAllCourtCases" must {

    "return empty Seq when profile filter matches no pedigrees" in {
      val (svc, _, pedigreeRepo) = buildService()
      when(pedigreeRepo.findByProfile("AR-B-HIBA-1-100-P")).thenReturn(Future.successful(Seq.empty))

      val search = PedigreeSearch(0, 10, "", isOwnCases = false, None, profile = Some("AR-B-HIBA-1-100-P"))
      val result = await(svc.getAllCourtCases(search))
      result mustBe empty
    }

    "return court cases when no profile filter" in {
      val (svc, dataRepo, _) = buildService()
      val mockView = CourtCaseModelView(1L, "CC-001", None, None, assignee, None, None, None, PedigreeStatus.Open, "MPI")
      when(dataRepo.getAllCourtCases(eqTo(None), any())).thenReturn(Future.successful(Seq(mockView)))
      val matchesRepo = mock[PedigreeMatchesRepository]
      when(matchesRepo.numberOfPendingMatches(any())).thenReturn(Future.successful(0))
      val (svc2, dataRepo2, _) = buildService(dataRepo = dataRepo)
      // Also stub the matchesRepo for countPendingCourCaseMatches
      // The service uses pedigreeMatchesRepository.numberOfPendingMatches — stub it on the passed-in repo
      val (svc3, dr3, _) = {
        val cache       = new fixtures.StubCacheService
        val fullText    = new _root_.search.FullTextSearchServiceStub
        val catSvc      = mock[configdata.CategoryService]
        val genoRepo    = mock[PedigreeGenotypificationRepository]
        val mr          = mock[PedigreeMatchesRepository]
        val scenarioRepo = mock[PedigreeScenarioRepository]
        val pedRepo     = mock[PedigreeRepository]
        when(mr.numberOfPendingMatches(any())).thenReturn(Future.successful(0))
        when(dataRepo.getAllCourtCases(eqTo(None), any())).thenReturn(Future.successful(Seq(mockView)))
        when(dataRepo.getPedigrees(any())).thenReturn(Future.successful(Seq.empty))
        val s = new PedigreeServiceImpl(dataRepo, pedRepo, cache, mock[profile.ProfileService], fullText,
          catSvc, genoRepo, mr, scenarioRepo, mock[matching.MatchingService], mock[PedCheckService], mock[TraceService])
        (s, dataRepo, pedRepo)
      }

      val search = PedigreeSearch(0, 10, "", isOwnCases = false, None, profile = None)
      val result = await(svc3.getAllCourtCases(search))
      result must have length 1
    }

    "call repo with pedigree ids when profile filter has matches" in {
      val (svc, dataRepo, pedigreeRepo) = buildService()
      val matchedIds = Seq(pedigreeId, 2L)
      val mockView   = CourtCaseModelView(courtCaseId, "CC-001", None, None, assignee, None, None, None, PedigreeStatus.Open, "MPI")
      when(pedigreeRepo.findByProfile("AR-B-HIBA-1-100-P")).thenReturn(Future.successful(matchedIds))
      when(dataRepo.getAllCourtCases(eqTo(Some(matchedIds)), any())).thenReturn(Future.successful(Seq(mockView)))
      val matchesRepo = mock[PedigreeMatchesRepository]
      val (svc2, _, _) = {
        val cache       = new fixtures.StubCacheService
        val fullText    = new _root_.search.FullTextSearchServiceStub
        val mr          = mock[PedigreeMatchesRepository]
        when(mr.numberOfPendingMatches(any())).thenReturn(Future.successful(0))
        when(dataRepo.getPedigrees(any())).thenReturn(Future.successful(Seq.empty))
        val s = new PedigreeServiceImpl(dataRepo, pedigreeRepo, cache, mock[profile.ProfileService], fullText,
          mock[configdata.CategoryService], mock[PedigreeGenotypificationRepository], mr,
          mock[PedigreeScenarioRepository], mock[matching.MatchingService], mock[PedCheckService], mock[TraceService])
        (s, dataRepo, pedigreeRepo)
      }

      val search = PedigreeSearch(0, 10, "", isOwnCases = false, None, profile = Some("AR-B-HIBA-1-100-P"))
      val result = await(svc2.getAllCourtCases(search))
      result must have length 1
    }
  }

  // ─── getTotalCourtCases ───────────────────────────────────────────────────

  "PedigreeServiceImpl.getTotalCourtCases" must {

    "return 0 when profile filter matches no pedigrees" in {
      val (svc, _, pedigreeRepo) = buildService()
      when(pedigreeRepo.findByProfile("AR-B-HIBA-1-100-P")).thenReturn(Future.successful(Seq.empty))

      val search = PedigreeSearch(0, 10, "", isOwnCases = false, None, profile = Some("AR-B-HIBA-1-100-P"))
      val result = await(svc.getTotalCourtCases(search))
      result mustBe 0
    }

    "return count when profile filter matches pedigrees" in {
      val (svc, dataRepo, pedigreeRepo) = buildService()
      val matchedIds = Seq(pedigreeId)
      when(pedigreeRepo.findByProfile("AR-B-HIBA-1-100-P")).thenReturn(Future.successful(matchedIds))
      when(dataRepo.getTotalCourtCases(eqTo(Some(matchedIds)), any())).thenReturn(Future.successful(5))

      val search = PedigreeSearch(0, 10, "", isOwnCases = false, None, profile = Some("AR-B-HIBA-1-100-P"))
      val result = await(svc.getTotalCourtCases(search))
      result mustBe 5
    }

    "return count directly when no profile filter" in {
      val (svc, dataRepo, _) = buildService()
      when(dataRepo.getTotalCourtCases(eqTo(None), any())).thenReturn(Future.successful(3))

      val search = PedigreeSearch(0, 10, "", isOwnCases = false, None, profile = None)
      val result = await(svc.getTotalCourtCases(search))
      result mustBe 3
    }
  }
}
