package configdata

import models.Tables.geneticists
import types.Geneticist
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}

trait GeneticistRepository {
  def add(gen: Geneticist): Future[Int]
  def getAll(lab: String): Future[Seq[Geneticist]]
  def get(id: Long): Future[Option[Geneticist]]
  def update(gen: Geneticist): Future[Int]
}

@Singleton
class SlickGeneticistRepository @Inject() (
  db: slick.jdbc.JdbcBackend.Database
)(implicit ec: ExecutionContext) extends GeneticistRepository {

  private def toRow(gen: Geneticist): models.Tables.GeneticistRow =
    models.Tables.GeneticistRow(
      gen.id,
      gen.name,
      gen.lastname,
      gen.laboratory,
      gen.email,
      gen.telephone
    )

  private def fromRow(row: models.Tables.GeneticistRow): Geneticist =
    Geneticist(
      row.name,
      row.laboratory,
      row.lastname,
      row.email,
      row.telephone,
      row.id
    )

  override def add(gen: Geneticist): Future[Int] = {
    db.run(geneticists += toRow(gen))
  }

  override def getAll(lab: String): Future[Seq[Geneticist]] = {
    db.run(geneticists.filter(_.laboratory === lab).result).map(_.map(fromRow))
  }

  override def get(id: Long): Future[Option[Geneticist]] = {
    db.run(geneticists.filter(_.id === Option(id)).result.headOption).map(_.map(fromRow))
  }

  override def update(gen: Geneticist): Future[Int] = {
    db.run(geneticists.filter(_.id === gen.id).update(toRow(gen)))
  }
}
