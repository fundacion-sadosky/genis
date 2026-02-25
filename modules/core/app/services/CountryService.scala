package services


trait CountryService {
  def listCountries: scala.concurrent.Future[Seq[String]]
  def listProvinces(country: String): scala.concurrent.Future[Seq[String]]
}

import javax.inject.Singleton

@Singleton
class CountryServiceImpl extends CountryService {
  override def listCountries: scala.concurrent.Future[Seq[String]] = scala.concurrent.Future.successful(Seq.empty)
  override def listProvinces(country: String): scala.concurrent.Future[Seq[String]] = scala.concurrent.Future.successful(Seq.empty)
}
