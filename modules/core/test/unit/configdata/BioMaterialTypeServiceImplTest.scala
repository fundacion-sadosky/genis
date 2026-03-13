package unit.configdata

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.{verify, when}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

import configdata.{BioMaterialType, BioMaterialTypeRepository, BioMaterialTypeServiceImpl}
import types.AlphanumericId

class BioMaterialTypeServiceImplTest extends AnyWordSpec with Matchers with MockitoSugar:

  private val timeout = 2.seconds

  private val bmt = BioMaterialType(AlphanumericId("BLOOD"), "Sangre", Some("Muestra"))

  "BioMaterialTypeServiceImpl" must {
    "delegate list() to repository" in {
      val repo = mock[BioMaterialTypeRepository]
      when(repo.list()).thenReturn(Future.successful(Seq(bmt)))
      val service = new BioMaterialTypeServiceImpl(repo)

      val result = Await.result(service.list(), timeout)

      result mustBe Seq(bmt)
      verify(repo).list()
    }

    "delegate insert() to repository" in {
      val repo = mock[BioMaterialTypeRepository]
      when(repo.insert(bmt)).thenReturn(Future.successful(1))
      val service = new BioMaterialTypeServiceImpl(repo)

      val result = Await.result(service.insert(bmt), timeout)

      result mustBe 1
      verify(repo).insert(bmt)
    }

    "delegate update() to repository" in {
      val repo = mock[BioMaterialTypeRepository]
      when(repo.update(bmt)).thenReturn(Future.successful(1))
      val service = new BioMaterialTypeServiceImpl(repo)

      val result = Await.result(service.update(bmt), timeout)

      result mustBe 1
      verify(repo).update(bmt)
    }

    "delegate delete() to repository" in {
      val repo = mock[BioMaterialTypeRepository]
      when(repo.delete("BLOOD")).thenReturn(Future.successful(1))
      val service = new BioMaterialTypeServiceImpl(repo)

      val result = Await.result(service.delete("BLOOD"), timeout)

      result mustBe 1
      verify(repo).delete("BLOOD")
    }
  }
