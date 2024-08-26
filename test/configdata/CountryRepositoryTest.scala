package configdata

import specs.PdgSpec
import scala.concurrent.Await
import laboratories.SlickLaboratoryRepository
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS

class CountryRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)
  
  "A Country Repository" must{
    "list the countries" in {
      val countryRepo = new SlickCountryRepository
      
      val result = Await.result(countryRepo.getCountries, duration)

      result.head.code mustBe "AR"
    }

    "list the provinces in a country" in {
      val countryRepo = new SlickCountryRepository

      val result = Await.result(countryRepo.getProvinces("AR"), duration)

      result.size mustBe 24
    }
  }
  
}