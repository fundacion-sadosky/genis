package kits

import play.api.db.slick.{DB, Session}

import scala.concurrent.Future
import bulkupload.ProtoProfileRepository
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.Play.current
import play.api.db.slick._
import profile.ProfileRepository
import services.{CacheService, Keys}
import specs.PdgSpec
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

class MockKitRepository(addResult: Either[String,String],
                        deleteResult: Either[String,String],
                        updateResult: Either[String,String]=Right(""),
                        updateLocus:Either[String,String] = Right(""),
                        deleteDiscreteValuesLocus:Either[String,String]= Right("")) extends StrKitRepository {
  override def add(kit: StrKit)(implicit session: Session): Either[String, String] = addResult
  override def deleteAlias(id: String)(implicit session: Session): Either[String, String] = deleteResult
  override def addAlias(id: String, alias: String)(implicit session: Session): Either[String, String] = addResult
  override def deleteLocus(id: String)(implicit session: Session): Either[String, String] = deleteResult
  override def update(kit: StrKit)(implicit session: Session): Either[String, String]=updateResult
  override def delete(id: String)(implicit session: Session): Either[String, String] = deleteResult
  override def addLocus(id: String, locus: NewStrKitLocus)(implicit session: Session): Either[String, String] = addResult
  override def listFull(): Future[Seq[FullStrKit]] = Future.successful(Seq())
  override def list(): Future[Seq[StrKit]] = Future.successful(Seq())
  override def getKitsAlias: Future[Map[String, String]] = Future.successful(Map())
  override def getAllLoci: Future[Seq[String]] = Future.successful(Seq())
  override def getLociAlias: Future[Map[String, String]] = Future.successful(Map())
  override def findLociByKit(kitId: String): Future[List[StrKitLocus]] = Future.successful(Nil)
  override def findLociByKits(kitIds: Seq[String]): Future[Map[String, List[StrKitLocus]]] = Future.successful(Map())
  override def runInTransactionAsync[T](f: Session => T): Future[T] = Future { DB.withTransaction { implicit session => f(session) } }
  override def get(id: String): Future[Option[StrKit]] = Future.successful(None)
  override def getFull(id: String): Future[Option[FullStrKit]] = Future.successful(None)
}