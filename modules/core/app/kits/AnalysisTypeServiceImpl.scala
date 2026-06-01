package kits

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.JdbcBackend.Database
import models._

@Singleton
class AnalysisTypeServiceImpl @Inject()(db: Database)(implicit ec: ExecutionContext) extends AnalysisTypeService:

  private val table = AnalysisTypeTable.query

  private def rowToAt(row: AnalysisTypeRow): AnalysisType =
    AnalysisType(row.id, row.name, row.mitochondrial)

  override def list(): Future[Seq[AnalysisType]] =
    db.run(table.result).map(_.map(rowToAt))

  override def getByName(name: String): Future[Option[AnalysisType]] =
    db.run(table.filter(_.name === name).result.headOption).map(_.map(rowToAt))

  override def getById(id: Int): Future[Option[AnalysisType]] =
    db.run(table.filter(_.id === id).result.headOption).map(_.map(rowToAt))
