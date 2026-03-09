package unit.kits

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.{verify, when}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

import kits.{StrKit, StrKitRepository, StrKitServiceImpl}

class StrKitServiceImplTest extends AnyWordSpec with Matchers with MockitoSugar:

  private val timeout = 2.seconds
  private val kit = StrKit("PP16", "PowerPlex 16", 1, 16, 15)

  "StrKitServiceImpl" must {
    "delegate get() to repository" in {
      val repo = mock[StrKitRepository]
      when(repo.get("PP16")).thenReturn(Future.successful(Some(kit)))
      val service = new StrKitServiceImpl(repo)

      Await.result(service.get("PP16"), timeout) mustBe Some(kit)
      verify(repo).get("PP16")
    }

    "delegate list() to repository" in {
      val repo = mock[StrKitRepository]
      when(repo.list()).thenReturn(Future.successful(Seq(kit)))
      val service = new StrKitServiceImpl(repo)

      Await.result(service.list(), timeout) mustBe Seq(kit)
      verify(repo).list()
    }

    "delegate add() to repository" in {
      val repo = mock[StrKitRepository]
      when(repo.add(kit)).thenReturn(Future.successful(Right("PP16")))
      val service = new StrKitServiceImpl(repo)

      Await.result(service.add(kit), timeout) mustBe Right("PP16")
      verify(repo).add(kit)
    }

    "delegate delete() to repository" in {
      val repo = mock[StrKitRepository]
      when(repo.delete("PP16")).thenReturn(Future.successful(Right("PP16")))
      val service = new StrKitServiceImpl(repo)

      Await.result(service.delete("PP16"), timeout) mustBe Right("PP16")
      verify(repo).delete("PP16")
    }
  }
