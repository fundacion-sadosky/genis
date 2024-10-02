package kits

import java.sql.SQLException
import javax.inject.{Inject, Singleton}
import models.Tables
import play.api.Application
import play.api.db.slick._
import util.{DefaultDb, Transaction}
import play.api.i18n.{Messages, MessagesApi}

import scala.concurrent.Future
import slick.driver.PostgresDriver.simple.{Session, _}
import slick.lifted.TableQuery


abstract class LocusRepository extends DefaultDb  with Transaction {
  def add(locus: Locus)(implicit session: Session): Either[String, String]
  def update(locus: Locus)(implicit session: Session): Either[String, Unit]
  def addAlias(id: String, alias: String)(implicit session: Session): Either[String, String]
  def addLink(id: String, link: LocusLink)(implicit session: Session): Either[String, String]
  def listFull(): Future[Seq[FullLocus]]
  def canDeleteLocusByKit(id: String)(implicit session: Session): Boolean
  def canDeleteLocusByLink(id: String)(implicit session: Session): Boolean
  def delete(id: String)(implicit session: Session): Either[String,String]
  def deleteAlias(id: String)(implicit session: Session): Either[String,String]
  def deleteAliasById(id: String)(implicit session: Session): Either[String,String]
  def deleteLinks(id: String)(implicit session: Session): Either[String,String]
  def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[Locus]]
  def getLocusByAnalysisType(analysisType: Int): Future[Seq[Locus]]
}

@Singleton
class SlickLocusRepository @Inject() (implicit app: Application, messagesApi: MessagesApi) extends LocusRepository {
  implicit val messages: Messages = messagesApi.preferred(Seq.empty)

  val loci: TableQuery[Tables.Locus] = Tables.Locus
  val lociAlias: TableQuery[Tables.LocusAlias] = Tables.LocusAlias
  val kitsLociRel: TableQuery[Tables.StrkitLocus] = Tables.StrkitLocus
  val lociLinks: TableQuery[Tables.LocusLink] = Tables.LocusLink
  val analysisTypes: TableQuery[Tables.AnalysisType] = Tables.AnalysisType

  val queryGetLocus = Compiled { id: Column[String] =>
    for{
      l <- loci if l.id === id
    } yield l
  }

  val queryGetAllLocus = Compiled (for (locus <- loci) yield locus)

  val queryGetKitsByLocus = Compiled { id: Column[String] =>
    for{
      kit <- kitsLociRel if kit.locus === id
    } yield kit
  }

  val queryGetLocusLinks = Compiled { id: Column[String] =>
    for{
      l <- lociLinks if l.locus === id || l.link === id
    } yield l
  }

  val queryGetLocusAlias = Compiled { id: Column[String] =>
    for{
      l <- lociAlias if l.marker === id
    } yield l
  }
  val queryGetLocusAliasById = Compiled { id: Column[String] =>
    for{
      l <- lociAlias if l.alias === id
    } yield l
  }
  val queryGetLinkedLocus = Compiled { id: Column[String] =>
    for{
      l <- lociLinks if l.link === id
    } yield l
  }

  val queryGetLocusByAnalysisTypeName = Compiled { analysisType: Column[String] =>
    for (
      (l, at) <- loci innerJoin analysisTypes if l.`type` === at.id && at.name === analysisType
    ) yield l
  }

  val queryGetLocusByAnalysisType = Compiled { analysisType: Column[Int] =>
    for (
      l <- loci if l.`type` === analysisType
    ) yield l
  }

  private def rowToEntity(locus: Tables.LocusRow): Locus = {
    Locus(locus.id, locus.name, locus.chromosome, locus.minimumAllelesQty, locus.maximumAllelesQty, locus.`type`)
  }

  override def add(locus: Locus)(implicit session: Session): Either[String, String] = {
    try {
      val lociRow = Tables.LocusRow(locus.id, locus.name, locus.chromosome, locus.minimumAllelesQty, locus.maximumAllelesQty, locus.analysisType,locus.required,locus.minAlleleValue,locus.maxAlleleValue)
      loci += lociRow
      Right(locus.id)
    } catch {
      case e: SQLException if e.getSQLState.startsWith("23") => Left(Messages("error.E0688",locus.id))
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def addAlias(id: String, alias: String)(implicit session: Session): Either[String, String] = {
    try {
      val aliasRow = Tables.LocusAliasRow(alias, id)
      lociAlias += aliasRow
      Right(id)
    } catch {
      case e: SQLException if e.getSQLState.startsWith("23") => Left(Messages("error.E0640", alias))
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def addLink(id: String, link: LocusLink)(implicit session: Session): Either[String, String] = {
    try {
      val linkRow = Tables.LocusLinkRow(0, id, link.locus, link.factor, link.distance)
      lociLinks += linkRow
      Right(id)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def listFull(): Future[Seq[FullLocus]] = Future {
    DB.withSession { implicit session =>
      queryGetAllLocus.list
        .map(locus =>
          FullLocus(
            Locus(locus.id, locus.name, locus.chromosome, locus.minimumAllelesQty, locus.maximumAllelesQty, locus.`type`,locus.required,locus.minAlleleValue,locus.maxAlleleValue),
            queryGetLocusAlias(locus.id).list.map(a => a.alias),
            queryGetLocusLinks(locus.id).list.map(l => {
              if (locus.id == l.locus) LocusLink(l.link, l.factor, l.distance)
              else LocusLink(l.locus, l.factor, l.distance)
            }
          )
        )
      )
    }
  }

  override def canDeleteLocusByKit(id: String)(implicit session: Session): Boolean = {
    queryGetKitsByLocus(id).list.isEmpty
  }

  override def canDeleteLocusByLink(id: String)(implicit session: Session): Boolean = {
    queryGetLinkedLocus(id).list.isEmpty
  }


  override def deleteLinks(id: String)(implicit session: Session): Either[String,String] = {
    try {
      queryGetLocusLinks(id).delete
      Right(id)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def deleteAlias(id: String)(implicit session: Session): Either[String,String] = {
    try {
      queryGetLocusAlias(id).delete
      Right(id)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }
  override def deleteAliasById(id: String)(implicit session: Session): Either[String,String] = {
    try {
      queryGetLocusAliasById(id).delete
      Right(id)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }
  override def delete(id: String)(implicit session: Session): Either[String,String] = {
      try {
        queryGetLocus(id).delete
        Right(id)
      } catch {
        case e: Exception => {
          e.printStackTrace()
          Left(e.getMessage)
        }
      }
  }

  override def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[Locus]] = Future {
    DB.withSession { implicit session =>
      queryGetLocusByAnalysisTypeName(analysisType).list
        .map(locus =>
          Locus(locus.id, locus.name, locus.chromosome, locus.minimumAllelesQty, locus.maximumAllelesQty, locus.`type`)
          )
    }
  }
  override def update(locus: Locus)(implicit session: Session): Either[String, Unit] = {
    loci.filter(_.id === locus.id).map(x => (x.required,x.minAlleleValue,x.maxAlleleValue)).update((locus.required,locus.minAlleleValue,locus.maxAlleleValue))
    Right(())
  }
  override def getLocusByAnalysisType(analysisType: Int): Future[Seq[Locus]] = Future {
    DB.withSession { implicit session =>
      queryGetLocusByAnalysisType(analysisType).list
        .map(locus =>
          Locus(locus.id, locus.name, locus.chromosome, locus.minimumAllelesQty, locus.maximumAllelesQty, locus.`type`)
        )
    }
  }

}
