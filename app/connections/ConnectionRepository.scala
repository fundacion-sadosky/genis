package connections

import javax.inject.{Inject, Singleton}
import models.Tables
import models.Tables.ConnectionRow
import models.Tables.MatchSendStatusRow
import models.Tables.MatchUpdateSendStatusRow
import models.Tables.FileSentRow
import play.api.Application
import play.api.db.slick.Config.driver.simple.{Column, Compiled, TableQuery, columnExtensionMethods, longColumnType, runnableCompiledToAppliedQueryInvoker, slickDriver, stringColumnType}
import util.{DefaultDb, Transaction}
import play.api.i18n.{Messages, MessagesApi}

import scala.concurrent.{Await, Future}
import scala.slick.driver.PostgresDriver.simple._

abstract class ConnectionRepository extends DefaultDb with Transaction {
  // Metodos Genericos
  def insert(row: ConnectionRow): Future[Either[String, Long]]

  def insertOrUpdate(row: ConnectionRow): Future[Either[String, Option[Long]]]

  def getById(id: Long): Future[Option[ConnectionRow]]

  def deleteById(id: Long): Future[Either[String, Long]]

  // Metodos Especificos
  def getByName(name: String): Future[Option[ConnectionRow]]

  def getConnections(): Future[Either[String, Connection]]

  def updateConnections(connection: Connection): Future[Either[String, Connection]]

  def getSupInstanceUrl(): Future[Option[String]]
  def updateMatchSendStatus(id: String, targetLab:Option[String],status:Option[Long], message:Option[String] = None): Future[Either[String,Unit]]
  def updateMatchSendStatusHitOrDiscard(id: String, targetLab:Option[String],statusHitDiscard:Option[Long] = None,message:Option[String] = None): Future[Either[String,Unit]]
  def getMatchSendStatusById(id:String, targetLab:Option[String] = None):Future[Option[Long]]
  def insertMatchUpdateSendStatus(id: String, targetLab:Option[String],statusHitDiscard:Option[Long] = None,message:Option[String] = None): Future[Either[String,Unit]]
  def getMatchUpdateSendStatusById(id:String, targetLab:Option[String] = None):Future[Option[Long]]
  def getFailedMatchSent(labCode:String):Future[Seq[MatchSendStatusRow]]
  def getFailedMatchUpdateSent(labCode:String):Future[Seq[MatchUpdateSendStatusRow]]
  def updateFileSent(id: String, targetLab:Option[String],status:Option[Long],fileType:String): Future[Either[String,Unit]]
  def getFailedFileSent(labCode:String):Future[Seq[FileSentRow]]
}

@Singleton
class SlickConnectionRepository @Inject()(implicit val app: Application, messagesApi: MessagesApi) extends ConnectionRepository with DefaultDb {
  implicit val messages: Messages = messagesApi.preferred(Seq.empty)

  val matchSendStatus: TableQuery[Tables.MatchSendStatus] = Tables.MatchSendStatus
  val matchUpdateSendStatus: TableQuery[Tables.MatchUpdateSendStatus] = Tables.MatchUpdateSendStatus
  val fileSent: TableQuery[Tables.FileSent] = Tables.FileSent

  val connectionTable: TableQuery[Tables.Connection] = Tables.Connection
  val instanciaSuperior = "INSTANCIA_SUPERIOR"
  val pkiName = "PKI"

  private def queryConnectionURLByName(name: Column[String]) = connectionTable.filter(_.name === name).filter(_.deleted === false).map(_.url)

  private def queryConnectionByName(name: Column[String]) = connectionTable.filter(_.name === name).filter(_.deleted === false)

  private def queryGetById(id: Column[Long]) = connectionTable.filter(_.id === id).filter(_.deleted === false)

  val getConectionURLByName = Compiled(queryConnectionURLByName _)
  val getConnectionByName = Compiled(queryConnectionByName _)
  val getConnectionById = Compiled(queryGetById _)

  val getMatchSendStatusByIdCompiled = Compiled(queryMatchSendStatusById _)
  val getMatchUpdateSendStatusByIdCompiled = Compiled(queryMatchUpdateSendStatusById _)

