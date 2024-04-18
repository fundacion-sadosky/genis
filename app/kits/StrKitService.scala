package kits

import javax.inject.{Inject, Singleton}

import bulkupload.ProtoProfileRepository
import profile.ProfileRepository
import services.{CacheService, Keys}

import play.api.i18n.Messages

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}


abstract class StrKitService {

  def get(id: String): Future[Option[StrKit]]
  def getFull(id: String): Future[Option[FullStrKit]]
  def list(): Future[Seq[StrKit]]
  def listFull(): Future[Seq[FullStrKit]]
  def findLociByKit(kitId: String): Future[List[StrKitLocus]]
  def getKitAlias: Future[Map[String, String]]
  def getLocusAlias: Future[Map[String, String]]
  def findLociByKits(kitIds: Seq[String]): Future[Map[String, List[StrKitLocus]]]
  def add(kit: FullStrKit): Future[Either[String, String]]
  def update(kit: FullStrKit): Future[Either[String, String]]
  def delete(id: String): Future[Either[String, String]]
}

@Singleton
class StrKitServiceImpl @Inject() (
  cache: CacheService,
  strKitRepository: StrKitRepository,
  profileRepository: ProfileRepository,
  protoProfileRepository: ProtoProfileRepository) extends StrKitService {

  private def cleanCache = {
    cache.pop(Keys.strKits)
  }

  override def getFull(id: String): Future[Option[FullStrKit]] = {
    strKitRepository.getFull(id)
  }

  override def get(id: String): Future[Option[StrKit]] = {
    strKitRepository.get(id).map {
      case Some(k) => Some(StrKit(k.id, k.name, k.`type`, k.locy_quantity, k.representative_parameter))
      case None => None
    }
  }

  override def list(): Future[Seq[StrKit]] = {
    this.listFull().map { seq =>
      seq.map(k => StrKit(k.id, k.name, k.`type`, k.locy_quantity, k.representative_parameter))
    }
  }

  override def listFull(): Future[Seq[FullStrKit]] = {
    cache.asyncGetOrElse(Keys.strKits)(strKitRepository.listFull())
  }

  override def findLociByKit(kitId: String): Future[List[StrKitLocus]] = {
    strKitRepository.findLociByKit(kitId)
  }

  override def findLociByKits(kitIds: Seq[String]): Future[Map[String, List[StrKitLocus]]] = {
    strKitRepository.findLociByKits(kitIds)
  }

  override def getKitAlias: Future[Map[String, String]] = {
    val fka = strKitRepository.getKitsAlias
    val fk = strKitRepository.list

    for {
      ka <- fka
      k <- fk
    } yield {
      val mk = k.map { x => (x.id, x.id) }.toMap
      ka ++ mk
    }
  }

  override def getLocusAlias: Future[Map[String, String]] = {
    val fla = strKitRepository.getLociAlias
    val fl = strKitRepository.getAllLoci

    for {
      la <- fla
      l <- fl
    } yield {
      val mss = l.map { x => (x, x) }.toMap
      la ++ mss
    }
  }

  override def add(full: FullStrKit): Future[Either[String, String]] = {
    if(full.locy_quantity<full.representative_parameter){
        Future.successful(Left(Messages("error.E0697")))
    }else{
    strKitRepository.runInTransactionAsync { implicit session =>
      val addResult = strKitRepository.add(StrKit(full.id, full.name, full.`type`, full.locy_quantity, full.representative_parameter))

      val aliasResult = addResult.fold(Left(_), r =>
      full.alias.foldLeft[Either[String,String]](Right(full.id)){
      case (prev,current) => prev.fold(Left(_),r=>strKitRepository.addAlias(full.id, current))
    })

      val locusResult = aliasResult.fold(Left(_), r =>
      full.locus.foldLeft[Either[String,String]](Right(full.id)){
      case (prev,current) => prev.fold(Left(_),r=>strKitRepository.addLocus(full.id, current))
    })

      locusResult match {
      case Left(_) => session.rollback()
      case Right(_) => this.cleanCache
    }
      locusResult
    }
    }
  }

  override def update(kit: FullStrKit): Future[Either[String, String]] = {
    if(kit.locy_quantity<kit.representative_parameter){
      Future.successful(Left(Messages("error.E0697")))
    }else {
      strKitRepository.runInTransactionAsync { implicit session =>
        val updateResult=strKitRepository.update(StrKit(kit.id, kit.name, kit.`type`, kit.locy_quantity, kit.representative_parameter))
        val deleteAliasResult = updateResult.fold(Left(_), r =>
          strKitRepository.deleteAlias(kit.id))
        val aliasResult = deleteAliasResult.fold(Left(_), r =>
          kit.alias.foldLeft[Either[String,String]](Right(kit.id)){
            case (prev,current) => prev.fold(Left(_),r=>strKitRepository.addAlias(kit.id, current))
          })
        aliasResult match {
          case Left(_) => session.rollback()
          case Right(_) => this.cleanCache
        }
        aliasResult
      }
    }
  }

  override def delete(id: String): Future[Either[String, String]] = {
    strKitRepository.runInTransactionAsync { implicit session =>
      // these need to be blocking to keep the session alive
      val canDeleteProfile = Await.result(profileRepository.canDeleteKit(id), Duration(10, SECONDS))
      val canDeleteProto = Await.result(protoProfileRepository.canDeleteKit(id), Duration(10, SECONDS))

      if (!canDeleteProfile) Left(Messages("error.E0695" ,id ))
      else if (!canDeleteProto) Left (Messages("error.E0696", id ))

      else {
        val aliasResult = strKitRepository.deleteAlias(id)
        val locusResult = aliasResult.fold(Left(_), r => strKitRepository.deleteLocus(id))
        val deleteResult = locusResult.fold(Left(_), r => strKitRepository.delete(id))

        deleteResult match {
          case Left(_) => session.rollback()
          case Right(_) => this.cleanCache
        }
        deleteResult
      }
    }

  }


}
