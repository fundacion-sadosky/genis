package unit.pedigree

import configdata.CategoryService
import fixtures.PedigreeFixtures._
import inbox.NotificationService
import matching.{MatchingService, MongoId}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import pedigree.*
import profiledata.{ProfileData, ProfileDataRepository}
import trace.TraceService
import types.{AlphanumericId, SampleCode}

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

    // Q-C2 verification: getMatchById returns None when the matchId is
    // not a valid 24-char hex ObjectId, OR when the document doesn't exist.
    // In both cases discard must surface error.E0631 instead of crashing.
    "return Left(error.E0631) when getMatchById returns None" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      when(matchesRepo.getMatchById(any())).thenReturn(Future.successful(None))
      val svc = buildService(matchesRepo = matchesRepo)

      await(svc.discard("not-a-valid-objectid", assignee, isSuperUser = true)) mustBe Left("error.E0631")
      verify(matchesRepo, never()).discardProfile(any())
      verify(matchesRepo, never()).discardPedigree(any())
    }

    "stop after discardProfile failure (does not call discardPedigree)" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val matchResult = pedigreeDirectLinkMatch(user = assignee)
      when(matchesRepo.getMatchById(mongoId.id)).thenReturn(Future.successful(Some(matchResult)))
      when(matchesRepo.discardProfile(mongoId.id)).thenReturn(Future.successful(Left("error.E0630")))
      val svc = buildService(matchesRepo = matchesRepo)

      val result = await(svc.discard(mongoId.id, assignee, isSuperUser = false))
      result mustBe Left("error.E0630")
      verify(matchesRepo).discardProfile(mongoId.id)
      verify(matchesRepo, never()).discardPedigree(any())
    }
  }

  // ─── confirm ──────────────────────────────────────────────────────────────

  "PedigreeMatchesServiceImpl.confirm" must {

    "confirm both pedigree and profile sides when both repository calls succeed" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val matchResult = pedigreeDirectLinkMatch()
      when(matchesRepo.getMatchById(mongoId.id)).thenReturn(Future.successful(Some(matchResult)))
      when(matchesRepo.confirmProfile(mongoId.id)).thenReturn(Future.successful(Right(mongoId.id)))
      when(matchesRepo.confirmPedigree(mongoId.id)).thenReturn(Future.successful(Right(mongoId.id)))
      val dataRepo = mock[PedigreeDataRepository]
      when(dataRepo.getPedigreeDescriptionById(any())).thenReturn(Future.successful((None, None)))
      val traceService = mock[TraceService]
      when(traceService.add(any())).thenReturn(Future.successful(()))
      when(traceService.addTracePedigree(any())).thenReturn(Future.successful(()))
      val svc = buildService(matchesRepo = matchesRepo, dataRepo = dataRepo,
        traceService = traceService, notifSvc = mock[NotificationService])

      val result = await(svc.confirm(mongoId.id, assignee, isSuperUser = false))
      result mustBe Right(mongoId.id)
      verify(matchesRepo).confirmProfile(mongoId.id)
      verify(matchesRepo).confirmPedigree(mongoId.id)
    }

    // Q-C2 verification
    "return Left(error.E0631) when getMatchById returns None" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      when(matchesRepo.getMatchById(any())).thenReturn(Future.successful(None))
      val svc = buildService(matchesRepo = matchesRepo)

      await(svc.confirm("not-a-valid-objectid", assignee, isSuperUser = true)) mustBe Left("error.E0631")
      verify(matchesRepo, never()).confirmProfile(any())
      verify(matchesRepo, never()).confirmPedigree(any())
    }

    "stop after confirmProfile failure (does not call confirmPedigree)" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val matchResult = pedigreeDirectLinkMatch()
      when(matchesRepo.getMatchById(mongoId.id)).thenReturn(Future.successful(Some(matchResult)))
      when(matchesRepo.confirmProfile(mongoId.id)).thenReturn(Future.successful(Left("error.E0630")))
      val svc = buildService(matchesRepo = matchesRepo)

      val result = await(svc.confirm(mongoId.id, assignee, isSuperUser = false))
      result mustBe Left("error.E0630")
      verify(matchesRepo).confirmProfile(mongoId.id)
      verify(matchesRepo, never()).confirmPedigree(any())
    }

    "propagate confirmPedigree failure even if confirmProfile succeeded" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val matchResult = pedigreeDirectLinkMatch()
      when(matchesRepo.getMatchById(mongoId.id)).thenReturn(Future.successful(Some(matchResult)))
      when(matchesRepo.confirmProfile(mongoId.id)).thenReturn(Future.successful(Right(mongoId.id)))
      when(matchesRepo.confirmPedigree(mongoId.id)).thenReturn(Future.successful(Left("error.E0630")))
      val svc = buildService(matchesRepo = matchesRepo)

      val result = await(svc.confirm(mongoId.id, assignee, isSuperUser = false))
      result mustBe Left("error.E0630")
      verify(matchesRepo).confirmProfile(mongoId.id)
      verify(matchesRepo).confirmPedigree(mongoId.id)
    }
  }

  // ─── masiveGroupDiscardByGroup ────────────────────────────────────────────

  "PedigreeMatchesServiceImpl.masiveGroupDiscardByGroup" must {

    "return Left(\"Sin matches\") when the repository finds no matches in the group" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      when(matchesRepo.getAllMatchNonDiscardedByGroup(any(), any()))
        .thenReturn(Future.successful(Seq.empty))
      val svc = buildService(matchesRepo = matchesRepo)

      await(svc.masiveGroupDiscardByGroup("group-id", "pedigree", isSuperUser = true, userId = superUser)) mustBe Left("Sin matches")
      verify(matchesRepo, never()).discardProfile(any())
    }

    "return Right(id) when every individual discard succeeds" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val match1 = pedigreeDirectLinkMatch(id = MongoId("5e8f8f8f8f8f8f8f8f8f8f01"))
      val match2 = pedigreeDirectLinkMatch(id = MongoId("5e8f8f8f8f8f8f8f8f8f8f02"))
      when(matchesRepo.getAllMatchNonDiscardedByGroup(any(), any()))
        .thenReturn(Future.successful(Seq(match1, match2)))
      when(matchesRepo.getMatchById(match1._id.id)).thenReturn(Future.successful(Some(match1)))
      when(matchesRepo.getMatchById(match2._id.id)).thenReturn(Future.successful(Some(match2)))
      when(matchesRepo.discardProfile(any())).thenReturn(Future.successful(Right("ok")))
      when(matchesRepo.discardPedigree(any())).thenReturn(Future.successful(Right("ok")))
      val dataRepo = mock[PedigreeDataRepository]
      when(dataRepo.getPedigreeDescriptionById(any())).thenReturn(Future.successful((None, None)))
      val traceService = mock[TraceService]
      when(traceService.add(any())).thenReturn(Future.successful(()))
      when(traceService.addTracePedigree(any())).thenReturn(Future.successful(()))
      val svc = buildService(matchesRepo = matchesRepo, dataRepo = dataRepo,
        traceService = traceService, notifSvc = mock[NotificationService])

      await(svc.masiveGroupDiscardByGroup("group-id", "pedigree", isSuperUser = true, userId = superUser)) mustBe Right("group-id")
    }

    "return Left with comma-joined errors when at least one discard fails" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val match1 = pedigreeDirectLinkMatch(id = MongoId("5e8f8f8f8f8f8f8f8f8f8f01"))
      val match2 = pedigreeDirectLinkMatch(id = MongoId("5e8f8f8f8f8f8f8f8f8f8f02"), user = "another-user")
      when(matchesRepo.getAllMatchNonDiscardedByGroup(any(), any()))
        .thenReturn(Future.successful(Seq(match1, match2)))
      when(matchesRepo.getMatchById(match1._id.id)).thenReturn(Future.successful(Some(match1)))
      when(matchesRepo.getMatchById(match2._id.id)).thenReturn(Future.successful(Some(match2)))
      when(matchesRepo.discardProfile(match1._id.id)).thenReturn(Future.successful(Right("ok")))
      when(matchesRepo.discardPedigree(match1._id.id)).thenReturn(Future.successful(Right("ok")))
      val dataRepo = mock[PedigreeDataRepository]
      when(dataRepo.getPedigreeDescriptionById(any())).thenReturn(Future.successful((None, None)))
      val traceService = mock[TraceService]
      when(traceService.add(any())).thenReturn(Future.successful(()))
      when(traceService.addTracePedigree(any())).thenReturn(Future.successful(()))
      val svc = buildService(matchesRepo = matchesRepo, dataRepo = dataRepo,
        traceService = traceService, notifSvc = mock[NotificationService])

      // userId = assignee, not superuser → match1 ok (assignee match), match2 fails E0642
      val result = await(svc.masiveGroupDiscardByGroup("group-id", "pedigree", isSuperUser = false, userId = assignee))
      result mustBe a[Left[?, ?]]
      result.left.toOption.get must include("error.E0642")
    }
  }

  // ─── getMatchesByGroup ────────────────────────────────────────────────────

  "PedigreeMatchesServiceImpl.getMatchesByGroup" must {

    "enrich each match with internalSampleCode from profileDataRepo when the profile exists" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val match1 = pedigreeDirectLinkMatch(profileCode = "AR-B-HIBA-1")
      when(matchesRepo.getMatchesByGroup(any())).thenReturn(Future.successful(Seq(match1)))

      val profileDataRepo = mock[ProfileDataRepository]
      val pd = ProfileData(
        category = AlphanumericId("IR"),
        globalCode = SampleCode("AR-B-HIBA-1"),
        attorney = None,
        bioMaterialType = None,
        court = None,
        crimeInvolved = None,
        crimeType = None,
        criminalCase = None,
        internalSampleCode = "INT-001",
        assignee = assignee,
        laboratory = "",
        deleted = false,
        responsibleGeneticist = None,
        profileExpirationDate = None,
        sampleDate = None,
        sampleEntryDate = None,
        isExternal = false
      )
      when(profileDataRepo.get(SampleCode("AR-B-HIBA-1"))).thenReturn(Future.successful(Some(pd)))
      val svc = buildService(matchesRepo = matchesRepo, profileDataRepo = profileDataRepo)

      val search = PedigreeMatchGroupSearch(user = assignee, isSuperUser = false, id = pedigreeId.toString,
        groupBy = "pedigree", kind = PedigreeMatchKind.DirectLink, page = 0, pageSize = 10,
        sortField = "matchingDate", ascending = false)
      val result = await(svc.getMatchesByGroup(search))
      result must have size 1
      result.head.internalCode mustBe "INT-001"
    }

    "use empty internalCode when profileDataRepo finds no profile" in {
      val matchesRepo = mock[PedigreeMatchesRepository]
      val match1 = pedigreeDirectLinkMatch(profileCode = "AR-B-HIBA-99")
      when(matchesRepo.getMatchesByGroup(any())).thenReturn(Future.successful(Seq(match1)))
      val profileDataRepo = mock[ProfileDataRepository]
      when(profileDataRepo.get(any())).thenReturn(Future.successful(None))
      val svc = buildService(matchesRepo = matchesRepo, profileDataRepo = profileDataRepo)

      val search = PedigreeMatchGroupSearch(user = assignee, isSuperUser = false, id = pedigreeId.toString,
        groupBy = "pedigree", kind = PedigreeMatchKind.DirectLink, page = 0, pageSize = 10,
        sortField = "matchingDate", ascending = false)
      val result = await(svc.getMatchesByGroup(search))
      result must have size 1
      result.head.internalCode mustBe ""
    }
  }
}
