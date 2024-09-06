package connections

import javax.inject.{Inject, Singleton}
import models.Tables
import play.api.{Application, Logger}
import play.api.db.slick.Config.driver.simple.TableQuery
import util.{DefaultDb, Transaction}
import play.api.i18n.{Messages, MessagesApi}

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
import scala.slick.driver.PostgresDriver.simple._
abstract class InferiorInstanceRepository extends DefaultDb with Transaction {

  def insert(row: InferiorInstance): Future[Either[String, Long]]

  def deleteById(id: Long): Future[Either[String, Unit]]

  def findAll(): Future[Either[String, List[InferiorInstanceFull]]]

  def findAllInstanceStatus(): Future[Either[String, List[InferiorInstanceStatus]]]

  def update(row: InferiorInstanceFull): Future[Either[String, Unit]]

  def countByURL(url: String): Future[Long]

  def findByURL(url:String): Future[Option[InferiorInstance]]

  def findByLabCode(labcode:String): Future[Option[InferiorInstance]]

  def isInferiorInstanceEnabled(url:String): Future[Boolean]

  def updateURL(row: InferiorInstance): Future[Either[String, Unit]]

}

@Singleton
class SlickInferiorInstanceRepository @Inject()(implicit val app: Application,
                                                messagesApi: MessagesApi) extends InferiorInstanceRepository with DefaultDb {
  val inferiorInstanceTable: TableQuery[Tables.InferiorInstance] = Tables.InferiorInstance
  val instanceStatus: TableQuery[Tables.InstanceStatus] = Tables.InstanceStatus
  val logger: Logger = Logger(this.getClass())
  implicit val messages: Messages = messagesApi.preferred(Seq.empty)

  private def queryGetById(id: Column[Long]) = inferiorInstanceTable.filter(_.id === id)
  private def queryGetByURL(url: Column[String]) =  inferiorInstanceTable.filter(_.url === url)
  private def queryGetByLabCode(labCode: Column[String]) =  inferiorInstanceTable.filter(_.laboratory === labCode)

  private def queryGetByURLAndStatus(url: Column[String],status:Column[Long]) =  inferiorInstanceTable.filter(_.url === url).filter(_.status === status)

  val getInferiorInstanceTableById = Compiled(queryGetById _)
  val getInferiorInstanceTableByURL = Compiled(queryGetByURL _)
  val getInferiorInstanceTableByLabCode = Compiled(queryGetByLabCode _)

  val getInferiorInstanceTableByURLAndStatus = Compiled(queryGetByURLAndStatus _)

  val queryGetAll = Compiled(for ((ii) <- inferiorInstanceTable.sortBy(_.status))
                              yield (ii.id,ii.url, ii.status,ii.laboratory))
  val queryGetAllInstanceStatus = Compiled(instanceStatus.map(x => (x.id,x.status)))

  override def insert(row: InferiorInstance): Future[Either[String, Long]] = {

    this.runInTransactionAsync { implicit session => {
      try {
        val id = (inferiorInstanceTable returning inferiorInstanceTable.map(_.id)) += models.Tables.InferiorInstanceRow(id = 0, url = row.url,  1,laboratory = row.laboratory)
        Right(id)
      } catch {
        case e: Exception => {
            logger.error(e.getMessage,e)
            Left(e.getMessage)
          }
        }
      }
    }
  }

  def findByURL(url:String): Future[Option[InferiorInstance]] = {
    this.runInTransactionAsync { implicit session => {
      getInferiorInstanceTableByURL(url).firstOption.map{
        x => {
          InferiorInstance(url=x.url,laboratory = x.laboratory)
        }
      }
      }
    }
  }

  def countByURL(url:String): Future[Long] = {
    this.runInTransactionAsync { implicit session => {
      getInferiorInstanceTableByURL(url).list.length
    }
    }
  }

  override def deleteById(id: Long): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getInferiorInstanceTableById(id).delete
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage,e)
          Left(e.getMessage)
        }
      }

    }
    }
  }

  override def findAll(): Future[Either[String, List[InferiorInstanceFull]]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        val resultList = queryGetAll.list
          .map(x => InferiorInstanceFull(id = x._1, url = x._2, connectivity = "",idStatus = x._3,laboratory = x._4))
        Right(resultList)
      } catch {
        case e: Exception => {
          logger.error(e.getMessage,e)
          Left(e.getMessage)
        }
      }
    }

    }
  }

  override def findAllInstanceStatus(): Future[Either[String, List[InferiorInstanceStatus]]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        Right(queryGetAllInstanceStatus.list.map(x => InferiorInstanceStatus(id = x._1,description = x._2)))
      } catch {
        case e: Exception => {
          logger.error(e.getMessage,e)
          Left(e.getMessage)
        }
      }
    }
    }
  }

  override def updateURL(row: InferiorInstance): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        inferiorInstanceTable.filter(_.laboratory === row.laboratory).map(x => x.url).update(row.url)
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage,e)
          Left(Messages("error.E0701"))
        }
      }

    }
    }
  }
  override def update(row: InferiorInstanceFull): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
          inferiorInstanceTable.filter(_.id === row.id).map(x => x.status).update(row.idStatus)
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage,e)
          Left(Messages("error.E0701"))
        }
      }

    }
    }
  }

  def Duration(i: Int, seconds: Any): _root_.scala.concurrent.duration.Duration = ???

  def isInferiorInstanceEnabled(url:String): Future[Boolean] = {
    this.runInTransactionAsync { implicit session => {
      getInferiorInstanceTableByURLAndStatus((url,2)).list.length == 1
    }
    }
  }

  def findByLabCode(labcode:String): Future[Option[InferiorInstance]] = {
    this.runInTransactionAsync { implicit session => {
      getInferiorInstanceTableByLabCode(labcode).firstOption.map{
        x => {
          InferiorInstance(url=x.url,laboratory = x.laboratory)
        }
      }
    }
    }
  }

}
