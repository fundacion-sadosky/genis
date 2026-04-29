package pedigree

import models.Tables
import play.api.libs.json.Json
import slick.jdbc.PostgresProfile.api.*

import scala.concurrent.Future

import scala.concurrent.{ExecutionContext, Future}

case class PedCheck(
  id: Long,
  idPedigree: Long,
  locus: String,
  globalCode: String
)

object PedCheck:
  implicit val format: play.api.libs.json.OFormat[PedCheck] = Json.format[PedCheck]

case class PedCheckResult(
  pedigreeConsistency: List[PedigreeConsistency],
  isConsistent: Boolean,
  consistencyRun: Boolean
)

object PedCheckResult:
  implicit val format: play.api.libs.json.OFormat[PedCheckResult] = Json.format[PedCheckResult]

// ---------------------------------------------------------------------------
// PedCheckRepository
// ---------------------------------------------------------------------------

trait PedCheckRepository:
  def insert(pedChecks: List[PedCheck]): Future[Either[String, Unit]]
  def getPedCheck(idPedigree: Long): Future[List[PedCheck]]
  def cleanConsistency(idPedigree: Long): Future[Either[String, Unit]]

@jakarta.inject.Singleton
class SlickPedCheckRepository @jakarta.inject.Inject() (
  db: slick.jdbc.JdbcBackend.Database
)(implicit ec: ExecutionContext) extends PedCheckRepository:

  private val table = Tables.PedCheck

  override def insert(pedChecks: List[PedCheck]): Future[Either[String, Unit]] =
    val rows = pedChecks.map(pc => Tables.PedCheckRow(0L, pc.idPedigree, pc.locus, pc.globalCode))
    val action = for
      _ <- table.filter(_.idPedigree === pedChecks.headOption.map(_.idPedigree).getOrElse(0L)).delete
      _ <- table ++= rows
    yield ()
    db.run(action.transactionally)
      .map(_ => Right(()))
      .recover { case e => Left(e.getMessage) }

  override def getPedCheck(idPedigree: Long): Future[List[PedCheck]] =
    db.run(table.filter(_.idPedigree === idPedigree).result)
      .map(_.toList.map(r => PedCheck(r.id, r.idPedigree, r.locus, r.globalCode)))

  override def cleanConsistency(idPedigree: Long): Future[Either[String, Unit]] =
    db.run(table.filter(_.idPedigree === idPedigree).delete)
      .map(_ => Right(()))
      .recover { case e => Left(e.getMessage) }

// ---------------------------------------------------------------------------
// PedCheckService
// ---------------------------------------------------------------------------

trait PedCheckService:
  def cleanConsistency(pedigreeId: Long): Future[Either[String, Unit]]
  def getPedCheck(pedigreeId: Long): Future[List[PedCheck]]

@jakarta.inject.Singleton
class PedCheckServiceImpl @jakarta.inject.Inject() (
  pedCheckRepository: PedCheckRepository
)(using ec: scala.concurrent.ExecutionContext) extends PedCheckService:
  override def cleanConsistency(pedigreeId: Long): Future[Either[String, Unit]] =
    pedCheckRepository.cleanConsistency(pedigreeId)
  override def getPedCheck(pedigreeId: Long): Future[List[PedCheck]] =
    pedCheckRepository.getPedCheck(pedigreeId)

@jakarta.inject.Singleton
class PedCheckServiceStub extends PedCheckService:
  override def cleanConsistency(pedigreeId: Long): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def getPedCheck(pedigreeId: Long): Future[List[PedCheck]] = Future.successful(List.empty)
