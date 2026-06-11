package services

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Json, OWrites}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._

import models.Tables

case class Country(code: String, name: String)
case class Province(code: String, name: String)

object Country:
  implicit val writes: OWrites[Country] = Json.writes[Country]

object Province:
  implicit val writes: OWrites[Province] = Json.writes[Province]

trait CountryService:
  def listCountries: Future[Seq[Country]]
  def listProvinces(country: String): Future[Seq[Province]]

@Singleton
class CountryServiceImpl @Inject() (db: Database)(using ec: ExecutionContext) extends CountryService:

  override def listCountries: Future[Seq[Country]] =
    db.run(Tables.countries.sortBy(_.name).result)
      .map(_.map(r => Country(r.code, r.name)))

  override def listProvinces(country: String): Future[Seq[Province]] =
    db.run(Tables.provinces.filter(_.country === country).sortBy(_.name).result)
      .map(_.map(r => Province(r.code, r.name)))