  private def queryFailedMatchSent(targetLab:Column[String]) = matchSendStatus.filter(_.status === 7L).filter(_.targetLab === targetLab).sortBy(_.date.desc)
  private def queryFailedMatchUpdateSent(targetLab:Column[String]) = matchUpdateSendStatus.filter(_.status inSet List(11L,12L,13L)).filter(_.targetLab === targetLab).sortBy(_.date.desc)

  val getqueryFailedMatchSent = Compiled(queryFailedMatchSent _)
  val getqueryFailedMatchUpdateSent = Compiled(queryFailedMatchUpdateSent _)


  private def queryMatchSendStatusById(id: Column[String],targetLab:Column[String]) = matchSendStatus.filter(_.id === id).filter(_.targetLab === targetLab)
  private def queryMatchUpdateSendStatusById(id: Column[String],targetLab:Column[String]) = matchUpdateSendStatus.filter(_.id === id).filter(_.targetLab === targetLab)

  override def getConnections(): Future[Either[String, Connection]] = {

    this.runInTransactionAsync { implicit session => {
      var urlSuperiorInstance: String = ""
      var urlPki: String = ""
      try {
        urlSuperiorInstance = getConectionURLByName(instanciaSuperior).first
      } catch {
        case e: Exception => {
        }
      }
      try {
        urlPki = getConectionURLByName(pkiName).first
      } catch {
        case e: Exception => {
        }
      }
      Right(Connection(urlSuperiorInstance, urlPki))
    }
    }
  }

  override def insert(row: ConnectionRow): Future[Either[String, Long]] = {

    this.runInTransactionAsync { implicit session => {
      try {
        val id = (connectionTable returning connectionTable.map(_.id)) += row
        Right(id)
      } catch {
        case e: Exception => {
          Left(e.getMessage)
        }
      }

    }
    }

  }

