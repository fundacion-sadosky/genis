package configdata

import models.Tables.laboratories
import types.Laboratory
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.JdbcBackend.Database
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import org.postgresql.util.PSQLException
import play.api.Logger

trait LaboratoryRepository {
  def add(lab: Laboratory): Future[Either[String, String]]
  def getAll(): Future[Seq[Laboratory]]
  def get(id: String): Future[Option[Laboratory]]
  def update(lab: Laboratory): Future[Either[String, String]]
}

@Singleton
class SlickLaboratoryRepository @Inject() (
  db: Database
)(implicit ec: ExecutionContext) extends LaboratoryRepository {

  private val logger: Logger = Logger(this.getClass)

  // El legacy devolvía Left(e.getMessage), filtrando detalles de la base al cliente. Acá
  // logueamos el detalle server-side y devolvemos un mensaje en español (ver security-checklist).
  private def dbError(e: Throwable): String = {
    logger.error("Error de base de datos en laboratorio", e)
    e match {
      case psql: PSQLException if psql.getSQLState == "23505" => "Código de laboratorio ya utilizado"
      case _                                                  => "Error inesperado en la base de datos"
    }
  }

  private def toRow(lab: Laboratory): models.Tables.LaboratoryRow =
    models.Tables.LaboratoryRow(
      lab.code,
      lab.name,
      lab.country,
      lab.province,
      lab.address,
      lab.telephone,
      lab.contactEmail,
      lab.dropIn,
      lab.dropOut
    )

  private def fromRow(row: models.Tables.LaboratoryRow): Laboratory =
    Laboratory(
      row.name,
      row.codeName,
      row.country,
      row.province,
      row.address,
      row.telephone,
      row.contactEmail,
      row.dropIn,
      row.dropOut
    )

  override def add(lab: Laboratory): Future[Either[String, String]] = {
    val action = (laboratories += toRow(lab)).map(_ => lab.code)
    db.run(action.transactionally)
      .map(code => Right(code))
      .recover { case e => Left(dbError(e)) }
  }

  override def getAll(): Future[Seq[Laboratory]] = {
    db.run(laboratories.result).map(_.map(fromRow))
  }

  override def get(id: String): Future[Option[Laboratory]] = {
    db.run(laboratories.filter(_.codeName === id).result.headOption).map(_.map(fromRow))
  }

  override def update(lab: Laboratory): Future[Either[String, String]] = {
    val action = laboratories.filter(_.codeName === lab.code).update(toRow(lab)).map(_ => lab.code)
    db.run(action.transactionally)
      .map(code => Right(code))
      .recover { case e => Left(dbError(e)) }
  }
}
