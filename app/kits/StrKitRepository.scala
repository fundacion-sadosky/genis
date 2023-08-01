package kits

import java.sql.SQLException
import javax.inject.{Inject, Singleton}

import models.Tables
import play.api.Application
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import util.{DefaultDb, Transaction}

import play.api.i18n.Messages

import scala.concurrent.Future

abstract class StrKitRepository extends DefaultDb with Transaction {
  def get(id: String): Future[Option[StrKit]]
  def getFull(id: String): Future[Option[FullStrKit]]
  def list(): Future[Seq[StrKit]]
  def listFull(): Future[Seq[FullStrKit]]
  def findLociByKit(kitId: String): Future[List[StrKitLocus]]
  def getKitsAlias: Future[Map[String, String]]
  def getLociAlias: Future[Map[String, String]]
  def getAllLoci: Future[Seq[String]]
  def findLociByKits(kitIds: Seq[String]): Future[Map[String, List[StrKitLocus]]]
  def add(kit: StrKit)(implicit session: Session): Either[String, String]
  def addAlias(id: String, alias: String)(implicit session: Session): Either[String, String]
  def addLocus(id: String, locus: NewStrKitLocus)(implicit session: Session): Either[String, String]
  def update(kit: StrKit)(implicit session: Session): Either[String, String]
  def delete(id: String)(implicit session: Session): Either[String,String]
  def deleteAlias(id: String)(implicit session: Session): Either[String,String]
  def deleteLocus(id: String)(implicit session: Session): Either[String,String]
}

@Singleton
class SlickKitDataRepository @Inject() (implicit app: Application) extends StrKitRepository {

  val kits: TableQuery[Tables.Strkit] = Tables.Strkit
  val kitsLociRel: TableQuery[Tables.StrkitLocus] = Tables.StrkitLocus
  val loci: TableQuery[Tables.Locus] = Tables.Locus
  val kitsAlias: TableQuery[Tables.StrkitAlias] = Tables.StrkitAlias
  val lociAlias: TableQuery[Tables.LocusAlias] = Tables.LocusAlias

  val queryGetAllKitsCompiled = Compiled(for (kit <- kits) yield (kit))

  private def queryLociByKit(kitId: Column[String]) = (for (
    (locus, relation) <- loci innerJoin kitsLociRel
      on (_.id === _.locus) if relation.strkit === kitId
  ) yield (
    locus.id, locus.name, locus.chromosome, locus.minimumAllelesQty, locus.maximumAllelesQty, relation.fluorophore, relation.order,locus.required)).sortBy(f => f._7.asc)

  val queryLociByKitCompiled = Compiled(queryLociByKit _)

  val queryGetKitAlias = Compiled(for (ka <- kitsAlias) yield (ka))

  val queryGetLociAlias = Compiled(for (la <- lociAlias) yield (la))

  val queryGetAllLoci = Compiled(for (l <- loci) yield (l))

  val queryGetKitAliasById = Compiled { id: Column[String] =>
    for{
      l <- kitsAlias if l.kit === id
    } yield l
  }

  val queryGetKitLocusById = Compiled { id: Column[String] =>
    for{
      l <- kitsLociRel if l.strkit === id
    } yield l
  }

  val queryGetKit = Compiled { id: Column[String] =>
    for{
      l <- kits if l.id === id
    } yield l
  }

  override def getFull(id: String): Future[Option[FullStrKit]] = Future {
    DB.withSession { implicit session =>
      queryGetKit(id).firstOption map { f =>
        FullStrKit(f.id, f.name, f.`type`, f.lociQty, f.representativeParameter,
          queryGetKitAliasById(id).list.map(a => a.alias),
          queryLociByKitCompiled(id).list.map(l => NewStrKitLocus(l._1, l._6, l._7.getOrElse(Int.MaxValue)))
        )
      }
    }
  }

  override def get(id: String): Future[Option[StrKit]] = Future {
    DB.withSession { implicit session =>
      queryGetKit(id).firstOption map { f =>
        StrKit(f.id, f.name, f.`type`, f.lociQty, f.representativeParameter)
      }
    }
  }

  def list(): Future[List[StrKit]] = Future {
    DB.withSession { implicit session =>
      queryGetAllKitsCompiled.list.map(f => new StrKit(f.id, f.name, f.`type`, f.lociQty, f.representativeParameter))
    }
  }

