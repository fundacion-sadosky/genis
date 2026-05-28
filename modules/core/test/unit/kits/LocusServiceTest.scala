package kits

import org.mockito.Mockito.{verify, when, never}
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import pedigree.MutationService
import services.{CacheService, LocusCacheKey}
import fixtures.{LocusFixtures, StubCacheService}

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class LocusServiceTest extends AnyWordSpec with Matchers with MockitoSugar:

  val duration: Duration = Duration(10, SECONDS)

  val fullLocus: FullLocus = LocusFixtures.fullLocus1
  val id: String = fullLocus.locus.id
  val errorMsg: String = "Error message"

  private def mockMutationService(): MutationService =
    val ms = mock[MutationService]
    when(ms.addLocus(any[FullLocus])).thenReturn(Future.successful(Right(())))
    ms

  "LocusServiceImpl" must {

    "list full locus" in {
      val repo = mock[LocusRepository]
      when(repo.listFull()).thenReturn(Future.successful(LocusFixtures.fullLocusList))
      val cache = new StubCacheService
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      val result = Await.result(service.listFull(), duration)

      result mustBe LocusFixtures.fullLocusList
    }

    "list locus (returns only Locus from listFull)" in {
      val repo = mock[LocusRepository]
      when(repo.listFull()).thenReturn(Future.successful(LocusFixtures.fullLocusList))
      val cache = new StubCacheService
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      val result = Await.result(service.list(), duration)

      result mustBe LocusFixtures.fullLocusList.map(_.locus)
    }

    "add locus successfully" in {
      val repo = mock[LocusRepository]
      when(repo.add(any[FullLocus])).thenReturn(Future.successful(Right(id)))
      val cache = new StubCacheService
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      val result = Await.result(service.add(fullLocus), duration)

      result mustBe Right(id)
    }

    "add locus with error" in {
      val repo = mock[LocusRepository]
      when(repo.add(any[FullLocus])).thenReturn(Future.successful(Left(errorMsg)))
      val cache = new StubCacheService
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      val result = Await.result(service.add(fullLocus), duration)

      result mustBe Left(errorMsg)
    }

    "update locus successfully" in {
      val repo = mock[LocusRepository]
      when(repo.update(any[FullLocus])).thenReturn(Future.successful(Right(())))
      val cache = new StubCacheService
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      val result = Await.result(service.update(fullLocus), duration)

      result mustBe Right(())
    }

    "update locus with error" in {
      val repo = mock[LocusRepository]
      when(repo.update(any[FullLocus])).thenReturn(Future.successful(Left(errorMsg)))
      val cache = new StubCacheService
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      val result = Await.result(service.update(fullLocus), duration)

      result mustBe Left(errorMsg)
    }

    "delete locus successfully" in {
      val repo = mock[LocusRepository]
      when(repo.delete(any[String])).thenReturn(Future.successful(Right(id)))
      val cache = new StubCacheService
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      val result = Await.result(service.delete(id), duration)

      result mustBe Right(id)
    }

    "delete locus with error" in {
      val repo = mock[LocusRepository]
      when(repo.delete(any[String])).thenReturn(Future.successful(Left(errorMsg)))
      val cache = new StubCacheService
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      val result = Await.result(service.delete(id), duration)

      result mustBe Left(errorMsg)
    }

    "clean cache after successful add" in {
      val repo = mock[LocusRepository]
      when(repo.add(any[FullLocus])).thenReturn(Future.successful(Right(id)))
      val cache = mock[CacheService]
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      Await.result(service.add(fullLocus), duration)

      verify(cache).pop(LocusCacheKey)
    }

    "not clean cache after failed add" in {
      val repo = mock[LocusRepository]
      when(repo.add(any[FullLocus])).thenReturn(Future.successful(Left(errorMsg)))
      val cache = mock[CacheService]
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      Await.result(service.add(fullLocus), duration)

      verify(cache, never()).pop(LocusCacheKey)
    }

    "clean cache after successful update" in {
      val repo = mock[LocusRepository]
      when(repo.update(any[FullLocus])).thenReturn(Future.successful(Right(())))
      val cache = mock[CacheService]
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      Await.result(service.update(fullLocus), duration)

      verify(cache).pop(LocusCacheKey)
    }

    "not clean cache after failed update" in {
      val repo = mock[LocusRepository]
      when(repo.update(any[FullLocus])).thenReturn(Future.successful(Left(errorMsg)))
      val cache = mock[CacheService]
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      Await.result(service.update(fullLocus), duration)

      verify(cache, never()).pop(LocusCacheKey)
    }

    "clean cache after successful delete" in {
      val repo = mock[LocusRepository]
      when(repo.delete(any[String])).thenReturn(Future.successful(Right(id)))
      val cache = mock[CacheService]
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      Await.result(service.delete(id), duration)

      verify(cache).pop(LocusCacheKey)
    }

    "not clean cache after failed delete" in {
      val repo = mock[LocusRepository]
      when(repo.delete(any[String])).thenReturn(Future.successful(Left(errorMsg)))
      val cache = mock[CacheService]
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      Await.result(service.delete(id), duration)

      verify(cache, never()).pop(LocusCacheKey)
    }

    "call mutationService.addLocus when analysisType is 1" in {
      val repo = mock[LocusRepository]
      when(repo.add(any[FullLocus])).thenReturn(Future.successful(Right(id)))
      val mutationService = mockMutationService()
      val cache = new StubCacheService
      val service = new LocusServiceImpl(cache, repo, mutationService)

      Await.result(service.add(fullLocus), duration)

      // fullLocus has analysisType == 1
      verify(mutationService).addLocus(any[FullLocus])
    }

    "not call mutationService.addLocus when analysisType is not 1" in {
      val repo = mock[LocusRepository]
      val nonAutosomal = LocusFixtures.fullLocus3NonAutosomal
      when(repo.add(any[FullLocus])).thenReturn(Future.successful(Right(nonAutosomal.locus.id)))
      val mutationService = mockMutationService()
      val cache = new StubCacheService
      val service = new LocusServiceImpl(cache, repo, mutationService)

      Await.result(service.add(nonAutosomal), duration)

      verify(mutationService, never()).addLocus(any[FullLocus])
    }

    "get locus by analysis type name" in {
      val loci = LocusFixtures.fullLocusList.map(_.locus)
      val repo = mock[LocusRepository]
      when(repo.getLocusByAnalysisTypeName(any[String])).thenReturn(Future.successful(loci))
      val cache = new StubCacheService
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      val result = Await.result(service.getLocusByAnalysisTypeName("Autosomal"), duration)

      result mustBe loci.map(_.id)
    }

    "get locus by analysis type" in {
      val loci = LocusFixtures.fullLocusList.map(_.locus)
      val repo = mock[LocusRepository]
      when(repo.getLocusByAnalysisType(any[Int])).thenReturn(Future.successful(loci))
      val cache = new StubCacheService
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      val result = Await.result(service.getLocusByAnalysisType(1), duration)

      result mustBe loci.map(_.id)
    }

    "locusRangeMap returns map with AleleRange using defaults" in {
      val locusNoRange = LocusFixtures.locus2.copy(minAlleleValue = None, maxAlleleValue = None)
      val locusWithRange = LocusFixtures.locus1 // has min=5, max=30
      val repo = mock[LocusRepository]
      when(repo.listFull()).thenReturn(Future.successful(Seq(
        FullLocus(locusWithRange, List.empty, List.empty),
        FullLocus(locusNoRange, List.empty, List.empty)
      )))
      val cache = new StubCacheService
      val service = new LocusServiceImpl(cache, repo, mockMutationService())

      val result = Await.result(service.locusRangeMap(), duration)

      result(locusWithRange.id) mustBe AleleRange(BigDecimal(5), BigDecimal(30))
      result(locusNoRange.id) mustBe AleleRange(BigDecimal(0), BigDecimal(99))
    }
  }
