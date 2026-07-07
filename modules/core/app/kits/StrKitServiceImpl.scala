package kits

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import play.api.i18n.MessagesApi

class StrKitServiceImpl @Inject()(
  repository: StrKitRepository,
  messagesApi: MessagesApi
)(implicit ec: ExecutionContext) extends StrKitService {

  private implicit val messages: play.api.i18n.Messages = messagesApi.preferred(Seq.empty)
  def get(id: String): Future[Option[StrKit]] = repository.get(id)
  def getFull(id: String): Future[Option[FullStrKit]] = repository.getFull(id)
  def list(): Future[Seq[StrKit]] = repository.list()
  def listFull(): Future[Seq[FullStrKit]] = repository.listFull()
  def findLociByKit(kitId: String): Future[List[StrKitLocus]] = repository.findLociByKit(kitId)
  def findLociByKits(kitIds: Seq[String]): Future[Map[String, List[StrKitLocus]]] = repository.findLociByKits(kitIds)
  def getKitAlias: Future[Map[String, String]] =
    for
      aliases <- repository.getKitsAlias
      kits    <- repository.list()
    yield aliases ++ kits.map(k => k.id -> k.id)

  def getLocusAlias: Future[Map[String, String]] =
    for
      aliases <- repository.getLociAlias
      lociIds <- repository.getAllLoci
    yield aliases ++ lociIds.map(id => id -> id)
  def add(full: FullStrKit): Future[Either[String, String]] =
    if full.locy_quantity < full.representative_parameter then
      Future.successful(Left(messages("error.E0697")))
    else repository.addFull(full)
  def addAlias(id: String, alias: String): Future[Either[String, String]] = repository.addAlias(id, alias)
  def addLocus(id: String, locus: NewStrKitLocus): Future[Either[String, String]] = repository.addLocus(id, locus)
  def update(full: FullStrKit): Future[Either[String, String]] =
    if full.locy_quantity < full.representative_parameter then
      Future.successful(Left(messages("error.E0697")))
    else repository.updateFull(full)
  def delete(id: String): Future[Either[String, String]] = repository.delete(id)
  def deleteAlias(id: String): Future[Either[String, String]] = repository.deleteAlias(id)
  def deleteLocus(id: String): Future[Either[String, String]] = repository.deleteLocus(id)
}
