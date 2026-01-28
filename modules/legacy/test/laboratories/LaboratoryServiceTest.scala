package laboratories

import scala.Right
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS

import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar

import configdata.Country
import configdata.Province
import javax.inject.Inject
import javax.inject.Singleton
import services.CacheService
import specs.PdgSpec
import stubs.Stubs

class LaboratoryServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val lab = Stubs.laboratory
  val seqLab = List(lab)

  "A LaboratoryService" must {
    "add a laboratory" in {
      val mockLaboratoryRepository = mock[LaboratoryRepository]
      when(mockLaboratoryRepository.add(lab)).thenReturn(Future.successful(Right("NME")))
      val target: LaboratoryService = new LaboratoryServiceImpl(mockLaboratoryRepository, "SHDG")

      val result = Await.result(target.add(lab), duration)

      result mustBe Right("NME")
    }

    "list the laboratories" in {
      val mockLaboratoryRepository = mock[LaboratoryRepository]
      when(mockLaboratoryRepository.getAll).thenReturn(Future.successful(seqLab))
      val target: LaboratoryService = new LaboratoryServiceImpl(mockLaboratoryRepository, "SHDG")

      val result = Await.result(target.list, duration)

      result mustBe seqLab
    }

    "get a laboratory" in {
      val mockLaboratoryRepository = mock[LaboratoryRepository]
      when(mockLaboratoryRepository.get("NME")).thenReturn(Future.successful(Some(lab)))
      val target: LaboratoryService = new LaboratoryServiceImpl(mockLaboratoryRepository, "SHDG")

      val result = Await.result(target.get("NME"), duration)

      result mustBe Some(lab)
    }

    "update a laboratory" in {
      val mockLaboratoryRepository = mock[LaboratoryRepository]
      when(mockLaboratoryRepository.update(lab)).thenReturn(Future.successful(Right("NME")))
      val target: LaboratoryService = new LaboratoryServiceImpl(mockLaboratoryRepository, "SHDG")

      val result = Await.result(target.update(lab), duration)

      result mustBe Right("NME")
    }

    "get instance laboratory" in {
      val mockLaboratoryRepository = mock[LaboratoryRepository]
      when(mockLaboratoryRepository.get("SHDG")).thenReturn(Future.successful(Some(Stubs.instanceLaboratory)))
      val target: LaboratoryService = new LaboratoryServiceImpl(mockLaboratoryRepository, "SHDG")

      val result = Await.result(target.get("SHDG"), duration)

      result.get.instance mustBe Some(true)
    }
  }
}