package pedigree

import javax.inject.{Inject, Singleton}

import models.Tables
import play.api.{Application, Logger}
import play.api.db.slick.Config.driver.simple._
import util.{DefaultDb, Transaction}
import scala.concurrent.Future
import slick.lifted.Compiled

abstract class PedCheckRepository extends DefaultDb with Transaction {
  def insert(pedChecks:List[PedCheck]): Future[Either[String, Unit]]
  def getPedCheck(idPedigree:Long):Future[List[PedCheck]]
  def cleanConsistency(idPedigree:Long): Future[Either[String, Unit]]
}
@Singleton
class SlickPedCheckRepository @Inject() (implicit app: Application) extends PedCheckRepository  {

  val logger: Logger = Logger(this.getClass())

  val pedCheckTable: TableQuery[Tables.PedCheck] = Tables.PedCheck

  private def queryGetPedCheckByPedId(id: Column[Long]) = pedCheckTable.filter(_.idPedigree === id)

  val getPedCheckByPedId = Compiled(queryGetPedCheckByPedId _)

  def getPedCheck(idPedigree:Long):Future[List[PedCheck]] = {
    this.runInTransactionAsync { implicit session =>
      getPedCheckByPedId(idPedigree).list.map(x => PedCheck(x.id, x.idPedigree,x.locus,x.globalCode))
    }
  }
  def cleanConsistency(idPedigree:Long): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getPedCheckByPedId(idPedigree).delete
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Right(())
        }
      }
    }
    }
  }

  def insert(pedChecks:List[PedCheck]): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        if(pedChecks.nonEmpty){
          getPedCheckByPedId(pedChecks.head.idPedigree).delete
        }
        val ids = pedChecks.map(x => {
          val id = (pedCheckTable returning pedCheckTable.map(_.id)) +=
            models.Tables.PedCheckRow(id = 0, x.idPedigree,
              x.locus, x.globalCode)
          id
        })
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Right(())
        }
      }
    }
    }
  }

}
