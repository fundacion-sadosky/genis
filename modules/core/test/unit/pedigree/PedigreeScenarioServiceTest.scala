package unit.pedigree

import fixtures.PedigreeFixtures._
import inbox.{NotificationService, PedigreeLRInfo}
import matching.MongoId
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import pedigree.*
import scenarios.ScenarioStatus

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class PedigreeScenarioServiceTest extends AnyWordSpec with Matchers with MockitoSugar {

  val duration: Duration = 10.seconds

  def await[T](f: Future[T]): T = Await.result(f, duration)

  def buildService(
    scenarioRepo: PedigreeScenarioRepository = mock[PedigreeScenarioRepository],
    pedigreeService: PedigreeService = mock[PedigreeService],
    notificationService: NotificationService = mock[NotificationService]
  ): PedigreeScenarioServiceImpl = {
    val provider = new jakarta.inject.Provider[PedigreeService] { def get(): PedigreeService = pedigreeService }
    new PedigreeScenarioServiceImpl(scenarioRepo, provider, notificationService)
  }

  // ─── createScenario ───────────────────────────────────────────────────────

  "PedigreeScenarioServiceImpl.createScenario" must {
    "return Right(MongoId) on success" in {
      val repo = mock[PedigreeScenarioRepository]
      val scenario = pedigreeScenario()
      when(repo.create(scenario)).thenReturn(Future.successful(Right(mongoId)))
      val svc = buildService(scenarioRepo = repo)

      await(svc.createScenario(scenario)) mustBe Right(mongoId)
    }
  }

  // ─── updateScenario ───────────────────────────────────────────────────────

  "PedigreeScenarioServiceImpl.updateScenario" must {
    "return Right(MongoId) on success" in {
      val repo = mock[PedigreeScenarioRepository]
      val scenario = pedigreeScenario()
      when(repo.update(scenario)).thenReturn(Future.successful(Right(mongoId)))
      val svc = buildService(scenarioRepo = repo)

      await(svc.updateScenario(scenario)) mustBe Right(mongoId)
    }
  }

  // ─── getScenarios ─────────────────────────────────────────────────────────

  "PedigreeScenarioServiceImpl.getScenarios" must {
    "return sequence of scenarios for a pedigree id" in {
      val repo     = mock[PedigreeScenarioRepository]
      val scenario = pedigreeScenario()
      when(repo.getByPedigree(pedigreeId)).thenReturn(Future.successful(Seq(scenario)))
      val svc = buildService(scenarioRepo = repo)

      val result = await(svc.getScenarios(pedigreeId))
      result must contain(scenario)
    }
  }

  // ─── changeScenarioStatus ─────────────────────────────────────────────────

  "PedigreeScenarioServiceImpl.changeScenarioStatus" must {

    "return Right(MongoId) for valid transition Pending -> Deleted" in {
      val repo     = mock[PedigreeScenarioRepository]
      val scenario = pedigreeScenario(status = ScenarioStatus.Pending)
      when(repo.update(scenario)).thenReturn(Future.successful(Right(mongoId)))
      when(repo.changeStatus(mongoId, ScenarioStatus.Deleted)).thenReturn(Future.successful(Right(mongoId)))
      val pedSvc = mock[PedigreeService]
      val svc = buildService(scenarioRepo = repo, pedigreeService = pedSvc)

      val result = await(svc.changeScenarioStatus(scenario, ScenarioStatus.Deleted, assignee, isSuperUser = false))
      result mustBe Right(mongoId)
    }

    "return Left when transition from Validated is requested" in {
      val repo     = mock[PedigreeScenarioRepository]
      val scenario = pedigreeScenario(status = ScenarioStatus.Validated)
      val svc = buildService(scenarioRepo = repo)

      val result = await(svc.changeScenarioStatus(scenario, ScenarioStatus.Deleted, assignee, isSuperUser = false))
      result mustBe a[Left[?, ?]]
      result.left.get must include("error.E0930")
    }

    "call repo.update before changeStatus" in {
      val repo     = mock[PedigreeScenarioRepository]
      val scenario = pedigreeScenario(status = ScenarioStatus.Pending)
      when(repo.update(scenario)).thenReturn(Future.successful(Right(mongoId)))
      when(repo.changeStatus(mongoId, ScenarioStatus.Deleted)).thenReturn(Future.successful(Right(mongoId)))
      val pedSvc = mock[PedigreeService]
      val svc = buildService(scenarioRepo = repo, pedigreeService = pedSvc)

      await(svc.changeScenarioStatus(scenario, ScenarioStatus.Deleted, assignee, isSuperUser = false))

      verify(repo).update(scenario)
      verify(repo).changeStatus(mongoId, ScenarioStatus.Deleted)
    }

    "not call changeStatus when update fails" in {
      val repo     = mock[PedigreeScenarioRepository]
      val scenario = pedigreeScenario(status = ScenarioStatus.Pending)
      when(repo.update(scenario)).thenReturn(Future.successful(Left("db error")))
      val svc = buildService(scenarioRepo = repo)

      val result = await(svc.changeScenarioStatus(scenario, ScenarioStatus.Deleted, assignee, isSuperUser = false))
      result mustBe Left("db error")
      verify(repo, never()).changeStatus(any(), any())
    }

    "call pedigreeService.changePedigreeStatus when transitioning to Validated" in {
      val repo     = mock[PedigreeScenarioRepository]
      val pedSvc   = mock[PedigreeService]
      val scenario = pedigreeScenario(status = ScenarioStatus.Pending)
      when(repo.update(scenario)).thenReturn(Future.successful(Right(mongoId)))
      when(pedSvc.changePedigreeStatus(eqTo(pedigreeId), eqTo(PedigreeStatus.Validated), any(), eqTo(true)))
        .thenReturn(Future.successful(Right(pedigreeId)))
      when(repo.changeStatus(mongoId, ScenarioStatus.Validated)).thenReturn(Future.successful(Right(mongoId)))
      val pedData = pedigreeDataCreation()
      when(pedSvc.getPedigree(pedigreeId)).thenReturn(Future.successful(Some(pedData)))
      val notifSvc = mock[NotificationService]
      val svc = buildService(scenarioRepo = repo, pedigreeService = pedSvc, notificationService = notifSvc)

      val result = await(svc.changeScenarioStatus(scenario, ScenarioStatus.Validated, assignee, isSuperUser = false))
      result mustBe Right(mongoId)
      verify(pedSvc).changePedigreeStatus(pedigreeId, PedigreeStatus.Validated, assignee, isSuperUser = true)
    }

    "call notificationService.solve on success" in {
      val repo     = mock[PedigreeScenarioRepository]
      val pedSvc   = mock[PedigreeService]
      val notifSvc = mock[NotificationService]
      val scenario = pedigreeScenario(status = ScenarioStatus.Pending)
      when(repo.update(scenario)).thenReturn(Future.successful(Right(mongoId)))
      when(repo.changeStatus(mongoId, ScenarioStatus.Deleted)).thenReturn(Future.successful(Right(mongoId)))
      when(pedSvc.getPedigree(pedigreeId)).thenReturn(Future.successful(Some(pedigreeDataCreation())))
      val svc = buildService(scenarioRepo = repo, pedigreeService = pedSvc, notificationService = notifSvc)

      await(svc.changeScenarioStatus(scenario, ScenarioStatus.Deleted, assignee, isSuperUser = false))
      // Give the onComplete side-effect a moment to run
      Thread.sleep(100)
      verify(notifSvc).solve(eqTo(assignee), any())
    }

    "not call notificationService.solve on failure" in {
      val repo     = mock[PedigreeScenarioRepository]
      val notifSvc = mock[NotificationService]
      val scenario = pedigreeScenario(status = ScenarioStatus.Validated)
      val svc = buildService(scenarioRepo = repo, notificationService = notifSvc)

      await(svc.changeScenarioStatus(scenario, ScenarioStatus.Deleted, assignee, isSuperUser = false))
      Thread.sleep(50)
      verify(notifSvc, never()).solve(any(), any())
    }
  }
}
