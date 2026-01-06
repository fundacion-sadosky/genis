package pedigrees

import java.util.Date

import inbox.{NotificationService, PedigreeLRInfo}
import org.bson.types.ObjectId
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import pedigree.PedigreeStatus.PedigreeStatus
import pedigree._
import scenarios.ScenarioStatus
import scenarios.ScenarioStatus._
import specs.PdgSpec
import stubs.Stubs
import types.MongoId

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, _}

class PedigreeScenarioServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)
  val scenario = PedigreeScenario(MongoId(new ObjectId().toString), 55l, "scenario", "descripción", Seq(), ScenarioStatus.Pending, "test")
  val pedigree = PedigreeDataCreation(PedigreeMetaData(55, 55, "Pedigree", new Date(), PedigreeStatus.Active, "pdg"), None)

  "PedigreeScenarioService" must {
    "create a scenario" in {
      val mockResult = Right(scenario._id)

      val pedigreeScenarioRepository = mock[PedigreeScenarioRepository]
      when(pedigreeScenarioRepository.create(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

      val service = new PedigreeScenarioServiceImpl(pedigreeScenarioRepository, null, Stubs.notificationServiceMock)
      val result = Await.result(service.createScenario(scenario), duration)

      result mustBe mockResult
    }

    "update a scenario" in {
      val mockResult = Right(scenario._id)

      val pedigreeScenarioRepository = mock[PedigreeScenarioRepository]
      when(pedigreeScenarioRepository.update(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

      val service = new PedigreeScenarioServiceImpl(pedigreeScenarioRepository, null, Stubs.notificationServiceMock)
      val result = Await.result(service.updateScenario(scenario), duration)

      result mustBe mockResult
    }

    "get scenarios by pedigree" in {
      val mockResult = Seq(scenario)

      val pedigreeScenarioRepository = mock[PedigreeScenarioRepository]
      when(pedigreeScenarioRepository.getByPedigree(any[Long])).thenReturn(Future.successful(mockResult))

      val service = new PedigreeScenarioServiceImpl(pedigreeScenarioRepository, null, Stubs.notificationServiceMock)
      val result = Await.result(service.getScenarios(scenario.pedigreeId), duration)

      result mustBe mockResult
    }

    "change a scenario status" in {
      val mockResult = Right(scenario._id)

      val pedigreeScenarioRepository = mock[PedigreeScenarioRepository]
      when(pedigreeScenarioRepository.changeStatus(any[MongoId], any[ScenarioStatus])).thenReturn(Future.successful(mockResult))
      when(pedigreeScenarioRepository.update(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getCourtCase(scenario.pedigreeId, "", true)).thenReturn(Future.successful(Some(Stubs.courtCaseFull)))

      val service = new PedigreeScenarioServiceImpl(pedigreeScenarioRepository, pedigreeService, Stubs.notificationServiceMock)
      val result = Await.result(service.changeScenarioStatus(scenario, ScenarioStatus.Deleted, "", true), duration)

      result mustBe mockResult
    }

    "not change a scenario status when the transition is invalid E0930" in {
      val scenario = PedigreeScenario(MongoId(new ObjectId().toString), 55l, "scenario", "descripción", Seq(), ScenarioStatus.Validated, "base")

      val service = new PedigreeScenarioServiceImpl(null, null, Stubs.notificationServiceMock)
      val result = Await.result(service.changeScenarioStatus(scenario, ScenarioStatus.Deleted, "", true), duration)

      result.isLeft mustBe true
      result mustBe Left("E0930: La transición de Validated a Deleted no es válida.")
    }

    "save changes to scenario when changing its status" in {
      val mockResult = Right(scenario._id)

      val pedigreeScenarioRepository = mock[PedigreeScenarioRepository]
      when(pedigreeScenarioRepository.changeStatus(any[MongoId], any[ScenarioStatus])).thenReturn(Future.successful(mockResult))
      when(pedigreeScenarioRepository.update(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getCourtCase(scenario.pedigreeId, "", true)).thenReturn(Future.successful(Some(Stubs.courtCaseFull)))

      val service = new PedigreeScenarioServiceImpl(pedigreeScenarioRepository, pedigreeService, Stubs.notificationServiceMock)
      val result = Await.result(service.changeScenarioStatus(scenario, ScenarioStatus.Deleted, "", true), duration)

      result mustBe mockResult
      verify(pedigreeScenarioRepository).update(scenario)
    }
  }

  "not change status if update fails during change status" in {
    val mockResult = Left("Error")

    val pedigreeScenarioRepository = mock[PedigreeScenarioRepository]
    when(pedigreeScenarioRepository.update(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

    val pedigreeService = mock[PedigreeService]
    when(pedigreeService.getCourtCase(scenario.pedigreeId, "", true)).thenReturn(Future.successful(Some(Stubs.courtCaseFull)))

    val service = new PedigreeScenarioServiceImpl(pedigreeScenarioRepository, pedigreeService, Stubs.notificationServiceMock)
    val result = Await.result(service.changeScenarioStatus(scenario, ScenarioStatus.Validated, "", true), duration)

    result mustBe mockResult
    verify(pedigreeScenarioRepository, times(0)).changeStatus(scenario._id, ScenarioStatus.Validated)
  }

  "solve all notifications when status change is successful" in {
    val mockResult = Right(scenario._id)

    val pedigreeScenarioRepository = mock[PedigreeScenarioRepository]
    when(pedigreeScenarioRepository.changeStatus(any[MongoId], any[ScenarioStatus])).thenReturn(Future.successful(mockResult))
    when(pedigreeScenarioRepository.update(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

    val pedigreeService = mock[PedigreeService]
    when(pedigreeService.getPedigree(scenario.pedigreeId)).thenReturn(Future.successful(Some(pedigree)))

    val notificationService = mock[NotificationService]

    val service = new PedigreeScenarioServiceImpl(pedigreeScenarioRepository, pedigreeService, notificationService)
    Await.result(service.changeScenarioStatus(scenario, ScenarioStatus.Deleted, "", true), duration)

    // Thread sleep porque hay un future on success
    Thread.sleep(1000)

    verify(notificationService).solve(pedigree.pedigreeMetaData.assignee, PedigreeLRInfo(scenario.pedigreeId, 55, scenario.name))
  }

  "not solve notifications when status change fails" in {
    val mockResult = Left("Error")

    val pedigreeScenarioRepository = mock[PedigreeScenarioRepository]
    when(pedigreeScenarioRepository.update(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

    val pedigreeService = mock[PedigreeService]
    when(pedigreeService.getPedigree(scenario.pedigreeId)).thenReturn(Future.successful(Some(pedigree)))

    val notificationService = mock[NotificationService]

    val service = new PedigreeScenarioServiceImpl(pedigreeScenarioRepository, pedigreeService, notificationService)
    Await.result(service.changeScenarioStatus(scenario, ScenarioStatus.Deleted, "", true), duration)

    // Thread sleep porque hay un future on success
    Thread.sleep(1000)

    verify(notificationService, times(0)).solve(Stubs.courtCaseFull.assignee, PedigreeLRInfo(scenario.pedigreeId, 55, scenario.name))
  }

  "validate a scenario and validate the pedigree" in {
    val mockResult = Right(scenario._id)

    val pedigreeScenarioRepository = mock[PedigreeScenarioRepository]
    when(pedigreeScenarioRepository.changeStatus(any[MongoId], any[ScenarioStatus])).thenReturn(Future.successful(mockResult))
    when(pedigreeScenarioRepository.update(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

    val pedigreeService = mock[PedigreeService]
    when(pedigreeService.changePedigreeStatus(any[Long], any[PedigreeStatus], any[String], any[Boolean])).thenReturn(Future.successful(Right(scenario.pedigreeId)))

    val service = new PedigreeScenarioServiceImpl(pedigreeScenarioRepository, pedigreeService, Stubs.notificationServiceMock)

    val result = Await.result(service.changeScenarioStatus(scenario, ScenarioStatus.Validated, "", true), duration)

    result mustBe mockResult
  }
}