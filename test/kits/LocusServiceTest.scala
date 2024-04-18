package kits

import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, _}
import org.scalatest.mock.MockitoSugar
import pedigree.MutationService
import play.api.Play.current
import play.api.db.slick._
import services.{CacheService, Keys}
import specs.PdgSpec
import stubs.Stubs

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


class LocusServiceTest  extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val locus = Stubs.fullLocus.head
  val id = locus.locus.id
  val error = Left("Error message")
  val mutationService = mock[MutationService]
  when(mutationService.addLocus(any[FullLocus])).thenReturn(Future.successful(Left("")))
  "A LocusService" must {

    "list full locus" in {
      val locusRepository = mock[LocusRepository]
      when(locusRepository.listFull()).thenReturn(Future.successful(Stubs.fullLocus))

      val service = new LocusServiceImpl(Stubs.cacheServiceMock, locusRepository,mutationService)

      val result = Await.result(service.listFull(), duration)

      result mustBe Stubs.fullLocus
    }

    "list locus" in {
      val locusRepository = mock[LocusRepository]
      when(locusRepository.listFull()).thenReturn(Future.successful(Stubs.fullLocus))

      val service = new LocusServiceImpl(Stubs.cacheServiceMock, locusRepository,mutationService)

      val result = Await.result(service.list(), duration)

      result mustBe Stubs.fullLocus.map(_.locus)
    }

    "add locus - ok" in {
      val locusRepository = new MockLocusRepository(Right(id), Right(id), false)

      val service = new LocusServiceImpl(Stubs.cacheServiceMock, locusRepository,mutationService)

      val result = Await.result(service.add(locus), duration)

      result mustBe Right(id)
    }

    "update locus - no ok" in {
      val locusRepository = new MockLocusRepository(Right(id), Right(id), false)

      val service = new LocusServiceImpl(Stubs.cacheServiceMock, locusRepository,mutationService)

      val result = Await.result(service.update(locus), duration)

      result.isLeft mustBe true
    }

    "add locus - error" in {
      val locusRepository = new MockLocusRepository(error, Right(id), true)

      val service = new LocusServiceImpl(Stubs.cacheServiceMock, locusRepository,mutationService)

      val result = Await.result(service.add(locus), duration)

      result mustBe error
    }

    "delete locus - ok" in {
      val locusRepository = new MockLocusRepository(Right(id), Right(id), true)

      val service = new LocusServiceImpl(Stubs.cacheServiceMock, locusRepository,mutationService)

      val result = Await.result(service.delete(id), duration)

      result mustBe Right(id)
    }

    "delete locus - error" in {
      val locusRepository = new MockLocusRepository(Right(id), error, true)

      val service = new LocusServiceImpl(Stubs.cacheServiceMock, locusRepository,mutationService)

      val result = Await.result(service.delete(id), duration)

      result mustBe error
    }

    "delete locus - can't be deleted E0693" in {
      val locusRepository = new MockLocusRepository(Right(id), Right(id), false)

      val service = new LocusServiceImpl(Stubs.cacheServiceMock, locusRepository,mutationService)

      val result = Await.result(service.delete(id), duration)

      result.isLeft mustBe true
      result mustBe Left("E0693: No se puede eliminar el marcador LOCUS 1 ya que está incluido en algún kit.")

    }

    "clean cache after add locus" in {
      val cache = mock[CacheService]

      val locusRepository = new MockLocusRepository(Right(id), Right(id), false)

      val service = new LocusServiceImpl(cache, locusRepository,mutationService)

      Await.result(service.add(locus), duration)

      verify(cache).pop(Keys.locus)
    }

    "clean cache after delete locus" in {
      val cache = mock[CacheService]

      val locusRepository = new MockLocusRepository(Right(id), Right(id), true)

      val service = new LocusServiceImpl(cache, locusRepository,mutationService)

      Await.result(service.delete(id), duration)

      verify(cache).pop(Keys.locus)
    }

    "get locus by analysis type name" in {
      val locus = Stubs.fullLocus.map(_.locus)

      val locusRepository = mock[LocusRepository]
      when(locusRepository.getLocusByAnalysisTypeName(any[String])).thenReturn(Future.successful(locus))

      val service = new LocusServiceImpl(Stubs.cacheServiceMock, locusRepository,mutationService)

      val result = Await.result(service.getLocusByAnalysisTypeName("Autosomal"), duration)

      result mustBe locus.map(_.id)
    }

    "get locus by analysis type" in {
      val locus = Stubs.fullLocus.map(_.locus)

      val locusRepository = mock[LocusRepository]
      when(locusRepository.getLocusByAnalysisType(any[Int])).thenReturn(Future.successful(locus))

      val service = new LocusServiceImpl(Stubs.cacheServiceMock, locusRepository,mutationService)

      val result = Await.result(service.getLocusByAnalysisType(1), duration)

      result mustBe locus.map(_.id)
    }

  }

}
