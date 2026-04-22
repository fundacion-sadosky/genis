package unit.pedigree

import configdata.CategoryService
import fixtures.PedigreeFixtures._
import inbox.NotificationService
import matching.MatchingService
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import pedigree.*
import profiledata.ProfileDataRepository
import trace.TraceService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class PedigreeMatchesServiceTest extends AnyWordSpec with Matchers with MockitoSugar {

  val duration: Duration = 10.seconds
  def await[T](f: Future[T]): T = Await.result(f, duration)

  def buildService(
    matchesRepo: PedigreeMatchesRepository = mock[PedigreeMatchesRepository],
    dataRepo: PedigreeDataRepository = mock[PedigreeDataRepository],
    notifSvc: NotificationService = mock[NotificationService],
    traceService: TraceService = mock[TraceService],
    profileDataRepo: ProfileDataRepository = mock[ProfileDataRepository],
    categorySvc: CategoryService = mock[CategoryService]
  ): PedigreeMatchesServiceImpl =
    new PedigreeMatchesServiceImpl(
      pedigreeDataRepository = dataRepo,
      pedigreeMatchesRepository = matchesRepo,
      bayesianNetworkService = mock[BayesianNetworkService],
      matchingService = mock[MatchingService],
      pedigreeGenotypificationRepository = mock[PedigreeGenotypificationRepository],
      profileDataRepo = profileDataRepo,
      traceService = traceService,
      notificationService = notifSvc,
      categoryService = categorySvc,
      exportProfilesPath = ""
    )

  // ─── allMatchesDiscarded ──────────────────────────────────────────────────

  "PedigreeMatchesServiceImpl.allMatchesDiscarded" must {
    "delegate to repository and return its result" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      when(matchesRepo.allMatchesDiscarded(pedigreeId)).thenReturn(Future.successful(true))
      val svc = buildService(matchesRepo = matchesRepo)

      await(svc.allMatchesDiscarded(pedigreeId)) mustBe true
    }
  }

  // ─── countMatches ─────────────────────────────────────────────────────────

  "PedigreeMatchesServiceImpl.countMatches" must {
    "return count from repository" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val search = PedigreeMatchCardSearch(user = assignee, isSuperUser = false, group = "pedigree",
        page = 0, pageSize = 10, profile = None, hourFrom = None, hourUntil = None,
        category = None, caseType = None, status = None, idCourtCase = None)
      val profileDataRepo = mock[ProfileDataRepository]
      when(profileDataRepo.getGlobalCode(any())).thenReturn(Future.successful(None))
      when(matchesRepo.countMatches(any())).thenReturn(Future.successful(7))
      val svc = buildService(matchesRepo = matchesRepo, profileDataRepo = profileDataRepo)

      await(svc.countMatches(search)) mustBe 7
    }
  }

  // ─── countMatchesByGroup ──────────────────────────────────────────────────

  "PedigreeMatchesServiceImpl.countMatchesByGroup" must {
    "return count from repository" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val search = PedigreeMatchGroupSearch(user = assignee, isSuperUser = false, id = pedigreeId.toString,
        groupBy = "pedigree", kind = PedigreeMatchKind.DirectLink, page = 0, pageSize = 10,
        sortField = "matchingDate", ascending = false)
      when(matchesRepo.countMatchesByGroup(search)).thenReturn(Future.successful(4))
      val svc = buildService(matchesRepo = matchesRepo)

      await(svc.countMatchesByGroup(search)) mustBe 4
    }
  }

  // ─── deleteMatches ────────────────────────────────────────────────────────

  "PedigreeMatchesServiceImpl.deleteMatches" must {
    "return Right(id) from repository" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      when(matchesRepo.deleteMatches(pedigreeId)).thenReturn(Future.successful(Right(pedigreeId)))
      val svc = buildService(matchesRepo = matchesRepo)

      await(svc.deleteMatches(pedigreeId)) mustBe Right(pedigreeId)
    }
  }

  // ─── getMatchById ─────────────────────────────────────────────────────────

  "PedigreeMatchesServiceImpl.getMatchById" must {
    "return Some(JsValue) containing the match id when match exists" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val matchResult = pedigreeDirectLinkMatch()
      when(matchesRepo.getMatchById(mongoId.id)).thenReturn(Future.successful(Some(matchResult)))
      val svc = buildService(matchesRepo = matchesRepo)

      val result = await(svc.getMatchById(mongoId.id))
      result mustBe defined
      (result.get \ "_id").as[String] mustBe mongoId.id
    }

    "return None when match does not exist" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      when(matchesRepo.getMatchById(any())).thenReturn(Future.successful(None))
      val svc = buildService(matchesRepo = matchesRepo)

      await(svc.getMatchById("nonexistent")) mustBe None
    }
  }

  // ─── discard ──────────────────────────────────────────────────────────────

  "PedigreeMatchesServiceImpl.discard" must {

    "discard both pedigree and profile sides as superuser" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val matchResult = pedigreeDirectLinkMatch(user = "other-user")
      when(matchesRepo.getMatchById(mongoId.id)).thenReturn(Future.successful(Some(matchResult)))
      when(matchesRepo.discardProfile(mongoId.id)).thenReturn(Future.successful(Right(mongoId.id)))
      when(matchesRepo.discardPedigree(mongoId.id)).thenReturn(Future.successful(Right(mongoId.id)))
      val dataRepo = mock[PedigreeDataRepository]
      when(dataRepo.getPedigreeDescriptionById(any())).thenReturn(Future.successful((None, None)))
      val traceService = mock[TraceService]
      when(traceService.add(any())).thenReturn(Future.successful(()))
      when(traceService.addTracePedigree(any())).thenReturn(Future.successful(()))
      val notifSvc = mock[NotificationService]
      val svc = buildService(matchesRepo = matchesRepo, dataRepo = dataRepo,
        traceService = traceService, notifSvc = notifSvc)

      val result = await(svc.discard(mongoId.id, superUser, isSuperUser = true))
      result mustBe Right(mongoId.id)
      verify(matchesRepo).discardProfile(mongoId.id)
      verify(matchesRepo).discardPedigree(mongoId.id)
    }

    "discard both sides when user is the pedigree assignee" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val matchResult = pedigreeDirectLinkMatch(user = assignee)
      when(matchesRepo.getMatchById(mongoId.id)).thenReturn(Future.successful(Some(matchResult)))
      when(matchesRepo.discardProfile(mongoId.id)).thenReturn(Future.successful(Right(mongoId.id)))
      when(matchesRepo.discardPedigree(mongoId.id)).thenReturn(Future.successful(Right(mongoId.id)))
      val dataRepo = mock[PedigreeDataRepository]
      when(dataRepo.getPedigreeDescriptionById(any())).thenReturn(Future.successful((None, None)))
      val traceService = mock[TraceService]
      when(traceService.add(any())).thenReturn(Future.successful(()))
      when(traceService.addTracePedigree(any())).thenReturn(Future.successful(()))
      val notifSvc = mock[NotificationService]
      val svc = buildService(matchesRepo = matchesRepo, dataRepo = dataRepo,
        traceService = traceService, notifSvc = notifSvc)

      val result = await(svc.discard(mongoId.id, assignee, isSuperUser = false))
      result mustBe Right(mongoId.id)
      verify(matchesRepo).discardProfile(mongoId.id)
      verify(matchesRepo).discardPedigree(mongoId.id)
    }

    "return Left(error.E0642) when user is not assignee and not superuser" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val matchResult = pedigreeDirectLinkMatch(user = "another-user")
      when(matchesRepo.getMatchById(mongoId.id)).thenReturn(Future.successful(Some(matchResult)))
      val svc = buildService(matchesRepo = matchesRepo)

      val result = await(svc.discard(mongoId.id, "unrelated-user", isSuperUser = false))
      result mustBe Left("error.E0642")
      verify(matchesRepo, never()).discardProfile(any())
      verify(matchesRepo, never()).discardPedigree(any())
    }
  }
}
