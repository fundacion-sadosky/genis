package controllers

import kits.AnalysisTypeService
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import specs.PdgSpec
import stubs.Stubs

import scala.concurrent.Future

class AnalysisTypesTest extends PdgSpec with MockitoSugar with Results {

  "AnalysisTypes controller" must {

    "list all analysis types" in {

      val analysisTypeService = mock[AnalysisTypeService]
      when(analysisTypeService.list()).thenReturn(Future.successful(Stubs.analysisTypes))

      val target = new AnalysisTypes(analysisTypeService)
      val result: Future[Result] = target.list().apply(FakeRequest())

      status(result) mustBe OK
    }

  }


}
