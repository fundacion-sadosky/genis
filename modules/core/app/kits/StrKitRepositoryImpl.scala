package kits


import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.JdbcBackend.Database
import models._

@Singleton
class StrKitRepositoryImpl @Inject()(implicit ec: ExecutionContext) extends StrKitRepository {
  // Inicializa la base de datos usando la configuración "strkits-db" en application.conf
  private val db = Database.forConfig("slick.dbs.default.db")

  private def toStrKit(row: StrKitRow): StrKit =
    StrKit(
      id = row.id,
      name = row.name,
      `type` = row.`type`,
      locy_quantity = row.lociQty,
      representative_parameter = row.representativeParameter
    )

  def get(id: String): Future[Option[StrKit]] = {
    db.run(StrKitTable.query.filter(_.id === id).result.headOption).map(_.map(toStrKit))
  }

  def list(): Future[Seq[StrKit]] = {
    db.run(StrKitTable.query.result).map(_.map(toStrKit))
  }

  // TODO: Implement getFull, listFull, and the rest using similar patterns
  def getFull(id: String): Future[Option[FullStrKit]] = Future.successful(None)
  def listFull(): Future[Seq[FullStrKit]] = Future.successful(Seq.empty)
  def findLociByKit(kitId: String): Future[List[StrKitLocus]] = Future.successful(List.empty)
  def getKitsAlias: Future[Map[String, String]] = Future.successful(Map.empty)
  def getLociAlias: Future[Map[String, String]] = Future.successful(Map.empty)
  def getAllLoci: Future[Seq[String]] = Future.successful(Seq.empty)
  def findLociByKits(kitIds: Seq[String]): Future[Map[String, List[StrKitLocus]]] = Future.successful(Map.empty)
  def add(kit: StrKit): Future[Either[String, String]] = Future.successful(Left("Not implemented"))
  def addAlias(id: String, alias: String): Future[Either[String, String]] = Future.successful(Left("Not implemented"))
  def addLocus(id: String, locus: NewStrKitLocus): Future[Either[String, String]] = Future.successful(Left("Not implemented"))
  def update(kit: StrKit): Future[Either[String, String]] = Future.successful(Left("Not implemented"))
  def delete(id: String): Future[Either[String, String]] = Future.successful(Left("Not implemented"))
  def deleteAlias(id: String): Future[Either[String, String]] = Future.successful(Left("Not implemented"))
  def deleteLocus(id: String): Future[Either[String, String]] = Future.successful(Left("Not implemented"))
}
