package stats

import javax.inject.{Inject, Singleton}

import models.Tables
import play.api.db.slick.Config.driver.simple.{TableQuery, columnExtensionMethods, longColumnType, queryToInsertInvoker, stringColumnType, valueToConstColumn}
import play.api.db.slick.DB
import play.api.{Application, Logger}
import probability.ProbabilityModel
import util.DefaultDb

import scala.concurrent.Future
import scala.slick.driver.PostgresDriver.simple._
import scala.util.{Failure, Success, Try}

abstract class PopulationBaseFrequencyRepository extends DefaultDb {
  def add(populationBase: PopulationBaseFrequency): Future[Option[Int]]
  def getByName(name: String): Future[Option[PopulationBaseFrequency]]
  def getAll(): Future[Seq[PopulationSampleFrequency]]
  def getAllNames(): Future[Seq[(String, Double, String, Boolean, Boolean)]]
  def toggleStatePopulationBaseFrequency(name: String): Future[Option[Int]]
  def setAsDefault(name: String): Future[Int]
}

@Singleton
class SlickPopulationBaseFrequencyRepository @Inject() (implicit app: Application) extends PopulationBaseFrequencyRepository {

  val logger: Logger = Logger(this.getClass)

  val populationBaseFreq: TableQuery[Tables.PopulationBaseFrequency] = Tables.PopulationBaseFrequency
  val populationBaseName: TableQuery[Tables.PopulationBaseFrequencyName] = Tables.PopulationBaseFrequencyName

  val queryAll = Compiled(for (pbm <- populationBaseName) yield (pbm))

  private def queryDefineGetStateByName(name: Column[String]) = for { bn <- populationBaseName if bn.name === name } yield bn.active

  val queryGetStateByName = Compiled(queryDefineGetStateByName _)

  private def queryDefineGetOneBase(name: Column[String]) = for (
    (pbm, pbf) <- populationBaseName leftJoin populationBaseFreq on (_.id === _.baseName) if pbm.name === name && pbm.active
  ) yield (pbm.name, pbm.theta, pbm.model, pbf)

  val queryGetOneBase = Compiled(queryDefineGetOneBase _)

  val queryGetAllBase = Compiled(for (
    (pbm, pbf) <- populationBaseName leftJoin populationBaseFreq on (_.id === _.baseName) if pbm.active
  ) yield (pbm.name, pbm.theta, pbm.model, pbf))

  override def setAsDefault(name: String): Future[Int] = Future {
    DB.withTransaction { implicit session =>
      val giveDefault = for { bn <- populationBaseName if bn.default === true } yield bn.default
      val s = giveDefault.update(false)
      val setNewDefault = for { bn <- populationBaseName if bn.name === name && bn.active } yield bn.default
      val result = setNewDefault.update(true)
      if (result == 0) { session.rollback }
      result
    }
  }

  override def getAllNames(): Future[Seq[(String, Double, String, Boolean, Boolean)]] = Future {
    DB.withSession { implicit session =>
      queryAll.run.map(pbmr => (pbmr.name, pbmr.theta, pbmr.model, pbmr.active, pbmr.default))
    }
  }

  override def toggleStatePopulationBaseFrequency(name: String): Future[Option[Int]] = Future {
    DB.withTransaction { implicit session =>
      val s = queryGetStateByName(name).update(!queryGetStateByName(name).first)
      Some(s)
    }
  }

  override def add(populationBase: PopulationBaseFrequency): Future[Option[Int]] = Future {
    DB.withTransaction { implicit session =>

      val popNameRow = new Tables.PopulationBaseFrequencyNameRow(0, populationBase.name, populationBase.theta, populationBase.model.toString, true, false)

      val posibleId = Try((populationBaseName returning populationBaseName.map(_.id)) += popNameRow)

      posibleId match {
        case Success(id) => {
          val seqPBF = populationBase.base.map(r => new Tables.PopulationBaseFrequencyRow(0, id, r.marker, r.allele, r.frequency))
          populationBaseFreq ++= seqPBF
        }
        case Failure(e) => {
          logger.debug(e.getMessage)
          None
        }
      }
    }
  }

  override def getByName(name: String): Future[Option[PopulationBaseFrequency]] = Future {
    DB.withSession { implicit session =>

      val retQry: scala.List[(String, Double, String, Tables.PopulationBaseFrequencyRow)]
      = queryGetOneBase(name).list

      val list = retQry.map({ t =>
        val row = t._4
        PopulationSampleFrequency(row.marker, row.allele, row.frequency)
      }).toList

      if (list.isEmpty) {
        None
      } else {
        Some(PopulationBaseFrequency(name, retQry(0)._2, ProbabilityModel.withName(retQry(0)._3), list))
      }
    }
  }
  override def getAll(): Future[Seq[PopulationSampleFrequency]] = Future {
    DB.withSession { implicit session =>

      val retQry: scala.List[(String, Double, String, Tables.PopulationBaseFrequencyRow)]
      = queryGetAllBase.list

      val list = retQry.map({ t =>
        val row = t._4
        PopulationSampleFrequency(row.marker, row.allele, row.frequency)
      })
      list
    }
  }
}
