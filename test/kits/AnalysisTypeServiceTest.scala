package kits

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import specs.PdgSpec
import stubs.Stubs

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import org.mockito.Matchers.any

class AnalysisTypeServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  "An AnalysisTypeService" must {
    "list all analysis types" in {
      val analysisTypeRepository = mock[AnalysisTypeRepository]
      when(analysisTypeRepository.list()).thenReturn(Future.successful(Stubs.analysisTypes))

      val service = new AnalysisTypeServiceImpl(Stubs.cacheServiceMock, analysisTypeRepository)

      val result = Await.result(service.list(), duration)

      result mustBe Stubs.analysisTypes
    }

    "get analysis type by name" in {
      val analysisTypeRepository = mock[AnalysisTypeRepository]
      when(analysisTypeRepository.getByName(any[String])).thenReturn(Future.successful(Stubs.analysisTypes.headOption))

      val service = new AnalysisTypeServiceImpl(null, analysisTypeRepository)

      val result = Await.result(service.getByName("Autosomal"), duration)

      result mustBe Stubs.analysisTypes.headOption
    }

    "get analysis type by id" in {
      val analysisTypeRepository = mock[AnalysisTypeRepository]
      when(analysisTypeRepository.getById(any[Int])).thenReturn(Future.successful(Stubs.analysisTypes.headOption))

      val service = new AnalysisTypeServiceImpl(null, analysisTypeRepository)

      val result = Await.result(service.getById(1), duration)

      result mustBe Stubs.analysisTypes.headOption
    }
  }

}
