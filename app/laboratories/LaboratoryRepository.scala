package laboratories

import scala.concurrent.Future
import scala.language.postfixOps
import javax.inject.Inject
import javax.inject.Singleton
import models.Tables
import play.api.Application
import slick.driver.PostgresDriver.simple._
import play.api.db.slick.Config.driver.simple.Column
import play.api.db.slick.Config.driver.simple.Compiled
import play.api.db.slick.Config.driver.simple.TableQuery
import play.api.db.slick.Config.driver.simple.booleanColumnExtensionMethods
import play.api.db.slick.Config.driver.simple.booleanColumnType
import play.api.db.slick.Config.driver.simple.columnExtensionMethods
import play.api.db.slick.Config.driver.simple.queryToInsertInvoker
import play.api.db.slick.Config.driver.simple.runnableCompiledToAppliedQueryInvoker
import play.api.db.slick.Config.driver.simple.slickDriver
import play.api.db.slick.Config.driver.simple.stringColumnType
import play.api.db.slick.DB
import types.Email
import util.DefaultDb


abstract class LaboratoryRepository {
  def add(lab: Laboratory): Future[Either[String, String]]
  def getAll(): Future[Seq[Laboratory]]
  def getAllDescriptive(): Future[Seq[Laboratory]]
  def get(id: String): Future[Option[Laboratory]]
  def update(laboratory: Laboratory): Future[Either[String, String]]
}

@Singleton
class SlickLaboratoryRepository @Inject() (implicit app: Application) extends LaboratoryRepository with DefaultDb {

  val laboratories: TableQuery[Tables.Laboratory] = Tables.Laboratory
  val countries: TableQuery[Tables.Country] = Tables.Country
  val provinces: TableQuery[Tables.Province] = Tables.Province

  private def queryDefineGetAllLabsDescriptions = for {
    lab <- laboratories
    cn <- countries
    pr <- provinces
    if (lab.country === cn.code && lab.province === pr.code)
  } yield (lab, cn, pr)

  val queryGetAllLabsDescriptions = Compiled(queryDefineGetAllLabsDescriptions)

  val queryGetAllLabs = Compiled(for (lab <- laboratories) yield lab)

  val queryGetLaboratory = Compiled(queryDefindeGetLaboratory _)

  private def queryDefindeGetLaboratory(id: Column[String]) = for (
    lab <- laboratories if lab.codeName === id ) yield (lab)

  override def getAllDescriptive(): Future[Seq[Laboratory]] = Future {
    DB.withSession { implicit session =>
      queryGetAllLabsDescriptions.list map {
        case (lab, cn, pr) =>
          new Laboratory(lab.name, lab.codeName, cn.name, pr.name, lab.address, lab.telephone, Email(lab.contactEmail), lab.dropIn, lab.dropOut)
      }
    }
  }

  override def add(lab: Laboratory): Future[Either[String, String]] = Future {

    DB.withTransaction { implicit session =>
      try {
        val labRow = Tables.LaboratoryRow(lab.code, lab.name, lab.country, lab.province, lab.address, lab.telephone, lab.contactEmail.text, lab.dropIn, lab.dropOut)
        Tables.Laboratory += labRow
        Right(labRow.codeName)
      } catch {
        case e: Exception => {
          e.printStackTrace()
          Left(e.getMessage())
        }
      }
    }
  }

  override def getAll(): Future[Seq[Laboratory]] = Future {
    DB.withSession { implicit session =>
      queryGetAllLabs.list.map(lab => Laboratory(lab.name, lab.codeName, lab.country, lab.province, lab.address, lab.telephone, Email(lab.contactEmail), lab.dropIn, lab.dropOut))
    }
  }

  override def get(id: String): Future[Option[Laboratory]] = Future {
    DB.withSession { implicit session =>

      queryGetLaboratory(id).firstOption map { lab =>
        Laboratory(lab.name, lab.codeName, lab.country, lab.province, lab.address, lab.telephone, Email(lab.contactEmail), lab.dropIn, lab.dropOut)
      }
    }
  }

  override def update(laboratory : Laboratory): Future[Either[String, String]] = Future {

    DB.withTransaction { implicit session =>
      try {
        val q = for (lab <- laboratories if lab.codeName === laboratory.code)
          yield (lab.codeName, lab.name, lab.country, lab.province, lab.address, lab.telephone, lab.contactEmail, lab.dropIn, lab.dropOut)

        val arg = (laboratory.code, laboratory.name, laboratory.country, laboratory.province, laboratory.address, laboratory.telephone, laboratory.contactEmail.text, laboratory.dropIn, laboratory.dropOut)

        q.update(arg)
        Right(laboratory.code)
      } catch {
        case e: Exception => {
          e.printStackTrace()
          Left(e.getMessage())
        }
      }
    }
  }
}
