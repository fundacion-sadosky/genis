package pedigrees

import matching.Stringency.{apply => _, _}
import org.bson.types.ObjectId
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import pedigree.{Individual, _}
import specs.PdgSpec
import stubs.Stubs
import types._
import java.util.Date

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, SECONDS}
import org.mockito.Mockito.verify
import scenarios.ScenarioStatus
import services.CacheService
import trace.{TracePedigree, TraceService}

class PedigreeServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val scenario = PedigreeScenario(MongoId(new ObjectId().toString), 55l, "scenario", "descripción", Seq(), ScenarioStatus.Pending, "base")
  val traceService = mock[TraceService]
  when(traceService.addTracePedigree(any[TracePedigree])).thenReturn(Future.successful(Right(1L)))
  "PedigreeService" must {
    "get full pedigree by id" in {
      val mockPedigreeDataRepository = mock[PedigreeDataRepository]
      val mockPedigreeRepository = mock[PedigreeRepository]
      val mockCacheService = mock[CacheService]

      val individuals = Seq(
        Individual(NodeAlias("Node1"), None, None, Sex.Male, None, false, None),
        Individual(NodeAlias("Node2"), None, None, Sex.Female, None, true, None)
      )
      val pedigreeGenogram = PedigreeGenogram(56, "user", individuals, PedigreeStatus.UnderConstruction, None,false,0.5,false,None,"MPI",None,7l)
      val pedigreeMetaData = PedigreeMetaData(56, 1, "PedigreeTest", new Date(), PedigreeStatus.UnderConstruction, "tst-admin")
      val pedigreeDataCreation = PedigreeDataCreation(pedigreeMetaData, None);

      when(mockPedigreeRepository.get(56)).thenReturn(Future.successful(Some(pedigreeGenogram)))
      when(mockPedigreeDataRepository.getPedigreeMetaData(56)).thenReturn(Future.successful(Some(pedigreeDataCreation)))

      val service = new PedigreeServiceImpl(mockPedigreeDataRepository, mockPedigreeRepository, mockCacheService)

      val result = Await.result(service.getPedigree(56), duration)

      result.isDefined mustBe true

      val pedigree = result.get

      pedigree.pedigreeGenogram.get._id mustBe 56
      pedigree.pedigreeGenogram.get.genogram mustBe individuals
      pedigree.pedigreeGenogram.get.status mustBe PedigreeStatus.UnderConstruction

      pedigree.pedigreeMetaData.id mustBe 56
      pedigree.pedigreeMetaData.courtCaseId mustBe 1
      pedigree.pedigreeMetaData.name mustBe "PedigreeTest"
      pedigree.pedigreeMetaData.status mustBe PedigreeStatus.UnderConstruction
    }

    "get pedigree by id non existent" in {
      val mockPedigreeRepository = mock[PedigreeRepository]
      val mockPedigreeDataRepository = mock[PedigreeDataRepository]

      when(mockPedigreeDataRepository.getPedigreeMetaData(56)).thenReturn(Future.successful(None))
      when(mockPedigreeRepository.get(56)).thenReturn(Future.successful(None))
      val mockCacheService = mock[CacheService]

      val service = new PedigreeServiceImpl(mockPedigreeDataRepository, mockPedigreeRepository, mockCacheService,null,null,null,null,null,null,null,null,traceService )

      val result = Await.result(service.getPedigree(56), duration)

      result mustBe None
    }

    "change pedigree status if the transition is valid" in {
      val id = Stubs.pedigree.pedigreeMetaData.id

      val pedigreeDataRepository = mock[PedigreeDataRepository]
      when(pedigreeDataRepository.getPedigreeMetaData(any[Long])).thenReturn(Future.successful(Some(Stubs.pedigree)))
      when(pedigreeDataRepository.changePedigreeStatus(any[Long], any[PedigreeStatus.Value])).thenReturn(Future.successful(Right(id)))

      val pedigreeRepository = mock[PedigreeRepository]
      when(pedigreeRepository.changeStatus(any[Long], any[PedigreeStatus.Value])).thenReturn(Future.successful(Right(id)))

      val mockCacheService = mock[CacheService]

      val service = new PedigreeServiceImpl(pedigreeDataRepository, pedigreeRepository, mockCacheService,null,null,null,null,null,null,null,null,traceService)

      val result = Await.result(service.changePedigreeStatus(id, PedigreeStatus.Active, "pdg", false), duration)

      result mustBe Right(id)
    }

    "revert change pedigree status if postgresql fails" in {
      val id = Stubs.pedigree.pedigreeMetaData.id

      val pedigreeDataRepository = mock[PedigreeDataRepository]
      when(pedigreeDataRepository.getPedigreeMetaData(any[Long])).thenReturn(Future.successful(Some(Stubs.pedigree)))
      when(pedigreeDataRepository.changePedigreeStatus(any[Long], any[PedigreeStatus.Value])).thenReturn(Future.successful(Left("error")))

      val pedigreeRepository = mock[PedigreeRepository]
      when(pedigreeRepository.changeStatus(any[Long], any[PedigreeStatus.Value])).thenReturn(Future.successful(Right(id)))
      val mockCacheService = mock[CacheService]

      val service = new PedigreeServiceImpl(pedigreeDataRepository, pedigreeRepository, mockCacheService)

      val result = Await.result(service.changePedigreeStatus(id, PedigreeStatus.Active, "pdg", false), duration)

      result.isLeft mustBe true
      verify(pedigreeRepository).changeStatus(id,Stubs.pedigree.pedigreeMetaData.status)
    }

    "not change pedigree status if the transition is invalid E0930" in {
      val pedigreeDataRepository = mock[PedigreeDataRepository]
      when(pedigreeDataRepository.getPedigreeMetaData(any[Long])).thenReturn(Future.successful(Some(Stubs.pedigree)))
      val mockCacheService = mock[CacheService]

      val service = new PedigreeServiceImpl(pedigreeDataRepository, null, mockCacheService,null,null,null,null,null,null,null,null,traceService)

      val result = Await.result(service.changePedigreeStatus(12l, PedigreeStatus.UnderConstruction, "pdg", false), duration)

      result.isLeft mustBe true
      result mustBe Left("E0930: La transición de UnderConstruction a UnderConstruction no es válida.")
    }

    "change pedigree status if superuser" in {
      val id = Stubs.pedigree.pedigreeMetaData.id

      val pedigreeDataRepository = mock[PedigreeDataRepository]
      when(pedigreeDataRepository.getPedigreeMetaData(any[Long])).thenReturn(Future.successful(Some(Stubs.pedigree)))
      when(pedigreeDataRepository.changePedigreeStatus(any[Long], any[PedigreeStatus.Value])).thenReturn(Future.successful(Right(id)))

      val pedigreeRepository = mock[PedigreeRepository]
      when(pedigreeRepository.changeStatus(any[Long], any[PedigreeStatus.Value])).thenReturn(Future.successful(Right(id)))
      val mockCacheService = mock[CacheService]

      val service = new PedigreeServiceImpl(pedigreeDataRepository, pedigreeRepository, mockCacheService,null,null,null,null,null,null,null,null,traceService)

      val result = Await.result(service.changePedigreeStatus(12l, PedigreeStatus.Active, "another_user", true), duration)

      result mustBe Right(id)
    }

    "not change pedigree status if user is invalid" in {
      val pedigreeDataRepository = mock[PedigreeDataRepository]
      when(pedigreeDataRepository.getPedigreeMetaData(any[Long])).thenReturn(Future.successful(Some(Stubs.pedigree)))
      val mockCacheService = mock[CacheService]

      val service = new PedigreeServiceImpl(pedigreeDataRepository, null, mockCacheService,null,null,null,null,null,null,null,null,traceService)

      val result = Await.result(service.changePedigreeStatus(12l, PedigreeStatus.Active, "another_user", false), duration)

      result.isLeft mustBe true
      result mustBe Left("E0644: El usuario another_user no tiene permisos para dar la baja.")
    }

    "update genogram when changing pedigree status to active" in {
      val id = Stubs.pedigree.pedigreeMetaData.id

      val pedigreeDataRepository = mock[PedigreeDataRepository]
      when(pedigreeDataRepository.getPedigreeMetaData(any[Long])).thenReturn(Future.successful(Some(Stubs.pedigree)))
      when(pedigreeDataRepository.changePedigreeStatus(any[Long], any[PedigreeStatus.Value])).thenReturn(Future.successful(Right(id)))

      val pedigreeRepository = mock[PedigreeRepository]
      when(pedigreeRepository.changeStatus(any[Long], any[PedigreeStatus.Value])).thenReturn(Future.successful(Right(id)))
      val mockCacheService = mock[CacheService]

      val service = new PedigreeServiceImpl(pedigreeDataRepository, pedigreeRepository, mockCacheService,null,null,null,null,null,null,null,null,traceService)

      val result = Await.result(service.changePedigreeStatus(id, PedigreeStatus.Active, "pdg", false), duration)

      result mustBe Right(id)
    }

    "get none court cases if sample code doesn't exist" in {
      val search = PedigreeSearch(0, 30, "pdg", false, None, Some("AR-SHDG-B-1234"))

      val pedigreeDataRepository = mock[PedigreeDataRepository]
      when(pedigreeDataRepository.getCourtCase(any[Long])).thenReturn(Future.successful(Some(Stubs.courtCase)))

      val pedigreeRepository = mock[PedigreeRepository]
      when(pedigreeRepository.findByProfile(any[String])).thenReturn(Future.successful(Seq.empty))
      val mockCacheService = mock[CacheService]

      val service = new PedigreeServiceImpl(pedigreeDataRepository, pedigreeRepository, mockCacheService)

      val result = Await.result(service.getAllCourtCases(search), duration)

      result.size mustBe 0
    }

    "get court cases by sample code" in {
      val pedigreeId = 12345l

      val search = PedigreeSearch(0, 30, "pdg", false, None, Some("AR-B-SHDG-1234"))
      val mockResult = Seq(Stubs.courtCaseModelView, Stubs.courtCaseModelView)

      val pedigreeDataRepository = mock[PedigreeDataRepository]
      when(pedigreeDataRepository.getPedigrees(any[CourtCasePedigreeSearch])).thenReturn(Future.successful(Nil))
      when(pedigreeDataRepository.getCourtCase(any[Long])).thenReturn(Future.successful(Some(Stubs.courtCase)))
      when(pedigreeDataRepository.getAllCourtCases(any[Option[Seq[Long]]], any[PedigreeSearch])).thenReturn(Future.successful(mockResult))
      val pedigreeRepository = mock[PedigreeRepository]
      when(pedigreeRepository.findByProfile(any[String])).thenReturn(Future.successful(Seq(pedigreeId)))
      val mockCacheService = mock[CacheService]
      val mockPedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      when(mockPedigreeMatchesRepository.numberOfPendingMatches(any[Long])).thenReturn(Future.successful(0))
      val service = new PedigreeServiceImpl(pedigreeDataRepository, pedigreeRepository, mockCacheService,null,null,null,null,mockPedigreeMatchesRepository)

      val result = Await.result(service.getAllCourtCases(search), duration)

      result mustBe mockResult
      verify(pedigreeDataRepository).getAllCourtCases(Some(Seq(pedigreeId)), search)
    }

    "get court cases" in {
      val search = PedigreeSearch(0, 30, "pdg", false)
      val mockResult = Seq(Stubs.courtCaseModelView, Stubs.courtCaseModelView)

      val pedigreeDataRepository = mock[PedigreeDataRepository]
      when(pedigreeDataRepository.getPedigrees(any[CourtCasePedigreeSearch])).thenReturn(Future.successful(Nil))
      when(pedigreeDataRepository.getCourtCase(any[Long])).thenReturn(Future.successful(Some(Stubs.courtCase)))
      when(pedigreeDataRepository.getAllCourtCases(any[Option[Seq[Long]]], any[PedigreeSearch])).thenReturn(Future.successful(mockResult))
      val mockCacheService = mock[CacheService]
      val mockPedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      when(mockPedigreeMatchesRepository.numberOfPendingMatches(any[Long])).thenReturn(Future.successful(0))
      val service = new PedigreeServiceImpl(pedigreeDataRepository, null, mockCacheService,null,null,null,null,mockPedigreeMatchesRepository)

      val result = Await.result(service.getAllCourtCases(search), duration)

      result mustBe mockResult
    }

    "count 0 court cases if sample code doesn't exist" in {
      val search = PedigreeSearch(0, 30, "pdg", false, None, Some("AR-SHDG-B-1234"))

      val pedigreeDataRepository = mock[PedigreeDataRepository]
      when(pedigreeDataRepository.getCourtCase(any[Long])).thenReturn(Future.successful(Some(Stubs.courtCase)))

      val pedigreeRepository = mock[PedigreeRepository]
      when(pedigreeRepository.findByProfile(any[String])).thenReturn(Future.successful(Seq.empty))
      val mockCacheService = mock[CacheService]

      val service = new PedigreeServiceImpl(pedigreeDataRepository, pedigreeRepository, mockCacheService)

      val result = Await.result(service.getTotalCourtCases(search), duration)

      result mustBe 0
    }

    "count court cases by sample code" in {
      val pedigreeId = 12345l

      val search = PedigreeSearch(0, 30, "pdg", false, None, Some("AR-B-SHDG-1234"))

      val pedigreeDataRepository = mock[PedigreeDataRepository]
      when(pedigreeDataRepository.getCourtCase(any[Long])).thenReturn(Future.successful(Some(Stubs.courtCase)))
      when(pedigreeDataRepository.getTotalCourtCases(any[Option[Seq[Long]]], any[PedigreeSearch])).thenReturn(Future.successful(2))

      val pedigreeRepository = mock[PedigreeRepository]
      when(pedigreeRepository.findByProfile(any[String])).thenReturn(Future.successful(Seq(pedigreeId)))
      val mockCacheService = mock[CacheService]

      val service = new PedigreeServiceImpl(pedigreeDataRepository, pedigreeRepository, mockCacheService)

      val result = Await.result(service.getTotalCourtCases(search), duration)

      result mustBe 2
      verify(pedigreeDataRepository).getTotalCourtCases(Some(Seq(pedigreeId)), search)
    }

    "count court cases" in {
      val search = PedigreeSearch(0, 30, "pdg", false)

      val pedigreeDataRepository = mock[PedigreeDataRepository]
      when(pedigreeDataRepository.getCourtCase(any[Long])).thenReturn(Future.successful(Some(Stubs.courtCase)))
      when(pedigreeDataRepository.getTotalCourtCases(any[Option[Seq[Long]]], any[PedigreeSearch])).thenReturn(Future.successful(2))
      val mockCacheService = mock[CacheService]

      val service = new PedigreeServiceImpl(pedigreeDataRepository, null, mockCacheService)

      val result = Await.result(service.getTotalCourtCases(search), duration)

      result mustBe 2
    }
  }
}