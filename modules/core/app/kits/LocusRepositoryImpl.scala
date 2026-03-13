package kits

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.JdbcBackend.Database
import slick.dbio.{DBIO, DBIOAction, NoStream, Effect}
import models._
import java.sql.SQLException

@Singleton
class LocusRepositoryImpl @Inject()(implicit ec: ExecutionContext) extends LocusRepository:

  private val db = Database.forConfig("slick.dbs.default.db")

  private val loci = LocusTable.query
  private val lociAlias = LocusAliasTable.query
  private val lociLinks = LocusLinkTable.query
  private val analysisTypes = AnalysisTypeTable.query

  private def rowToLocus(row: LocusRow): Locus =
    Locus(row.id, row.name, row.chromosome, row.minimumAllelesQty, row.maximumAllelesQty,
      row.`type`, row.required, row.minAlleleValue, row.maxAlleleValue)

  private type AnyDBIO[T] = DBIOAction[T, NoStream, Effect.All]

  private def succeed[T](value: T): AnyDBIO[T] =
    DBIO.successful(value).asInstanceOf[AnyDBIO[T]]

  // --- Private DBIO actions ---

  private def insertLocus(locus: Locus): AnyDBIO[Either[String, String]] =
    val row = LocusRow(locus.id, locus.name, locus.chromosome, locus.minimumAllelesQty,
      locus.maximumAllelesQty, locus.analysisType, locus.required, locus.minAlleleValue, locus.maxAlleleValue)
    (loci += row).map(_ => Right(locus.id): Either[String, String]).asTry.map {
      case scala.util.Success(r) => r
      case scala.util.Failure(e: SQLException) if e.getSQLState.startsWith("23") =>
        Left(s"El marcador ${locus.id} ya existe")
      case scala.util.Failure(e) => Left(e.getMessage)
    }

  private def insertAlias(locusId: String, alias: String): AnyDBIO[Either[String, String]] =
    (lociAlias += LocusAliasRow(alias, locusId)).map(_ => Right(locusId): Either[String, String]).asTry.map {
      case scala.util.Success(r) => r
      case scala.util.Failure(e: SQLException) if e.getSQLState.startsWith("23") =>
        Left(s"El alias $alias ya existe")
      case scala.util.Failure(e) => Left(e.getMessage)
    }

  private def insertLink(locusId: String, link: LocusLink): AnyDBIO[Either[String, String]] =
    val row = LocusLinkRow(0, locusId, link.locus, link.factor, link.distance)
    (lociLinks += row).map(_ => Right(locusId): Either[String, String]).asTry.map {
      case scala.util.Success(r) => r
      case scala.util.Failure(e) => Left(e.getMessage)
    }

  private def insertAliases(locusId: String, aliases: List[String]): AnyDBIO[Either[String, String]] =
    aliases.foldLeft(succeed(Right(locusId): Either[String, String])) { (prevAction, alias) =>
      prevAction.flatMap {
        case Left(err) => succeed(Left(err): Either[String, String])
        case Right(_) => insertAlias(locusId, alias)
      }
    }

  private def insertLinks(locusId: String, links: List[LocusLink]): AnyDBIO[Either[String, String]] =
    links.foldLeft(succeed(Right(locusId): Either[String, String])) { (prevAction, link) =>
      prevAction.flatMap {
        case Left(err) => succeed(Left(err): Either[String, String])
        case Right(_) => insertLink(locusId, link)
      }
    }

  private def removeAliases(locusId: String): DBIO[Int] =
    lociAlias.filter(_.marker === locusId).delete

  private def removeAliasById(aliasId: String): DBIO[Int] =
    lociAlias.filter(_.alias === aliasId).delete

  private def removeLinks(locusId: String): DBIO[Int] =
    lociLinks.filter(l => l.locus === locusId || l.link === locusId).delete

  private def removeLocus(locusId: String): DBIO[Int] =
    loci.filter(_.id === locusId).delete

  private def updateLocusFields(locus: Locus): DBIO[Int] =
    loci.filter(_.id === locus.id)
      .map(x => (x.required, x.minAlleleValue, x.maxAlleleValue))
      .update((locus.required, locus.minAlleleValue, locus.maxAlleleValue))

  // --- Public Future methods ---

  override def add(fullLocus: FullLocus): Future[Either[String, String]] =
    val action = for
      addResult <- insertLocus(fullLocus.locus)
      aliasResult <- addResult match
        case Left(err) => succeed(Left(err): Either[String, String])
        case Right(_) => insertAliases(fullLocus.locus.id, fullLocus.alias)
      linkResult <- aliasResult match
        case Left(err) => succeed(Left(err): Either[String, String])
        case Right(_) => insertLinks(fullLocus.locus.id, fullLocus.links)
    yield linkResult
    db.run(action.transactionally)

  override def update(fullLocus: FullLocus): Future[Either[String, Unit]] =
    val action = for
      // Get current aliases for this locus
      currentAliases <- lociAlias.filter(_.marker === fullLocus.locus.id).map(_.alias).result
      // Update locus fields
      _ <- updateLocusFields(fullLocus.locus)
      // Determine aliases to add/remove
      aliasesToAdd = fullLocus.alias.filter(a => !currentAliases.contains(a))
      aliasesToDelete = currentAliases.filter(a => !fullLocus.alias.contains(a))
      // Add new aliases
      addResult <- insertAliases(fullLocus.locus.id, aliasesToAdd)
      // Delete removed aliases
      deleteResult <- addResult match
        case Left(err) => succeed(Left(err): Either[String, String])
        case Right(_) =>
          aliasesToDelete.foldLeft(succeed(Right(fullLocus.locus.id): Either[String, String])) { (prevAction, alias) =>
            prevAction.flatMap {
              case Left(err) => succeed(Left(err): Either[String, String])
              case Right(_) => removeAliasById(alias).map(_ => Right(fullLocus.locus.id): Either[String, String]).asInstanceOf[AnyDBIO[Either[String, String]]]
            }
          }
    yield deleteResult.map(_ => ())
    db.run(action.transactionally)

  override def delete(id: String): Future[Either[String, String]] =
    val action = for
      _ <- removeAliases(id)
      _ <- removeLinks(id)
      _ <- removeLocus(id)
    yield Right(id): Either[String, String]
    db.run(action.transactionally).recover { case e: Exception => Left(e.getMessage) }

  override def listFull(): Future[Seq[FullLocus]] =
    val action = for
      allLoci <- loci.result
      fullLoci <- DBIO.sequence(allLoci.map { locusRow =>
        for
          aliases <- lociAlias.filter(_.marker === locusRow.id).map(_.alias).result
          links <- lociLinks.filter(l => l.locus === locusRow.id || l.link === locusRow.id).result
        yield FullLocus(
          rowToLocus(locusRow),
          aliases.toList,
          links.map { l =>
            if locusRow.id == l.locus then LocusLink(l.link, l.factor, l.distance)
            else LocusLink(l.locus, l.factor, l.distance)
          }.toList
        )
      })
    yield fullLoci
    db.run(action)

  override def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[Locus]] =
    val query = for
      (l, at) <- loci join analysisTypes on (_.`type` === _.id)
      if at.name === analysisType
    yield l
    db.run(query.result).map(_.map(rowToLocus))

  override def getLocusByAnalysisType(analysisType: Int): Future[Seq[Locus]] =
    db.run(loci.filter(_.`type` === analysisType).result).map(_.map(rowToLocus))
