package unit.pedigree

import fixtures.PedigreeFixtures._
import matching.CollapseRequest
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
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
    pedCheckService: PedCheckService = mock[PedCheckService],
    matchesRepo: PedigreeMatchesRepository = mock[PedigreeMatchesRepository]
  ): (PedigreeServiceImpl, PedigreeDataRepository, PedigreeRepository) = {
    val cache        = new fixtures.StubCacheService
    val fullText     = new _root_.search.FullTextSearchServiceStub
    val categoryService  = mock[configdata.CategoryService]
    val genoRepo     = mock[PedigreeGenotypificationRepository]
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
      when(traceService.addTracePedigree(any())).thenReturn(Future.successful(Right(0L)))
      val (svc2, dataRepo2, pedigreeRepo2) = buildService(dataRepo = dataRepo, pedigreeRepo = pedigreeRepo, traceService = traceService)

      val result = await(svc2.changePedigreeStatus(pedigreeId, PedigreeStatus.Active, assignee, isSuperUser = false))
      result mustBe Right(pedigreeId)
    }

    "return Left(error.E0930) for an invalid transition" in {
      val (svc, dataRepo, _) = buildService()
      val ped = pedigreeDataCreation(status = PedigreeStatus.UnderConstruction)
      when(dataRepo.getPedigreeMetaData(pedigreeId)).thenReturn(Future.successful(Some(ped)))

      val result = await(svc.changePedigreeStatus(pedigreeId, PedigreeStatus.UnderConstruction, assignee, isSuperUser = false))
      result mustBe Left(s"error.E0930|${PedigreeStatus.UnderConstruction}|${PedigreeStatus.UnderConstruction}")
    }

    "return Right(id) when called by superuser regardless of assignee" in {
      val (svc, dataRepo, pedigreeRepo) = buildService()
      val ped = pedigreeDataCreation(status = PedigreeStatus.UnderConstruction, user = "someone-else")
      when(dataRepo.getPedigreeMetaData(pedigreeId)).thenReturn(Future.successful(Some(ped)))
      when(pedigreeRepo.changeStatus(eqTo(pedigreeId), any())).thenReturn(Future.successful(Right(pedigreeId)))
      when(dataRepo.changePedigreeStatus(eqTo(pedigreeId), any())).thenReturn(Future.successful(Right(pedigreeId)))
      val traceService = mock[TraceService]
      when(traceService.addTracePedigree(any())).thenReturn(Future.successful(Right(0L)))
      val (svc2, dataRepo2, pedigreeRepo2) = buildService(dataRepo = dataRepo, pedigreeRepo = pedigreeRepo, traceService = traceService)

      val result = await(svc2.changePedigreeStatus(pedigreeId, PedigreeStatus.Active, superUser, isSuperUser = true))
      result mustBe Right(pedigreeId)
    }

    "return Left(error.E0644) when user is not the assignee and not superuser" in {
      val (svc, dataRepo, _) = buildService()
      val ped = pedigreeDataCreation(user = "someone-else")
      when(dataRepo.getPedigreeMetaData(pedigreeId)).thenReturn(Future.successful(Some(ped)))

      val result = await(svc.changePedigreeStatus(pedigreeId, PedigreeStatus.Active, assignee, isSuperUser = false))
      result mustBe Left(s"error.E0644|$assignee")
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

  // ─── createPedigree / clonePedigree — Q4 (genograma faltante) ─────────────

  "PedigreeServiceImpl.createPedigree" must {

    "return Left(error.E0201) when pedigreeDataCreation.pedigreeGenogram is None" in {
      val (svc, _, _) = buildService()
      val attempt = PedigreeDataCreation(pedigreeMetaData(), None)
      val result = await(svc.createPedigree(attempt, assignee))
      result mustBe Left("error.E0201")
    }
  }

  "PedigreeServiceImpl.clonePedigree" must {

    "return Left(error.E0201) when source pedigree does not exist (getPedigree returns None)" in {
      val (svc, dataRepo, pedigreeRepo) = buildService()
      when(dataRepo.getPedigreeMetaData(pedigreeId)).thenReturn(Future.successful(None))
      when(pedigreeRepo.get(pedigreeId)).thenReturn(Future.successful(None))

      val result = await(svc.clonePedigree(pedigreeId, assignee))
      result mustBe Left("error.E0201")
    }

    "return Left(error.E0201) when source pedigree has no genogram" in {
      val (svc, dataRepo, pedigreeRepo) = buildService()
      when(dataRepo.getPedigreeMetaData(pedigreeId)).thenReturn(Future.successful(Some(pedigreeDataCreation())))
      when(pedigreeRepo.get(pedigreeId)).thenReturn(Future.successful(None))

      val result = await(svc.clonePedigree(pedigreeId, assignee))
      result mustBe Left("error.E0201")
    }
  }

  // ─── closeAllPedigrees — Q6 (Future.sequence + collect errores) ───────────

  "PedigreeServiceImpl.closeAllPedigrees" must {

    "return Right(courtCaseId) cuando todos los pedigris cierran ok" in {
      val (_, dataRepo, pedigreeRepo) = buildService()
      val activos = Seq(
        PedigreeMetaDataView(10L, courtCaseId, "P1", None, PedigreeStatus.Active),
        PedigreeMetaDataView(11L, courtCaseId, "P2", None, PedigreeStatus.UnderConstruction)
      )
      when(dataRepo.getPedigrees(any())).thenReturn(Future.successful(activos))
      // Para que changePedigreeStatus interno funcione, cada ped debe encontrarse + transiciones válidas
      activos.foreach { p =>
        val pd = pedigreeDataCreation(id = p.id, status = p.status, user = assignee)
        when(dataRepo.getPedigreeMetaData(p.id)).thenReturn(Future.successful(Some(pd)))
        when(pedigreeRepo.changeStatus(eqTo(p.id), eqTo(PedigreeStatus.Closed)))
          .thenReturn(Future.successful(Right(p.id)))
        when(dataRepo.changePedigreeStatus(eqTo(p.id), eqTo(PedigreeStatus.Closed)))
          .thenReturn(Future.successful(Right(p.id)))
      }
      val traceSvc = mock[TraceService]
      when(traceSvc.addTracePedigree(any())).thenReturn(Future.successful(Right(0L)))
      val (svc, _, _) = buildService(dataRepo = dataRepo, pedigreeRepo = pedigreeRepo, traceService = traceSvc)

      val result = await(svc.closeAllPedigrees(courtCaseId, assignee))
      result mustBe Right(courtCaseId)
    }

    "return Right(courtCaseId) cuando no hay pedigris cerrables (filtra Validated/Deleted/Closed)" in {
      val (_, dataRepo, pedigreeRepo) = buildService()
      val noCerrables = Seq(
        PedigreeMetaDataView(20L, courtCaseId, "P", None, PedigreeStatus.Validated),
        PedigreeMetaDataView(21L, courtCaseId, "P", None, PedigreeStatus.Deleted),
        PedigreeMetaDataView(22L, courtCaseId, "P", None, PedigreeStatus.Closed)
      )
      when(dataRepo.getPedigrees(any())).thenReturn(Future.successful(noCerrables))
      val (svc, _, _) = buildService(dataRepo = dataRepo, pedigreeRepo = pedigreeRepo)

      val result = await(svc.closeAllPedigrees(courtCaseId, assignee))
      result mustBe Right(courtCaseId)
      // No se llama a changeStatus en ningún pedigrí
      verify(pedigreeRepo, never()).changeStatus(any(), any())
    }

    "solo cierra UnderConstruction y Active (filtro), ignora otros" in {
      val (_, dataRepo, pedigreeRepo) = buildService()
      val mixed = Seq(
        PedigreeMetaDataView(30L, courtCaseId, "act", None, PedigreeStatus.Active),
        PedigreeMetaDataView(31L, courtCaseId, "uc",  None, PedigreeStatus.UnderConstruction),
        PedigreeMetaDataView(32L, courtCaseId, "val", None, PedigreeStatus.Validated)
      )
      when(dataRepo.getPedigrees(any())).thenReturn(Future.successful(mixed))
      Seq(30L, 31L).foreach { id =>
        val pd = pedigreeDataCreation(id = id, status = if id == 30L then PedigreeStatus.Active else PedigreeStatus.UnderConstruction, user = assignee)
        when(dataRepo.getPedigreeMetaData(id)).thenReturn(Future.successful(Some(pd)))
        when(pedigreeRepo.changeStatus(eqTo(id), eqTo(PedigreeStatus.Closed)))
          .thenReturn(Future.successful(Right(id)))
        when(dataRepo.changePedigreeStatus(eqTo(id), eqTo(PedigreeStatus.Closed)))
          .thenReturn(Future.successful(Right(id)))
      }
      val traceSvc = mock[TraceService]
      when(traceSvc.addTracePedigree(any())).thenReturn(Future.successful(Right(0L)))
      val (svc, _, _) = buildService(dataRepo = dataRepo, pedigreeRepo = pedigreeRepo, traceService = traceSvc)

      val result = await(svc.closeAllPedigrees(courtCaseId, assignee))
      result mustBe Right(courtCaseId)
      verify(pedigreeRepo).changeStatus(30L, PedigreeStatus.Closed)
      verify(pedigreeRepo).changeStatus(31L, PedigreeStatus.Closed)
      verify(pedigreeRepo, never()).changeStatus(eqTo(32L), any())
    }

    "return Left con el primer error cuando algún cierre falla" in {
      val (_, dataRepo, pedigreeRepo) = buildService()
      val activos = Seq(
        PedigreeMetaDataView(40L, courtCaseId, "ok",  None, PedigreeStatus.Active),
        PedigreeMetaDataView(41L, courtCaseId, "bad", None, PedigreeStatus.Active)
      )
      when(dataRepo.getPedigrees(any())).thenReturn(Future.successful(activos))
      // ped 40 OK
      val pd40 = pedigreeDataCreation(id = 40L, status = PedigreeStatus.Active, user = assignee)
      when(dataRepo.getPedigreeMetaData(40L)).thenReturn(Future.successful(Some(pd40)))
      when(pedigreeRepo.changeStatus(eqTo(40L), eqTo(PedigreeStatus.Closed)))
        .thenReturn(Future.successful(Right(40L)))
      when(dataRepo.changePedigreeStatus(eqTo(40L), eqTo(PedigreeStatus.Closed)))
        .thenReturn(Future.successful(Right(40L)))
      // ped 41 falla en mongo changeStatus
      val pd41 = pedigreeDataCreation(id = 41L, status = PedigreeStatus.Active, user = assignee)
      when(dataRepo.getPedigreeMetaData(41L)).thenReturn(Future.successful(Some(pd41)))
      when(pedigreeRepo.changeStatus(eqTo(41L), eqTo(PedigreeStatus.Closed)))
        .thenReturn(Future.successful(Left("mongo down")))
      val traceSvc = mock[TraceService]
      when(traceSvc.addTracePedigree(any())).thenReturn(Future.successful(Right(0L)))
      val (svc, _, _) = buildService(dataRepo = dataRepo, pedigreeRepo = pedigreeRepo, traceService = traceSvc)

      val result = await(svc.closeAllPedigrees(courtCaseId, assignee))
      result mustBe Left("mongo down")
    }
  }

  // ─── changePedigreeStatus — Q5 (rollback también falla) ───────────────────

  "PedigreeServiceImpl.changePedigreeStatus" should {

    "preserve original error when rollback ALSO fails" in {
      val (_, dataRepo, pedigreeRepo) = buildService()
      val originalStatus = PedigreeStatus.UnderConstruction
      val ped = pedigreeDataCreation(status = originalStatus, user = assignee)
      when(dataRepo.getPedigreeMetaData(pedigreeId)).thenReturn(Future.successful(Some(ped)))
      // mongo update succeeds
      when(pedigreeRepo.changeStatus(eqTo(pedigreeId), eqTo(PedigreeStatus.Active)))
        .thenReturn(Future.successful(Right(pedigreeId)))
      // postgres fails
      when(dataRepo.changePedigreeStatus(eqTo(pedigreeId), eqTo(PedigreeStatus.Active)))
        .thenReturn(Future.successful(Left("postgres down")))
      // rollback to original status ALSO fails
      when(pedigreeRepo.changeStatus(eqTo(pedigreeId), eqTo(originalStatus)))
        .thenReturn(Future.successful(Left("mongo also down")))
      val (svc, _, _) = buildService(dataRepo = dataRepo, pedigreeRepo = pedigreeRepo)

      val result = await(svc.changePedigreeStatus(pedigreeId, PedigreeStatus.Active, assignee, isSuperUser = false))
      result mustBe Left("postgres down")  // original error, not "mongo also down"
    }
  }

  // ─── collapseGroup — Q11 (recover + manual-cleanup logging) ───────────────

  "PedigreeServiceImpl.collapseGroup" must {

    val collapseReq = CollapseRequest(
      globalCodeParent = "AR-B-HIBA-1",
      globalCodeChildren = List("AR-B-HIBA-2", "AR-B-HIBA-3"),
      courtCaseId = courtCaseId
    )

    // helper: configura areAssignedToPedigree para devolver Right (no profile en pedigris, no pending matches)
    def stubAreAssignedRight(dataRepo: PedigreeDataRepository, pedigreeRepo: PedigreeRepository,
                             matchesRepo: PedigreeMatchesRepository): Unit = {
      when(dataRepo.getPedigrees(any())).thenReturn(Future.successful(Seq.empty))
      when(pedigreeRepo.countByProfileIdPedigrees(any(), any())).thenReturn(Future.successful(0))
      when(matchesRepo.profileNumberOfPendingMatchesInPedigrees(any(), any())).thenReturn(Future.successful(0))
    }

    "propagate Left when areAssignedToPedigree fails (profile asociado a pedigree activo)" in {
      val dataRepo     = mock[PedigreeDataRepository]
      val pedigreeRepo = mock[PedigreeRepository]
      val matchesRepo  = mock[PedigreeMatchesRepository]
      // areAssignedToPedigree: getPedigrees devuelve algún pedigree y countByProfileIdPedigrees > 0 → Left
      when(dataRepo.getPedigrees(any())).thenReturn(Future.successful(Seq.empty))
      when(pedigreeRepo.countByProfileIdPedigrees(any(), any())).thenReturn(Future.successful(1))
      val (svc, _, _) = buildService(dataRepo = dataRepo, pedigreeRepo = pedigreeRepo, matchesRepo = matchesRepo)

      val result = await(svc.collapseGroup(collapseReq))
      result.isLeft mustBe true
      // 2 children → E0207 (más de uno)
      result.left.toOption.get must startWith("error.E0207|")
      // No se debe haber llamado a associateGroupedProfiles
      verify(dataRepo, never()).associateGroupedProfiles(any())
    }

    "return Left(original) when associateGroupedProfiles fails" in {
      val dataRepo     = mock[PedigreeDataRepository]
      val pedigreeRepo = mock[PedigreeRepository]
      val matchesRepo  = mock[PedigreeMatchesRepository]
      stubAreAssignedRight(dataRepo, pedigreeRepo, matchesRepo)
      when(dataRepo.associateGroupedProfiles(any())).thenReturn(Future.successful(Left("dup key")))
      val (svc, _, _) = buildService(dataRepo = dataRepo, pedigreeRepo = pedigreeRepo, matchesRepo = matchesRepo)

      val result = await(svc.collapseGroup(collapseReq))
      result mustBe Left("dup key")
    }

    "return Left(error.E0630) when discardCollapsing fails (recover branch with manual-cleanup log)" in {
      val dataRepo      = mock[PedigreeDataRepository]
      val pedigreeRepo  = mock[PedigreeRepository]
      val matchesRepo   = mock[PedigreeMatchesRepository]
      val matchingSvc   = mock[matching.MatchingService]
      stubAreAssignedRight(dataRepo, pedigreeRepo, matchesRepo)
      when(dataRepo.associateGroupedProfiles(any())).thenReturn(Future.successful(Right(())))
      when(matchingSvc.discardCollapsingByLeftAndRightProfile(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("matching service down")))
      when(matchingSvc.discardCollapsingByRightProfile(any(), any()))
        .thenReturn(Future.successful(()))
      val (svc, _, _) = buildService(
        dataRepo = dataRepo, pedigreeRepo = pedigreeRepo,
        matchesRepo = matchesRepo, matchingService = matchingSvc
      )

      val result = await(svc.collapseGroup(collapseReq))
      result mustBe Left("error.E0630")
    }

    "happy path: areAssigned=Right, associate=Right, discardCollapsing=ok → Right(())" in {
      val dataRepo      = mock[PedigreeDataRepository]
      val pedigreeRepo  = mock[PedigreeRepository]
      val matchesRepo   = mock[PedigreeMatchesRepository]
      val matchingSvc   = mock[matching.MatchingService]
      stubAreAssignedRight(dataRepo, pedigreeRepo, matchesRepo)
      when(dataRepo.associateGroupedProfiles(any())).thenReturn(Future.successful(Right(())))
      when(matchingSvc.discardCollapsingByLeftAndRightProfile(any(), any()))
        .thenReturn(Future.successful(()))
      when(matchingSvc.discardCollapsingByRightProfile(any(), any()))
        .thenReturn(Future.successful(()))
      val (svc, _, _) = buildService(
        dataRepo = dataRepo, pedigreeRepo = pedigreeRepo,
        matchesRepo = matchesRepo, matchingService = matchingSvc
      )

      val result = await(svc.collapseGroup(collapseReq))
      result mustBe Right(())
      // 2 hijos → discardCollapsingByLeftAndRight × 2 + discardCollapsingByRight × 1
      verify(matchingSvc, times(2)).discardCollapsingByLeftAndRightProfile(any(), any())
      verify(matchingSvc, times(1)).discardCollapsingByRightProfile(eqTo("AR-B-HIBA-1"), eqTo(courtCaseId))
    }
  }
}
