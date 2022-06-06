package configdata

import scala.concurrent.Future
import javax.inject.Singleton
import javax.inject.Inject
import services.CacheService
import services.Keys

abstract class CountryService {
  def listCountries(): Future[Seq[Country]]
  def listProvinces(country: String): Future[Seq[Province]]
}

@Singleton
class CountryServiceImpl @Inject() (cache: CacheService, cRepository: CountryRepository) extends CountryService{
  
  override def listCountries(): Future[Seq[Country]] = {
    cache.asyncGetOrElse(Keys.countries)(cRepository.getCountries)
  }

  override def listProvinces(country: String): Future[Seq[Province]] = {
    cache.asyncGetOrElse(Keys.provinces)(cRepository.getProvinces(country))
  }
}