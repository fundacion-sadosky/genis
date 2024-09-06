package kits

import javax.inject.{Inject, Singleton}
import profile.{Allele, Profile}
import matching.{AleleRange, NewMatchingResult}
import pedigree.MutationService
import services.Keys
import services.CacheService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.i18n.{Messages, MessagesApi}

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

trait LocusService {
  def add(locus: FullLocus): Future[Either[String, String]]
  def update(locus: FullLocus): Future[Either[String, Unit]]
  def listFull(): Future[Seq[FullLocus]]
  def list(): Future[Seq[Locus]]
  def delete(id: String): Future[Either[String, String]]
  def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[String]]
  def getLocusByAnalysisType(analysisType: Int): Future[Seq[String]]
  def locusRangeMap(): NewMatchingResult.AlleleMatchRange
  def saveLocusAlleles(list:List[(String,Double)]):Future[Either[String, Int]]
  def saveLocusAllelesFromProfile(profile:Profile):Future[Either[String, Int]]
  def refreshAllKis():Future[Unit]
}

@Singleton
class LocusServiceImpl @Inject() (cache: CacheService, locusRepository: LocusRepository,mutationService: MutationService, messagesApi: MessagesApi) extends LocusService {
  implicit val messages: Messages = messagesApi.preferred(Seq.empty)

  def locusRangeMap(): NewMatchingResult.AlleleMatchRange = {
    val listLocus = Await.result(this.list(), Duration(100, SECONDS))
    listLocus.map(locus=>locus.id -> AleleRange(locus.minAlleleValue.getOrElse(0),locus.maxAlleleValue.getOrElse(99))).toMap
  }
  private def cleanCache = {
    cache.pop(Keys.locus)
  }

  override def add(full: FullLocus): Future[Either[String, String]] = {
    locusRepository.runInTransactionAsync { implicit session =>
      val addResult = locusRepository.add(full.locus)

      val aliasResult = addResult.fold(Left(_), r =>
        full.alias.foldLeft[Either[String,String]](Right(full.locus.id)){
          case (prev,current) => prev.fold(Left(_),r=>locusRepository.addAlias(full.locus.id, current))
      })

      val linkResult = aliasResult.fold(Left(_), r =>
        full.links.foldLeft[Either[String,String]](Right(full.locus.id)){
          case (prev,current) => prev.fold(Left(_),r=>locusRepository.addLink(full.locus.id, current))
      })

      linkResult match {
        case Left(_) => session.rollback()
        case Right(_) => this.cleanCache
      }
      linkResult
    }.map(result => {
      if(result.isRight && full.locus.analysisType == 1){
        Future{
          this.mutationService.addLocus(full)
        }
      }
      result
    })
  }
  override def update(locus: FullLocus): Future[Either[String, Unit]] = {
    listFull().flatMap(oldListFullLocus =>
    {
      locusRepository.runInTransactionAsync { implicit session =>

        val oldFullLocusOpt = oldListFullLocus.find(_.locus.id == locus.locus.id)
        oldFullLocusOpt match {
          case None => Left("No existe el marcador")
          case Some(oldFullLocus) => {
            val updateLocusResult = locusRepository.update(locus.locus)

            val aliasesToAdd = locus.alias.filter(a => !oldFullLocus.alias.contains(a))
            val aliasesToDelete = oldFullLocus.alias.filter(a => !locus.alias.contains(a))

            val aliasAddResult = updateLocusResult.fold(Left(_), r =>
              aliasesToAdd.foldLeft[Either[String,String]](Right(locus.locus.id)){
                case (prev,current) => prev.fold(Left(_),r=>locusRepository.addAlias(locus.locus.id, current))
              })
            val aliasDeleteResult = aliasAddResult.fold(Left(_), r =>
              aliasesToDelete.foldLeft[Either[String,String]](Right(locus.locus.id)){
                case (prev,current) => prev.fold(Left(_),r=>locusRepository.deleteAliasById(current))
              })
            aliasDeleteResult match {
              case Left(_) => session.rollback()
              case Right(_) => this.cleanCache
            }
            updateLocusResult
          }
        }
      }
    }
    )

  }
  override def listFull(): Future[Seq[FullLocus]] = {
    cache.asyncGetOrElse(Keys.locus)(locusRepository.listFull())
  }

  override def list(): Future[Seq[Locus]] = {
    this.listFull().map { seq =>
      seq.map(_.locus)
    }
  }

  override def delete(id: String): Future[Either[String, String]] = {
    locusRepository.runInTransactionAsync { implicit session =>
      val canDeleteByKit = locusRepository.canDeleteLocusByKit(id)
      val canDeleteByLink = locusRepository.canDeleteLocusByLink(id)

      if (!canDeleteByKit) Left(Messages("error.E0693", id ))
      else if (!canDeleteByLink) Left(Messages("error.E0687", id))

      else {
        val aliasResult = locusRepository.deleteAlias(id)
        val linkResult = aliasResult.fold(Left(_), r => locusRepository.deleteLinks(id))
        val deleteResult = linkResult.fold(Left(_), r => locusRepository.delete(id))

        deleteResult match {
          case Left(_) => session.rollback()
          case Right(_) => this.cleanCache
        }
        deleteResult
      }
    }
  }

  override def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[String]] = {
    locusRepository.getLocusByAnalysisTypeName(analysisType) map { locus =>
      locus.map(l => l.id)
    }
  }

  override def getLocusByAnalysisType(analysisType: Int): Future[Seq[String]] = {
    locusRepository.getLocusByAnalysisType(analysisType) map { locus =>
      locus.map(l => l.id)
    }
  }
  override def saveLocusAlleles(list:List[(String,Double)]):Future[Either[String, Int]] = {
    this.mutationService.saveLocusAlleles(list)
  }

  override def saveLocusAllelesFromProfile(profile:Profile):Future[Either[String, Int]] = {
    val locusAlleles = profile.genotypification.get(1)
      .map(x => x.toList
        .map(y => y._2
          .map { case Allele(z) => {

            (y._1.toString, z.toDouble)
          }
          case _ => {
            ("",0.0)
          }
          }
        )).getOrElse(List.empty).flatten.filter(!_._1.isEmpty)
    this.saveLocusAlleles(locusAlleles)
  }

  override def refreshAllKis():Future[Unit] = {
    mutationService.refreshAllKisSecuential()
  }
}