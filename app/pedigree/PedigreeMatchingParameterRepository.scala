package pedigree

import javax.inject.{Inject, Singleton}

import models.Tables
import play.api.Application
import play.api.db.slick.Config.driver.simple._
import util.{DefaultDb, Transaction}

import scala.concurrent.Future

trait PedigreeMatchingParameterRepository {
  def getMaxMendelianExclusions: Future[Int]
  def setMaxMendelianExclusions(value: Int): Future[Int]
}

@Singleton
class SlickPedigreeMatchingParameterRepository @Inject() (implicit app: Application)
  extends PedigreeMatchingParameterRepository with DefaultDb with Transaction {

  // Fila unica de parametros globales de matching de pedigries.
  private val singletonId = 1L

  private val table: TableQuery[Tables.PedigreeMatchingParameter] = Tables.PedigreeMatchingParameter

  def getMaxMendelianExclusions: Future[Int] = {
    this.runInTransactionAsync { implicit session =>
      table.filter(_.id === singletonId).map(_.maxMendelianExclusions).firstOption.getOrElse(0)
    }
  }

  def setMaxMendelianExclusions(value: Int): Future[Int] = {
    this.runInTransactionAsync { implicit session =>
      val updated = table.filter(_.id === singletonId).map(_.maxMendelianExclusions).update(value)
      if (updated == 0) {
        table += Tables.PedigreeMatchingParameterRow(singletonId, value)
      }
      value
    }
  }

}