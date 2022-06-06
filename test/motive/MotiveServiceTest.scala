package motive

import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import specs.PdgSpec

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

class MotiveServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  "MotiveServiceTest" must {

    "get motive ok" in {
      val repo = mock[SlickMotiveRepository]
      val service = new MotiveServiceImpl(repo)
      when(repo.getMotives(1L,true)).thenReturn(Future.successful(List(Motive(1,1,"hola",true))))

      val motive = Await.result(service.getMotives(1L,true), duration)
      motive.head.description mustBe "hola"
    }

    "get motive types ok" in {
      val repo = mock[SlickMotiveRepository]
      val service = new MotiveServiceImpl(repo)
      when(repo.getMotivesTypes()).thenReturn(Future.successful(List(MotiveType(1,"hola"))))

      val motive = Await.result(service.getMotivesTypes(), duration)
      motive.head.description mustBe "hola"
    }

    "get insert motive ok" in {
      val repo = mock[SlickMotiveRepository]
      val service = new MotiveServiceImpl(repo)
      val mot = Motive(1,1,"hola",true)
      when(repo.insert(mot)).thenReturn(Future.successful(Right(1L)))
      val result = Await.result(service.insert(mot), duration)
      result.isRight mustBe true
    }

    "get update motive ok" in {
      val repo = mock[SlickMotiveRepository]
      val service = new MotiveServiceImpl(repo)
      val mot = Motive(1,1,"hola",true)
      when(repo.update(mot)).thenReturn(Future.successful(Right(())))
      val result = Await.result(service.update(mot), duration)
      result.isRight mustBe true
    }
    "get delete motive ok" in {
      val repo = mock[SlickMotiveRepository]
      val service = new MotiveServiceImpl(repo)
      when(repo.deleteLogicalMotiveById(1L)).thenReturn(Future.successful(Right(())))
      val result = Await.result(service.deleteMotiveById(1L), duration)
      result.isRight mustBe true
    }
  }

}