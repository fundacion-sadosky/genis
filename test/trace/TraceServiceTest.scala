package trace

import java.util.Date

import configdata.{CategoryAssociation, CategoryConfiguration, CategoryService}
import kits.AnalysisTypeService
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import specs.PdgSpec
import types.{AlphanumericId, MongoId, SampleCode}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import stubs.Stubs

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import org.mockito.Mockito.verify
import pedigree.PedigreeDataRepository
import profile.ProfileRepository

class TraceServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val traceInfo: TraceInfo = HitInfo("123456", SampleCode("AR-B-SHDG-1000"), "userId", 1)
  val trace: Trace = Trace(SampleCode("AR-B-SHDG-1234"), "userId", new Date(), traceInfo)
  val traceSearch = TraceSearch(0, 30, SampleCode("AR-B-SHDG-1"), "pdg", false)

  "search traces" in {
    val mockResult = IndexedSeq(trace)

    val traceRepository = mock[TraceRepository]
    when(traceRepository.search(any[TraceSearch])).thenReturn(Future.successful(mockResult))

    val service = new TraceServiceImpl(traceRepository, null, null, null, null)

    val result = Await.result(service.search(traceSearch), duration)

    result mustBe mockResult
  }

  "count notifications" in {
    val traceRepository = mock[TraceRepository]
    when(traceRepository.count(any[TraceSearch])).thenReturn(Future.successful(5))

    val service = new TraceServiceImpl(traceRepository, null, null, null, null)

    val result = Await.result(service.count(traceSearch), duration)

    result mustBe 5
  }

  "add a trace" in {
    val mockResult = Right(1l)

    val traceRepository = mock[TraceRepository]
    when(traceRepository.add(any[Trace])).thenReturn(Future.successful(mockResult))

    val service = new TraceServiceImpl(traceRepository, null, null, null, null)

    val result = Await.result(service.add(trace), duration)

    result mustBe mockResult
  }

  "add a trace and fail safely E0121" in {
    val service = new TraceServiceImpl(new MockFailureTraceRepository(), null, null, null, null)

    val result = Await.result(service.add(trace), duration)

    result.isLeft mustBe true
    result mustBe Left("E0121: Error realizando traza de tipo hit para el perfil AR-B-SHDG-1234.")
  }

  "get full description for analysis info" in {
    val traceInfo = AnalysisInfo(Seq.empty, None, Some(1), CategoryConfiguration("", "", "K", "2", 2))
    val trace = Trace(SampleCode("AR-B-SHDG-1234"), "userId", new Date(), traceInfo)

    val analysisTypeService = mock[AnalysisTypeService]
    when(analysisTypeService.getById(any[Int])).thenReturn(Future.successful(Stubs.analysisTypes.headOption))

    val traceRepository = mock[TraceRepository]
    when(traceRepository.getById(any[Long])).thenReturn(Future.successful(Some(trace)))

    val service = new TraceServiceImpl(traceRepository, null, analysisTypeService, null, null)

    val result = Await.result(service.getFullDescription(1), duration)

    result.length mustBe >(0)
    verify(analysisTypeService).getById(1)
  }

  "get full description for match process info" in {
    val traceInfo = MatchProcessInfo(Stubs.listOfMinimumStringencies)
    val trace = Trace(SampleCode("AR-B-SHDG-1234"), "userId", new Date(), traceInfo)

    val categoryService = mock[CategoryService]
    when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Some(Stubs.fullCatA1))

    val analysisTypeService = mock[AnalysisTypeService]
    when(analysisTypeService.getById(any[Int])).thenReturn(Future.successful(Stubs.analysisTypes.headOption))

    val traceRepository = mock[TraceRepository]
    when(traceRepository.getById(any[Long])).thenReturn(Future.successful(Some(trace)))

    val service = new TraceServiceImpl(traceRepository, categoryService, analysisTypeService, null, null)

    val result = Await.result(service.getFullDescription(1), duration)

    result.length mustBe >(0)
    verify(analysisTypeService, times(2)).getById(1)
    verify(categoryService).getCategory(AlphanumericId("MULTIPLE"))
    verify(categoryService).getCategory(AlphanumericId("SOSPECHOSO"))
  }

  "get full description for pedigree match process info" in {
    val traceInfo = PedigreeMatchProcessInfo(Stubs.listOfMinimumStringencies)
    val trace = Trace(SampleCode("AR-B-SHDG-1234"), "userId", new Date(), traceInfo)

    val categoryService = mock[CategoryService]
    when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Some(Stubs.fullCatA1))

    val analysisTypeService = mock[AnalysisTypeService]
    when(analysisTypeService.getById(any[Int])).thenReturn(Future.successful(Stubs.analysisTypes.headOption))

    val traceRepository = mock[TraceRepository]
    when(traceRepository.getById(any[Long])).thenReturn(Future.successful(Some(trace)))

    val service = new TraceServiceImpl(traceRepository, categoryService, analysisTypeService, null, null)

    val result = Await.result(service.getFullDescription(1), duration)

    result.length mustBe >(0)
    verify(analysisTypeService, times(2)).getById(1)
    verify(categoryService).getCategory(AlphanumericId("MULTIPLE"))
    verify(categoryService).getCategory(AlphanumericId("SOSPECHOSO"))
  }

  "get full description for match action info" in {
    val traceInfo = HitInfo("32423jwdcs", Stubs.sampleCode, "pdg", 1)
    val trace = Trace(SampleCode("AR-B-SHDG-1234"), "userId", new Date(), traceInfo)

    val categoryService = mock[CategoryService]
    when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Some(Stubs.fullCatA1))

    val analysisTypeService = mock[AnalysisTypeService]
    when(analysisTypeService.getById(any[Int])).thenReturn(Future.successful(Stubs.analysisTypes.headOption))

    val mockProfile = Stubs.newProfile
    val profileRepository = mock[ProfileRepository]
    when(profileRepository.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(mockProfile)))

    val traceRepository = mock[TraceRepository]
    when(traceRepository.getById(any[Long])).thenReturn(Future.successful(Some(trace)))

    val service = new TraceServiceImpl(traceRepository, categoryService, analysisTypeService, profileRepository, null)

    val result = Await.result(service.getFullDescription(1), duration)

    result.length mustBe >(0)
    verify(analysisTypeService).getById(1)
    verify(categoryService).getCategory(mockProfile.categoryId)
    verify(profileRepository).findByCode(Stubs.sampleCode)
  }

  "get full description for pedigree match action info" in {
    val traceInfo = PedigreeDiscardInfo("32423jwdcs", 15l, "pdg", 1)
    val trace = Trace(SampleCode("AR-B-SHDG-1234"), "userId", new Date(), traceInfo)


    val analysisTypeService = mock[AnalysisTypeService]
    when(analysisTypeService.getById(any[Int])).thenReturn(Future.successful(Stubs.analysisTypes.headOption))

    val mockPedigree = Stubs.pedigree
    val pedigreeDataRepository = mock[PedigreeDataRepository]
    when(pedigreeDataRepository.getPedigreeMetaData(any[Long])).thenReturn(Future.successful(Some(mockPedigree)))

    val traceRepository = mock[TraceRepository]
    when(traceRepository.getById(any[Long])).thenReturn(Future.successful(Some(trace)))

    val service = new TraceServiceImpl(traceRepository, null, analysisTypeService, null, pedigreeDataRepository)

    val result = Await.result(service.getFullDescription(1), duration)

    result.length mustBe >(0)
    verify(analysisTypeService).getById(1)
    verify(pedigreeDataRepository).getPedigreeMetaData(traceInfo.pedigree)
  }

  "get full description for association info" in {
    val traceInfo = AssociationInfo(SampleCode("AR-B-SHDG-1235"), "pdg", Seq(CategoryAssociation(1, AlphanumericId("VICTIMA"), 0)))
    val trace = Trace(SampleCode("AR-B-SHDG-1234"), "userId", new Date(), traceInfo)

    val categoryService = mock[CategoryService]
    when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Some(Stubs.fullCatA1))

    val mockProfile = Stubs.newProfile
    val profileRepository = mock[ProfileRepository]
    when(profileRepository.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(mockProfile)))

    val traceRepository = mock[TraceRepository]
    when(traceRepository.getById(any[Long])).thenReturn(Future.successful(Some(trace)))

    val service = new TraceServiceImpl(traceRepository, categoryService, null, profileRepository, null)

    val result = Await.result(service.getFullDescription(1), duration)

    result.length mustBe >(0)
    verify(categoryService).getCategory(AlphanumericId("VICTIMA"))
    verify(profileRepository).findByCode(SampleCode("AR-B-SHDG-1235"))
  }

  "get full description for invalid match" in {
    val trace = Trace(SampleCode("AR-B-SHDG-1234"), "userId", new Date(), ProfileDataInfo)

    val traceRepository = mock[TraceRepository]
    when(traceRepository.getById(any[Long])).thenReturn(Future.successful(Some(trace)))

    val service = new TraceServiceImpl(traceRepository, null, null, null, null)

    val result = Await.result(service.getFullDescription(1), duration)

    result.length mustBe 0
  }

}

class MockFailureTraceRepository extends TraceRepository {
  override def add(trace: Trace): Future[Either[String, Long]] = Future {
    throw new Exception()
  }
  override def count(traceSearch: TraceSearch): Future[Int] = Future.successful(0)
  override def search(traceSearch: TraceSearch): Future[IndexedSeq[Trace]] = Future.successful(IndexedSeq())

  override def getById(id: Long): Future[Option[Trace]] = Future.successful(Some(Stubs.trace))
  override def searchPedigree(traceSearch: TraceSearchPedigree): Future[IndexedSeq[TracePedigree]] = Future.successful(IndexedSeq.empty)
  override def countPedigree(traceSearch: TraceSearchPedigree): Future[Int] =  Future.successful(0)
  override def addTracePedigree(trace: TracePedigree): Future[Either[String, Long]]  = Future.successful(Right(0l))
}