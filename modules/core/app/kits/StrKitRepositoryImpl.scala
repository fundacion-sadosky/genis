package kits

import java.sql.SQLException
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.JdbcBackend.Database
import models._

@Singleton
class StrKitRepositoryImpl @Inject()(db: Database)(implicit ec: ExecutionContext) extends StrKitRepository:

  private val kits = StrKitTable.query
  private val kitsAlias = StrKitAliasTable.query
  private val kitsLociRel = StrKitLocusTable.query
  private val loci = LocusTable.query
  private val lociAlias = LocusAliasTable.query

  private def toStrKit(row: StrKitRow): StrKit =
    StrKit(row.id, row.name, row.`type`, row.lociQty, row.representativeParameter)

  private def rowsToStrKitLocus(locRow: LocusRow, relRow: StrKitLocusRow): StrKitLocus =
    StrKitLocus(locRow.id, locRow.name, locRow.chromosome, locRow.minimumAllelesQty,
      locRow.maximumAllelesQty, relRow.fluorophore, relRow.order.getOrElse(Int.MaxValue), locRow.required)

  private def lociForKit(kitId: String) =
    (loci join kitsLociRel on (_.id === _.locus))
      .filter { case (_, kl) => kl.strkit === kitId }
      .sortBy { case (_, kl) => kl.order.asc.nullsLast }
      .result
      .map(_.map { case (l, kl) => rowsToStrKitLocus(l, kl) }.toList)

  private def newLociForKit(kitId: String) =
    kitsLociRel
      .filter(_.strkit === kitId)
      .sortBy(_.order.asc.nullsLast)
      .result
      .map(_.map(kl => NewStrKitLocus(kl.locus, kl.fluorophore, kl.order.getOrElse(Int.MaxValue))))

  override def get(id: String): Future[Option[StrKit]] =
    db.run(kits.filter(_.id === id).result.headOption).map(_.map(toStrKit))

  override def getFull(id: String): Future[Option[FullStrKit]] =
    val action = for
      kitOpt <- kits.filter(_.id === id).result.headOption
      result <- kitOpt match
        case None => DBIO.successful(None)
        case Some(row) =>
          for
            aliases  <- kitsAlias.filter(_.kit === id).map(_.alias).result
            kitLoci  <- newLociForKit(id)
          yield Some(FullStrKit(row.id, row.name, row.`type`, row.lociQty, row.representativeParameter,
            aliases, kitLoci))
    yield result
    db.run(action)

  override def list(): Future[Seq[StrKit]] =
    db.run(kits.result).map(_.map(toStrKit))

  override def listFull(): Future[Seq[FullStrKit]] =
    val action = for
      allKits <- kits.result
      fullKits <- DBIO.sequence(allKits.map { row =>
        for
          aliases <- kitsAlias.filter(_.kit === row.id).map(_.alias).result
          kitLoci <- newLociForKit(row.id)
        yield FullStrKit(row.id, row.name, row.`type`, row.lociQty, row.representativeParameter,
          aliases, kitLoci)
      })
    yield fullKits
    db.run(action)

  override def findLociByKit(kitId: String): Future[List[StrKitLocus]] =
    db.run(lociForKit(kitId))

  override def getKitsAlias: Future[Map[String, String]] =
    db.run(kitsAlias.result).map(_.map(r => r.alias -> r.kit).toMap)

  override def getLociAlias: Future[Map[String, String]] =
    db.run(lociAlias.result).map(_.map(r => r.alias -> r.marker).toMap)

  override def getAllLoci: Future[Seq[String]] =
    db.run(loci.map(_.id).result)

  override def findLociByKits(kitIds: Seq[String]): Future[Map[String, List[StrKitLocus]]] =
    db.run(
      (loci join kitsLociRel on (_.id === _.locus))
        .filter { case (_, kl) => kl.strkit.inSet(kitIds) }
        .result
        .map(_.groupBy(_._2.strkit).map { case (kitId, rows) =>
          kitId -> rows.map { case (l, kl) => rowsToStrKitLocus(l, kl) }.toList
        })
    )

  override def add(kit: StrKit): Future[Either[String, String]] =
    val row = StrKitRow(kit.id, kit.name, kit.`type`, kit.locy_quantity, kit.representative_parameter)
    db.run(kits += row)
      .map(_ => Right(kit.id))
      .recover {
        case e: SQLException if e.getSQLState.startsWith("23") =>
          Left(s"El kit ${kit.id} ya existe")
        case e => Left(e.getMessage)
      }

  override def addAlias(id: String, alias: String): Future[Either[String, String]] =
    val row = StrKitAliasRow(id, alias)
    db.run(kitsAlias += row)
      .map(_ => Right(id))
      .recover {
        case e: SQLException if e.getSQLState.startsWith("23") =>
          Left(s"El alias $alias ya existe")
        case e => Left(e.getMessage)
      }

  override def addLocus(id: String, locus: NewStrKitLocus): Future[Either[String, String]] =
    val row = StrKitLocusRow(id, locus.locus, locus.fluorophore, Some(locus.order))
    db.run(kitsLociRel += row)
      .map(_ => Right(id))
      .recover { case e => Left(e.getMessage) }

  override def update(kit: StrKit): Future[Either[String, String]] =
    db.run(
      kits.filter(_.id === kit.id)
        .map(_.representativeParameter)
        .update(kit.representative_parameter)
    ).map(_ => Right(kit.id))
      .recover { case e => Left(e.getMessage) }

  override def delete(id: String): Future[Either[String, String]] =
    db.run(kits.filter(_.id === id).delete)
      .map(_ => Right(id))
      .recover { case e => Left(e.getMessage) }

  override def deleteAlias(id: String): Future[Either[String, String]] =
    db.run(kitsAlias.filter(_.kit === id).delete)
      .map(_ => Right(id))
      .recover { case e => Left(e.getMessage) }

  override def deleteLocus(id: String): Future[Either[String, String]] =
    db.run(kitsLociRel.filter(_.strkit === id).delete)
      .map(_ => Right(id))
      .recover { case e => Left(e.getMessage) }
