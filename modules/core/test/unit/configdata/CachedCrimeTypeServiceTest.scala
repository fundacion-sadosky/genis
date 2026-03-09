package unit.configdata

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

import configdata.{CachedCrimeTypeService, Crime, CrimeType, CrimeTypeRepository}
import fixtures.StubCacheService

class CachedCrimeTypeServiceTest extends AnyWordSpec with Matchers with MockitoSugar:

  private val timeout = 2.seconds

  private val ct1 = CrimeType("CT01", "Contra la propiedad", Some("Patrimoniales"),
    Seq(Crime("C01", "Robo", None)))
  private val ct2 = CrimeType("CT02", "Contra las personas", None,
    Seq(Crime("C02", "Lesiones", Some("Lesiones graves"))))

  "CachedCrimeTypeService.list()" must {
    "transform repository Seq into Map keyed by id" in {
      val repo = mock[CrimeTypeRepository]
      when(repo.list()).thenReturn(Future.successful(Seq(ct1, ct2)))
      val cache = new StubCacheService
      val service = new CachedCrimeTypeService(cache, repo)

      val result = Await.result(service.list(), timeout)

      result must have size 2
      result("CT01") mustBe ct1
      result("CT02") mustBe ct2
    }

    "return empty Map when repository returns empty Seq" in {
      val repo = mock[CrimeTypeRepository]
      when(repo.list()).thenReturn(Future.successful(Seq.empty))
      val cache = new StubCacheService
      val service = new CachedCrimeTypeService(cache, repo)

      val result = Await.result(service.list(), timeout)

      result mustBe empty
    }

    "preserve all CrimeType data in map values" in {
      val repo = mock[CrimeTypeRepository]
      when(repo.list()).thenReturn(Future.successful(Seq(ct1)))
      val cache = new StubCacheService
      val service = new CachedCrimeTypeService(cache, repo)

      val result = Await.result(service.list(), timeout)
      val entry = result("CT01")

      entry.name mustBe "Contra la propiedad"
      entry.description mustBe Some("Patrimoniales")
      entry.crimes must have size 1
      entry.crimes.head.id mustBe "C01"
    }
  }
