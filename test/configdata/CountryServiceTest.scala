package configdata

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS

import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar

import services.CacheService
import services.Keys
import specs.PdgSpec

class CountryServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val country = Country("AR", "Argentina")
  val province = Province("C", "CABA")

  val seqCountry: Future[Seq[Country]] = Future.successful(List(country))
  val seqProv: Future[Seq[Province]] = Future.successful(List(province))

  "A CountryService" must {

    "list the countries" in {
      val mockLaboratoryRepository = mock[CountryRepository]
      when(mockLaboratoryRepository.getCountries()).thenReturn(seqCountry)

      val mockCacheService = mock[CacheService]
      when(mockCacheService.asyncGetOrElse(Keys.countries)(mockLaboratoryRepository.getCountries)).thenReturn(seqCountry)

      val target: CountryService = new CountryServiceImpl(mockCacheService, mockLaboratoryRepository)

      val result = Await.result(target.listCountries, duration)

      result.head mustBe country
    }

    "list the provinces in a country" in {
      val mockLaboratoryRepository = mock[CountryRepository]
      when(mockLaboratoryRepository.getProvinces("AR")).thenReturn(seqProv)

      val mockCacheService = mock[CacheService]
      when(mockCacheService.asyncGetOrElse(Keys.provinces)(mockLaboratoryRepository.getProvinces("AR"))).thenReturn(seqProv)

      val target: CountryService = new CountryServiceImpl(mockCacheService, mockLaboratoryRepository)

      val result = Await.result(target.listProvinces("AR"), duration)

      result.head mustBe province
    }

  }

}