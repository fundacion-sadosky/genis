package motive

import controllers.MotiveController
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.http.Status.{OK}
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import specs.PdgSpec

import scala.concurrent.Future

class MotiveControllerTest extends PdgSpec with MockitoSugar with Results {

  "MotiveControllerTest" must {

    "get motivetypes" in {
      val motiveServiceImpl = mock[MotiveServiceImpl]

      when(motiveServiceImpl.getMotivesTypes()).thenReturn(Future.successful(List(MotiveType(1,"hola"))))

      val target = new MotiveController(motiveServiceImpl)

      val request = FakeRequest()

      val result: Future[Result] = target.getMotivesTypes.apply(request)
      status(result) mustBe OK
    }

    "get motives" in {
      val motiveServiceImpl = mock[MotiveServiceImpl]

      when(motiveServiceImpl.getMotives(1L,true)).thenReturn(Future.successful(List(Motive(1,1,"hola",true))))

      val target = new MotiveController(motiveServiceImpl)

      val request = FakeRequest()

      val result: Future[Result] = target.getMotives(1L,true).apply(request)
      status(result) mustBe OK
    }

    "insert motive" in {
      val motiveServiceImpl = mock[MotiveServiceImpl]

      val mot = Motive(1,1,"hola",true)
      when(motiveServiceImpl.insert(mot)).thenReturn(Future.successful(Right(1L)))

      val target = new MotiveController(motiveServiceImpl)

      val jsRequest = Json.obj("id" -> 1, "motiveType" -> 1, "description" -> "hola","freeText" -> true)

      val request = FakeRequest().withBody(jsRequest)

      val result: Future[Result] = target.insert().apply(request)
      status(result) mustBe OK
    }

    "update motive" in {
      val motiveServiceImpl = mock[MotiveServiceImpl]

      val mot = Motive(1,1,"hola",true)
      when(motiveServiceImpl.update(mot)).thenReturn(Future.successful(Right(())))

      val target = new MotiveController(motiveServiceImpl)

      val jsRequest = Json.obj("id" -> 1, "motiveType" -> 1, "description" -> "hola","freeText" -> true)

      val request = FakeRequest().withBody(jsRequest)

      val result: Future[Result] = target.update().apply(request)
      status(result) mustBe OK
    }


    "delete motive" in {
      val motiveServiceImpl = mock[MotiveServiceImpl]

      when(motiveServiceImpl.deleteMotiveById(1L)).thenReturn(Future.successful(Right(())))

      val target = new MotiveController(motiveServiceImpl)


      val request = FakeRequest()

      val result: Future[Result] = target.deleteMotiveById(1L).apply(request)
      status(result) mustBe OK
    }
  }
}