  override def deleteById(id: Long): Future[Either[String, Long]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getConnectionById(id).delete
        Right(id)
      } catch {
        case e: Exception => {
          Left(e.getMessage)
        }
      }
    }
    }
  }

  override def insertOrUpdate(row: ConnectionRow): Future[Either[String, Option[Long]]] = {

    this.runInTransactionAsync { implicit session => {
      insertOrUpdateSimple(row)
    }
    }

  }

  private def insertOrUpdateSimple(row: ConnectionRow)(implicit session:Session): Either[String, Option[Long]] = {

      try {
        val id = (connectionTable returning connectionTable.map(_.id)) insertOrUpdate row
        Right(id)
      } catch {
        case e: Exception => {
          Left(e.getMessage)
        }
      }

  }

  override def getById(id: Long): Future[Option[ConnectionRow]] = {

    this.runInTransactionAsync { implicit session => {
      try {
        getConnectionById(id).firstOption
      } catch {
        case e: Exception => {
          None
        }
      }

    }
    }

  }

  override def getByName(name: String): Future[Option[ConnectionRow]] = {

    this.runInTransactionAsync { implicit session => {
      getSimpleByName(name);
    }
    }

  }

  private def getSimpleByName(name: String)(implicit session:Session): Option[ConnectionRow] = {

      try {
        getConnectionByName(name).firstOption
      } catch {
        case e: Exception => {
          None
        }
      }

  }

  override def updateConnections(connection: Connection): Future[Either[String, Connection]] = {
    this.runInTransactionAsync { implicit session => {
      val errorMsg = Messages("error.E0708")

      try {
        val insertPki = this.getSimpleByName(pkiName)
        insertPki match {
          case None => this.insertOrUpdateSimple(ConnectionRow(0L, pkiName, connection.pki, false))
          case Some(pkiRow) => this.insertOrUpdateSimple(pkiRow.copy(url = connection.pki))
        }

        val insertSupRow = this.getSimpleByName(instanciaSuperior)
        insertSupRow match {
          case None => this.insertOrUpdateSimple(ConnectionRow(0L, instanciaSuperior, connection.superiorInstance, false))
          case Some(superiorInstanceRow) => this.insertOrUpdateSimple(superiorInstanceRow.copy(url = connection.superiorInstance))
        }

      }catch {
        case e: Exception => {
          Left(errorMsg)
        }
      }
      Right(connection)
    }
    }
  }

  override def getSupInstanceUrl(): Future[Option[String]] = {
    this.runInTransactionAsync { implicit session => {
        getConectionURLByName(instanciaSuperior).firstOption match {
          case Some("") => None
          case Some(url) => Some(url)
          case None => None
        }
    }
    }
  }

  override def updateMatchSendStatus(id: String, targetLab:Option[String],status:Option[Long],message:Option[String] = None): Future[Either[String,Unit]]= {
    this.runInTransactionAsync { implicit session => {
      try {
        matchSendStatus insertOrUpdate models.Tables.MatchSendStatusRow(id,targetLab.getOrElse("SUPERIOR"),status,message,Some(new java.sql.Timestamp(System.currentTimeMillis())))
        Right(())
      } catch {
        case e: Exception => {
          Left(e.getMessage)
        }
      }
    }
    }
  }
  override def updateFileSent(id: String, targetLab:Option[String],status:Option[Long],fileType:String): Future[Either[String,Unit]]= {
    this.runInTransactionAsync { implicit session => {
      try {
        fileSent insertOrUpdate models.Tables.FileSentRow(id,targetLab.getOrElse("SUPERIOR"),status,Some(new java.sql.Timestamp(System.currentTimeMillis())),fileType)
        Right(())
      } catch {
        case e: Exception => {
          Left(e.getMessage)
        }
      }
    }
    }
  }
  override def insertMatchUpdateSendStatus(id: String, targetLab:Option[String],statusHitDiscard:Option[Long] = None,message:Option[String] = None): Future[Either[String,Unit]]= {
    this.runInTransactionAsync { implicit session => {
      try {
        matchUpdateSendStatus insertOrUpdate models.Tables.MatchUpdateSendStatusRow(id,targetLab.getOrElse("SUPERIOR"),statusHitDiscard,message,Some(new java.sql.Timestamp(System.currentTimeMillis())))
        Right(())
      } catch {
        case e: Exception => {
          Left(e.getMessage)
        }
      }
    }
    }
  }
  override def updateMatchSendStatusHitOrDiscard(id: String, targetLab:Option[String],statusHitDiscard:Option[Long] = None,message:Option[String] = None): Future[Either[String,Unit]]= {
    this.runInTransactionAsync { implicit session => {
      try {
        matchUpdateSendStatus.filter(_.id === id)
          .filter(_.targetLab === targetLab.getOrElse("SUPERIOR"))
          .map(x => (x.status,x.message,x.date))
          .update(statusHitDiscard,message,Some(new java.sql.Timestamp(System.currentTimeMillis())))
        Right(())
      } catch {
        case e: Exception => {
          Left(e.getMessage)
        }
      }
    }
    }
  }

  override def getMatchSendStatusById(id:String, targetLab:Option[String] = None):Future[Option[Long]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getMatchSendStatusByIdCompiled(id,targetLab.getOrElse("SUPERIOR")).firstOption.flatMap(_.status)
      } catch {
        case e: Exception => {
          None
        }
      }
    }
    }
  }

  override def getMatchUpdateSendStatusById(id:String, targetLab:Option[String] = None):Future[Option[Long]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getMatchUpdateSendStatusByIdCompiled(id,targetLab.getOrElse("SUPERIOR")).firstOption.flatMap(_.status)
      } catch {
        case e: Exception => {
          None
        }
      }
    }
    }
  }
  override def getFailedMatchSent(labCode:String):Future[Seq[MatchSendStatusRow]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getqueryFailedMatchSent(labCode).list.toSeq
      } catch {
        case e: Exception => {
          Nil
        }
      }
    }
    }
  }
  override def getFailedMatchUpdateSent(labCode:String):Future[Seq[MatchUpdateSendStatusRow]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getqueryFailedMatchUpdateSent(labCode).list.toSeq
      } catch {
        case e: Exception => {
          Nil
        }
      }
    }
    }
  }
  override def getFailedFileSent(labCode:String):Future[Seq[FileSentRow]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        fileSent.filter(_.status === 15L).filter(_.targetLab === labCode).list.toSeq
      } catch {
        case e: Exception => {
          Nil
        }
      }
    }
    }
  }
}
