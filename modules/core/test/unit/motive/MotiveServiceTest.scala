package unit.motive

import motive.*
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

class MotiveServiceTest extends AnyWordSpec with Matchers with MockitoSugar {

  val duration = Duration(10, SECONDS)

  "MotiveServiceImpl" must {

    "return motives from repository" in {
      val repo    = mock[SlickMotiveRepository]
      val service = new MotiveServiceImpl(repo)
      when(repo.getMotives(1L, true)).thenReturn(Future.successful(List(Motive(1, 1, "hola", true))))

      val result = Await.result(service.getMotives(1L, true), duration)
      result.head.description mustBe "hola"
    }

    "return motive types from repository" in {
      val repo    = mock[SlickMotiveRepository]
      val service = new MotiveServiceImpl(repo)
      when(repo.getMotivesTypes()).thenReturn(Future.successful(List(MotiveType(1, "Causa penal"))))

      val result = Await.result(service.getMotivesTypes(), duration)
      result.head.description mustBe "Causa penal"
    }

    "insert a motive via repository" in {
      val repo    = mock[SlickMotiveRepository]
      val service = new MotiveServiceImpl(repo)
      val mot     = Motive(0, 1, "nuevo", false)
      when(repo.insert(mot)).thenReturn(Future.successful(Right(42L)))

      val result = Await.result(service.insert(mot), duration)
      result mustBe Right(42L)
    }

    "update a motive via repository" in {
      val repo    = mock[SlickMotiveRepository]
      val service = new MotiveServiceImpl(repo)
      val mot     = Motive(1, 1, "actualizado", false)
      when(repo.update(mot)).thenReturn(Future.successful(Right(())))

      val result = Await.result(service.update(mot), duration)
      result mustBe Right(())
    }

    "delete a motive logically via repository" in {
      val repo    = mock[SlickMotiveRepository]
      val service = new MotiveServiceImpl(repo)
      when(repo.deleteLogicalMotiveById(1L)).thenReturn(Future.successful(Right(())))

      val result = Await.result(service.deleteMotiveById(1L), duration)
      result mustBe Right(())
    }
  }
}