  def findLociByKit(kitId: String): Future[List[StrKitLocus]] = Future {
    DB.withSession { implicit session =>
      queryLociByKitCompiled(kitId).list.map(f => new StrKitLocus(f._1, f._2, f._3, f._4, f._5, f._6, f._7.getOrElse(Int.MaxValue),f._8))
    }
  }

  override def getKitsAlias: Future[Map[String, String]] = Future {
    DB.withSession { implicit session =>
      queryGetKitAlias.list.map { ka => (ka.alias, ka.kit) }.toMap
    }
  }

  override def getLociAlias: Future[Map[String, String]] = Future {
    DB.withSession { implicit session =>
      queryGetLociAlias.list.map { la => (la.alias, la.marker) }.toMap
    }
  }

  override def getAllLoci: Future[Seq[String]] = Future {
    DB.withSession { implicit session =>
      queryGetAllLoci.list.map { x => x.id }
    }
  }

  private def queryFindLociByKits(kitIds: List[String]) = for (
    (locus, relation) <- loci innerJoin kitsLociRel
      on (_.id === _.locus) if relation.strkit inSetBind kitIds
  ) yield (locus, relation)

  override def findLociByKits(kitIds: Seq[String]): Future[Map[String, List[StrKitLocus]]] = Future {
    def convert2Loci(l: List[(Tables.LocusRow, Tables.StrkitLocusRow)]): List[StrKitLocus] = {
      l.foldLeft(List[StrKitLocus]()) { (p, c) =>
        p :+ StrKitLocus(c._1.id, c._1.name, c._1.chromosome, c._1.minimumAllelesQty, c._1.maximumAllelesQty, c._2.fluorophore, c._2.order.getOrElse(Int.MaxValue))
      }
    }

    DB.withSession { implicit session =>
      val groupedByKit = queryFindLociByKits(kitIds.toList).list.groupBy { _._2.strkit }
      groupedByKit.map { r => r._1 -> convert2Loci(r._2) }
    }
  }

  def listFull(): Future[List[FullStrKit]] = Future {
    DB.withSession { implicit session =>
      queryGetAllKitsCompiled.list.map(kit =>
        FullStrKit(
          kit.id, kit.name, kit.`type`, kit.lociQty, kit.representativeParameter,
          queryGetKitAliasById(kit.id).list.map(a => a.alias),
          queryLociByKitCompiled(kit.id).list.map(l => NewStrKitLocus(l._1, l._6, l._7.getOrElse(Int.MaxValue)))
        )
      )
    }
  }

  override def add(kit: StrKit)(implicit session: Session): Either[String, String] = {
    try {
      val kitRow = Tables.StrkitRow(kit.id, kit.name, kit.`type`, kit.locy_quantity, kit.representative_parameter)
      kits += kitRow
      Right(kit.id)
    } catch {
      case e: SQLException if e.getSQLState.startsWith("23") => Left(Messages("error.E0694", kit.id))
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def addAlias(id: String, alias: String)(implicit session: Session): Either[String, String] = {
    try {
      val aliasRow = Tables.StrkitAliasRow(id, alias)
      kitsAlias += aliasRow
      Right(id)
    } catch {
      case e: SQLException if e.getSQLState.startsWith("23") => Left(Messages("error.E0640", alias))
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def addLocus(id: String, locus: NewStrKitLocus)(implicit session: Session): Either[String, String] = {
    try {
      val locusRow = Tables.StrkitLocusRow(id, locus.locus, locus.fluorophore, Some(locus.order))
      kitsLociRel += locusRow
      Right(id)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def deleteLocus(id: String)(implicit session: Session): Either[String,String] = {
    try {
      queryGetKitLocusById(id).delete
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
      queryGetKitAliasById(id).delete
      Right(id)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def update(kit : StrKit)(implicit session: Session): Either[String, String] = {
      try {
        val q = for (k <- kits if k.id === kit.id) yield (k.representativeParameter)
        val arg = (kit.representative_parameter)
        q.update(arg)
        Right(kit.id)
      } catch {
        case e: Exception => {
          e.printStackTrace()
          Left(e.getMessage())
        }
      }
  }

  override def delete(id: String)(implicit session: Session): Either[String,String] = {
    try {
      queryGetKit(id).delete
      Right(id)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

}
