package kits

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

class StrKitServiceImpl @Inject()(
  repository: StrKitRepository
)(implicit ec: ExecutionContext) extends StrKitService {
  def get(id: String): Future[Option[StrKit]] = repository.get(id)
  def getFull(id: String): Future[Option[FullStrKit]] = repository.getFull(id)
  def list(): Future[Seq[StrKit]] = repository.list()
  def listFull(): Future[Seq[FullStrKit]] = repository.listFull()
  def findLociByKit(kitId: String): Future[List[StrKitLocus]] = repository.findLociByKit(kitId)
  def findLociByKits(kitIds: Seq[String]): Future[Map[String, List[StrKitLocus]]] = repository.findLociByKits(kitIds)
  def getKitAlias: Future[Map[String, String]] = repository.getKitsAlias
  def getLocusAlias: Future[Map[String, String]] = repository.getLociAlias
  def add(kit: StrKit): Future[Either[String, String]] = repository.add(kit)
  def addAlias(id: String, alias: String): Future[Either[String, String]] = repository.addAlias(id, alias)
  def addLocus(id: String, locus: NewStrKitLocus): Future[Either[String, String]] = repository.addLocus(id, locus)
  def update(kit: StrKit): Future[Either[String, String]] = repository.update(kit)
  def delete(id: String): Future[Either[String, String]] = repository.delete(id)
  def deleteAlias(id: String): Future[Either[String, String]] = repository.deleteAlias(id)
  def deleteLocus(id: String): Future[Either[String, String]] = repository.deleteLocus(id)
}
