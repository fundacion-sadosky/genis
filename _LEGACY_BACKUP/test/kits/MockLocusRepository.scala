package kits

import play.api.Play.current
import play.api.db.slick._
import scala.concurrent.Future

class MockLocusRepository(addResult: Either[String,String],
                          deleteResult: Either[String,String],
                          canDelete: Boolean) extends LocusRepository {
  override def add(locus: Locus)(implicit session: Session): Either[String, String] = addResult
  override def update(locus: Locus)(implicit session: Session): Either[String, Unit] = Right(())
  override def deleteAlias(id: String)(implicit session: Session): Either[String, String] = deleteResult
  override def addAlias(id: String, alias: String)(implicit session: Session): Either[String, String] = addResult
  override def canDeleteLocusByKit(id: String)(implicit session: Session): Boolean = canDelete
  override def canDeleteLocusByLink(id: String)(implicit session: Session): Boolean = canDelete
  override def deleteLinks(id: String)(implicit session: Session): Either[String, String] = deleteResult
  override def delete(id: String)(implicit session: Session): Either[String, String] = deleteResult
  override def addLink(id: String, link: LocusLink)(implicit session: Session): Either[String, String] = addResult
  override def listFull(): Future[Seq[FullLocus]] = Future.successful(Seq())
  override def runInTransactionAsync[T](f: Session => T): Future[T] = Future { DB.withTransaction { implicit session => f(session) } }
  override def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[Locus]] = Future.successful(Nil)
  override def getLocusByAnalysisType(analysisType: Int): Future[Seq[Locus]] = Future.successful(Nil)
  override def deleteAliasById(id: String)(implicit session: Session): Either[String, String] = deleteResult
}

