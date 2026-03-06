package kits

import scala.concurrent.Future

trait StrKitService {
  def get(id: String): Future[Option[StrKit]]
  def getFull(id: String): Future[Option[FullStrKit]]
  def list(): Future[Seq[StrKit]]
  def listFull(): Future[Seq[FullStrKit]]
  def findLociByKit(kitId: String): Future[List[StrKitLocus]]
  def findLociByKits(kitIds: Seq[String]): Future[Map[String, List[StrKitLocus]]]
  def getKitAlias: Future[Map[String, String]]
  def getLocusAlias: Future[Map[String, String]]
  def add(kit: StrKit): Future[Either[String, String]]
  def addAlias(id: String, alias: String): Future[Either[String, String]]
  def addLocus(id: String, locus: NewStrKitLocus): Future[Either[String, String]]
  def update(kit: StrKit): Future[Either[String, String]]
  def delete(id: String): Future[Either[String, String]]
  def deleteAlias(id: String): Future[Either[String, String]]
  def deleteLocus(id: String): Future[Either[String, String]]
}
