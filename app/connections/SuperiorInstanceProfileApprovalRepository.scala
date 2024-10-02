package connections


import javax.inject.{Inject, Singleton}

import models.Tables
import play.api.{Application, Logger}
import play.api.db.slick.Config.driver.simple.TableQuery
import play.api.libs.json.Json
import profile.Profile
import util.{DefaultDb, Transaction}
import scala.concurrent.Future
import slick.driver.PostgresDriver.simple._

abstract class SuperiorInstanceProfileApprovalRepository extends DefaultDb with Transaction {

  def upsert(row: SuperiorInstanceProfileApproval): Future[Either[String, Long]]

  def findByGlobalCode(globalCode: String): Future[Either[String, SuperiorInstanceProfileApproval]]

  def findAll(profileApprovalSearch:ProfileApprovalSearch): Future[List[PendingProfileApproval]]
  def getTotal(): Future[Long]

  def delete(globalCode: String): Future[Either[String, Unit]]

  def deleteLogical(globalCode: String,
                    rejectionUser: Option[String] = None,
                    rejectionDate: Option[java.sql.Timestamp] = None,
                    idRejectMotive: Option[Long] = None,
                    rejectMotive: Option[String] = None): Future[Either[String, Unit]]
}

@Singleton
class SlickSuperiorInstanceProfileApprovalRepository @Inject()(implicit val app: Application) extends SuperiorInstanceProfileApprovalRepository with DefaultDb {
  val logger: Logger = Logger(this.getClass())
  val superiorInstanceProfileApprovalTable: TableQuery[Tables.SuperiorInstanceProfileApproval] = Tables.SuperiorInstanceProfileApproval

  private def queryGetAll() = superiorInstanceProfileApprovalTable.filter(_.deleted === false).sortBy(_.id.desc).map(x => (x.globalCode, x.profile,x.laboratoryImmediateInstance,x.receptionDate,x.errors,x.profileAssociated))

  private def queryGetByGlobalCode(globalCode: Column[String]) = superiorInstanceProfileApprovalTable.filter(_.globalCode === globalCode)

  val getByGlobalCode = Compiled(queryGetByGlobalCode _)
  val getAll = Compiled(queryGetAll)

  override def upsert(row: SuperiorInstanceProfileApproval): Future[Either[String, Long]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        val existingRow = getByGlobalCode(row.globalCode).firstOption
        if(existingRow.isDefined){
          getByGlobalCode(row.globalCode).delete
        }
        val id = (superiorInstanceProfileApprovalTable returning
          superiorInstanceProfileApprovalTable.map(_.id)) +=
          models.Tables.SuperiorInstanceProfileApprovalRow(
            id = row.id,
            globalCode = row.globalCode,
            profile = row.profile,
            laboratory = row.laboratory,
            laboratoryInstanceOrigin = row.laboratoryInstanceOrigin,
            laboratoryImmediateInstance = row.laboratoryImmediateInstance,
            sampleEntryDate = row.sampleEntryDate,
            receptionDate = Some(row.receptionDate.getOrElse(new java.sql.Timestamp(System.currentTimeMillis()))),
            errors = row.errors,
            profileAssociated = row.profileAssociated
          )
        Right(id)
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Left(e.getMessage)
        }
      }
    }
    }
  }

  def findByGlobalCode(globalCode: String): Future[Either[String, SuperiorInstanceProfileApproval]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getByGlobalCode(globalCode).firstOption match {
          case None => Left("no existe el perfil")
          case Some(row) =>{
            Right(SuperiorInstanceProfileApproval
            ( id = 0L,
              globalCode = row.globalCode,
              profile = row.profile,
              laboratory = row.laboratory,
              laboratoryInstanceOrigin = row.laboratoryInstanceOrigin,
              laboratoryImmediateInstance = row.laboratoryImmediateInstance,
              sampleEntryDate = row.sampleEntryDate,
              profileAssociated = row.profileAssociated))
          }
        }
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Left(e.getMessage)
        }
      }
    }
    }
  }

  def findAll(profileApprovalSearch:ProfileApprovalSearch): Future[List[PendingProfileApproval]] = {
    this.runInTransactionAsync { implicit session => {
      try {

        queryGetAll().drop((profileApprovalSearch.page-1) * profileApprovalSearch.pageSize).take(profileApprovalSearch.pageSize).list.map(x => {

          val profile = Json.fromJson[Profile](Json.parse(x._2)).get

          PendingProfileApproval(globalCode = x._1,laboratory = x._3, category = profile.categoryId.text, profile.analyses,receptionDate = x._4.get.toString,errors = x._5,profile.genotypification,x._6.isDefined,true,profile.assignee)

        })
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Nil
        }
      }
    }
    }
  }

  def getTotal(): Future[Long]= {
    this.runInTransactionAsync { implicit session => {
      try {
        getAll.list.size
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          0L
        }
      }
    }
    }
  }

  def delete(globalCode: String): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        queryGetByGlobalCode(globalCode).delete
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Left(e.getMessage)
        }
      }
    }
    }
  }
  override def deleteLogical(globalCode: String,
                    rejectionUser: Option[String] = None,
                    rejectionDate: Option[java.sql.Timestamp] = None,
                    idRejectMotive: Option[Long] = None,
                    rejectMotive: Option[String] = None): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        queryGetByGlobalCode(globalCode).map(x =>
          (x.deleted,x.rejectionUser,x.rejectMotive,x.rejectionDate,x.idRejectMotive))
          .update((true,rejectionUser,rejectMotive,rejectionDate,idRejectMotive))
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Left(e.getMessage)
        }
      }
    }
    }
  }

}
