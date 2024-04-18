package pedigree

import java.io.File
import java.util.Date
import javax.inject.{Inject, Named, Singleton}

import akka.actor.ActorSystem
import configdata.CategoryService
import inbox.{NotificationService, PedigreeMatchingInfo}
import kits.AnalysisType
import matching.{MatchStatus, MatchingService}
import pedigree.BayesianNetwork.Linkage
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.i18n.Messages

import scala.concurrent.{Await, Future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import probability.CalculationTypeService
import profile.{Profile, ProfileRepository}
import stats.PopulationBaseFrequencyService
import trace._
import types.{AlphanumericId, SampleCode}
import profiledata.ProfileDataRepository

import scala.concurrent.duration.Duration

abstract class PedigreeMatchesService {
  def getMatches(search: PedigreeMatchCardSearch): Future[Seq[MatchCardPedigree]]

  def countMatches(search: PedigreeMatchCardSearch): Future[Int]

  def getMatchById(matchingId: String): Future[Option[JsValue]]

  def getMatchesByGroup(search: PedigreeMatchGroupSearch): Future[Seq[PedigreeMatchResultData]]

  def countMatchesByGroup(search: PedigreeMatchGroupSearch): Future[Int]

  def allMatchesDiscarded(pedigreeId: Long): Future[Boolean]

  def discard(matchId: String, userId: String, isSuperUser: Boolean): Future[Either[String, String]]

  def deleteMatches(idPedigree: Long): Future[Either[String, Long]]

  def confirm(matchId: String, userId: String, isSuperUser: Boolean): Future[Either[String, String]]

  def getMatchesPedigree(search: PedMatchCardSearch): Future[Seq[MatchCardPedigrees]]

  def masiveGroupDiscardByGroup(id: String, group: String, isSuperUser: Boolean, userId: String, replicate:Boolean = true) : Future[Either[String, String]]

  def exportMatchesByGroup(matchesToExport : Seq[PedigreeMatchResultData])

  def getMatchesExportFile(): File
}
@Singleton
class PedigreeMatchesServiceImpl @Inject()(
  pedigreeDataRepository: PedigreeDataRepository,
  pedigreeMatchesRepository: PedigreeMatchesRepository,
  bayesianNetworkService: BayesianNetworkService,
  matchingService: MatchingService,
  pedigreeGenotypificationRepository: PedigreeGenotypificationRepository,
  profileRepository: ProfileRepository,
  calculationTypeService: CalculationTypeService,
  traceService: TraceService,
  profileDataRepo: ProfileDataRepository,
  notificationService: NotificationService,
  categoryService: CategoryService,
  @Named("exportProfilesPath") val exportProfilesPath: String = "") extends PedigreeMatchesService {

  override def getMatchesPedigree(search: PedMatchCardSearch): Future[Seq[MatchCardPedigrees]] = {
   profileDataRepo.getGlobalCode(search.profile.getOrElse("")).flatMap{
      case Some(globalCode)=>{
        doGetMatchesPedigree(search.copy(profile = Some(globalCode.text)))
      }
      case None => {
        doGetMatchesPedigree(search)
      }
    }
  }

  override def getMatches(search: PedigreeMatchCardSearch): Future[Seq[MatchCardPedigree]] = {
    profileDataRepo.getGlobalCode(search.profile.getOrElse("")).flatMap{
      case Some(globalCode)=>{
        doGetMatches(search.copy(profile = Some(globalCode.text)))
      }
      case None => {
        doGetMatches(search)
      }
    }
  }

  def doGetMatches(search: PedigreeMatchCardSearch): Future[Seq[MatchCardPedigree]] = {
    pedigreeMatchesRepository.getMatches(search) flatMap {
      matches => {
        Future.sequence(matches.map { m =>
          m._id match {
            case Right(globalCode) => { this.profileDataRepo.get(SampleCode(globalCode)).flatMap{
              profileOpt => {
              val pd = profileOpt.get
                (for{
                  hit <- pedigreeMatchesRepository.countProfilesHitPedigrees(pd.globalCode.text)
                  dis <- pedigreeMatchesRepository.countProfilesDiscardedPedigrees(pd.globalCode.text)
                  pending <- pedigreeMatchesRepository.profileNumberOfPendingMatches(pd.globalCode.text)
                  mejorLr <- pedigreeMatchesRepository.getMejorLrProf(pd.globalCode.text)
                  mejorLrData <- pedigreeDataRepository.getPedigreeMetaData(mejorLr.get.internalCode.toLong)
                }yield (hit,dis,pending,mejorLr, mejorLrData))
                .map{
                  case (hiit,diss,pendingg, mejorLr, mejorLrData)=>{
                    var mejor = MatchCardMejorLrPed(mejorLrData.get.pedigreeMetaData.courtCaseName, mejorLrData.get.pedigreeMetaData.name, Some(mejorLr.get.categoryId.get),
                      mejorLr.get.lr,mejorLr.get.estado)
                val categoria =  categoryService.getCategory(pd.category)
                    var prof = PedigreeMatchCard(pd.internalSampleCode, globalCode, m.assignee, m.lastMatchDate.date, m.count, "profile", "","",Some(categoria.get.name),hiit,pendingg,diss)
                    MatchCardPedigree(prof, mejor)
                  }
                }
                }
              } }
            case Left(idPedigree) => { pedigreeDataRepository.getPedigreeMetaData(idPedigree).flatMap {
              pedigreeOpt => {
                val pedigree = pedigreeOpt.get
                (for {
                  dis <- pedigreeMatchesRepository.numberOfDiscardedMatches(idPedigree)
                  pending <- pedigreeMatchesRepository.numberOfPendingMatches(idPedigree)
                  hit <- pedigreeMatchesRepository.numberOfHitMatches(idPedigree)
                  caseType <- pedigreeMatchesRepository.getTypeCourtCasePedigree(idPedigree)
                  mejorLr <- pedigreeMatchesRepository.getMejorLrPedigree(idPedigree)
                  mejorLrData <- this.profileDataRepo.get(SampleCode(mejorLr.get.internalCode))
                } yield (dis, pending, hit,caseType,mejorLr, mejorLrData))
                  .map {
                    case (dis, pen, hit ,caseType,mejorLr,mejorLrData) => {
                      val categoria =  categoryService.getCategory(AlphanumericId(mejorLr.get.categoryId.get))
                      var mejor = MatchCardMejorLrPed(mejorLr.get.internalCode, mejorLrData.get.internalSampleCode, Some(categoria.get.name),
                        mejorLr.get.lr,mejorLr.get.estado)
                      var pedi = PedigreeMatchCard(pedigree.pedigreeMetaData.name, idPedigree.toString, pedigree.pedigreeMetaData.assignee,
                        m.lastMatchDate.date, m.count, "pedigree", pedigree.pedigreeMetaData.courtCaseId.toString,
                        pedigree.pedigreeMetaData.courtCaseName,caseType, hit, pen, dis)
                      MatchCardPedigree(pedi,mejor)
                    }
                  }
              }
                }
                }
            }
          })
      }
    }
  }

  def doGetMatchesPedigree(search: PedMatchCardSearch): Future[Seq[MatchCardPedigrees]] = {
    var searchCard = PedigreeMatchCardSearch(search.user,search.isSuperUser,search.group,search.page,search.pageSize,
      search.profile,search.hourFrom,search.hourUntil, search.category, search.caseType,search.status, search.idCourtCase)
    var status = if(search.status.isEmpty) None else  Some(search.status.get.toString)

    pedigreeMatchesRepository.getMatches(searchCard) flatMap {
      matches => {
        Future.sequence( matches.map { m =>
        m._id match {
            case Right(globalCode) => { this.profileDataRepo.get(SampleCode(globalCode)).flatMap{
              profileOpt => {
                var searchCardGroup = PedigreeMatchGroupSearch(search.user,search.isSuperUser,globalCode,search.group,PedigreeMatchKind.Compatibility
                  ,search.pageMatch,search.pageSizeMatch,search.sortField, search.ascending ,status, search.idCourtCase)
                val pd = profileOpt.get
                (for{
                  hit <- pedigreeMatchesRepository.countProfilesHitPedigrees(pd.globalCode.text)
                  dis <- pedigreeMatchesRepository.countProfilesDiscardedPedigrees(pd.globalCode.text)
                  pending <- pedigreeMatchesRepository.profileNumberOfPendingMatches(pd.globalCode.text)
                  matchP <- pedigreeMatchesRepository.getMatchesByGroupPedigree(searchCardGroup)
                }yield (hit,dis,pending,matchP)).flatMap{
                    case (hiit,diss,pendingg, matchP)=>{
                       Future.sequence(matchP.map { m =>
                        pedigreeDataRepository.getPedigreeMetaData(m.internalCode.toLong).map { x =>
                          m.copy(sampleCode = x.map(_.pedigreeMetaData.name).getOrElse(""),internalCode = x.map(_.pedigreeMetaData.courtCaseName).getOrElse(""))
                        }
                      }).map(matchs => {
                        val categoria =  categoryService.getCategory(pd.category)
                        var prof = PedigreeMatchCard(pd.internalSampleCode, globalCode, m.assignee, m.lastMatchDate.date, m.count, "profile", "","",Some(categoria.get.name),hiit,pendingg,diss)
                        MatchCardPedigrees(prof, matchs)
                      })
                  }
              }
            }
            } }
            case Left(idPedigree) => { pedigreeDataRepository.getPedigreeMetaData(idPedigree).flatMap {
              pedigreeOpt => {
                var searchCardGroup = PedigreeMatchGroupSearch(search.user,search.isSuperUser,idPedigree.toString,search.group,PedigreeMatchKind.Compatibility
                  ,search.pageMatch,search.pageSizeMatch,search.sortField, search.ascending ,status, search.idCourtCase)
                val pedigree = pedigreeOpt.get
                (for {
                  dis <- pedigreeMatchesRepository.numberOfDiscardedMatches(idPedigree)
                  pending <- pedigreeMatchesRepository.numberOfPendingMatches(idPedigree)
                  hit <- pedigreeMatchesRepository.numberOfHitMatches(idPedigree)
                  caseType <- pedigreeMatchesRepository.getTypeCourtCasePedigree(idPedigree)
                  matchP <- pedigreeMatchesRepository.getMatchesByGroupPedigree(searchCardGroup)
                } yield (dis, pending, hit,caseType,matchP)).flatMap {
                    case (dis, pen, hit ,caseType,matchP) => {
                      Future.sequence(matchP.map { m =>
                        this.profileDataRepo.get(SampleCode(m.sampleCode)).map{ x=>
                          var auxCate = x.map(_.category.text).get
                          val categoria =  categoryService.getCategory(AlphanumericId(auxCate))
                          m.copy(sampleCode = x.map(_.internalSampleCode).getOrElse(""), categoryId = categoria.get.name)
                        } }).map( matchs => {
                        var pedi = PedigreeMatchCard(pedigree.pedigreeMetaData.name, idPedigree.toString, pedigree.pedigreeMetaData.assignee,
                          m.lastMatchDate.date, m.count, "pedigree", pedigree.pedigreeMetaData.courtCaseId.toString,
                          pedigree.pedigreeMetaData.courtCaseName, caseType, hit, pen, dis)
                        MatchCardPedigrees(pedi, matchs)
                      } )
                    }
                  }
              }
            }  }
          }
        } )
      }
    }
  }

  override def countMatches(search: PedigreeMatchCardSearch): Future[Int] = {
    profileDataRepo
      .getGlobalCode(search.profile.getOrElse(""))
      .flatMap{
      case Some(globalCode)=>{
        val count = pedigreeMatchesRepository
          .countMatches(search.copy(profile = Some(globalCode.text)))
        count
      }
      case None => {
        val count = pedigreeMatchesRepository.countMatches(search)
        count
      }
    }
  }

  override def getMatchById(matchingId: String): Future[Option[JsValue]] = {
    pedigreeMatchesRepository.getMatchById(matchingId) map {
      _ map { mr =>
        val matchResult = mr.asInstanceOf[PedigreeDirectLinkMatch]
        val result = Json.obj("globalCode" -> matchResult.profile.globalCode,
          "stringency" -> matchResult.result.stringency,
          "matchingAlleles" -> matchResult.result.matchingAlleles,
          "totalAlleles" -> matchResult.result.totalAlleles,
          "categoryId" -> matchResult.result.categoryId,
          "type" -> matchResult.`type`,
          "status" -> Json.obj(matchResult.profile.globalCode.text -> matchResult.profile.status,
            matchResult.pedigree.globalCode.text -> matchResult.pedigree.status))
        Json.obj("_id" -> matchingId, "results" -> JsArray(Seq(result)))
      }
    }
  }

  override def getMatchesByGroup(
    search: PedigreeMatchGroupSearch
  ): Future[Seq[PedigreeMatchResultData]] = {
     pedigreeMatchesRepository
       .getMatchesByGroup(search)
       .flatMap(
         x => Future.sequence(
           x.map {
             pedigree => {
               profileDataRepo
                 .get(pedigree.profile.globalCode)
                 .map{
                   case Some(si) => PedigreeMatchResultData(
                     pedigree,
                     si.internalSampleCode
                   )
                   case None => PedigreeMatchResultData(pedigree, "")
                 }
             }
           }
         )
       )
  }

  override def countMatchesByGroup(search: PedigreeMatchGroupSearch): Future[Int] = {
    pedigreeMatchesRepository.countMatchesByGroup(search)
  }

  override def allMatchesDiscarded(pedigreeId: Long): Future[Boolean] = {
    pedigreeMatchesRepository.allMatchesDiscarded(pedigreeId)
  }

  override def discard(
    matchId: String,
    userId: String,
    isSuperUser: Boolean
  ): Future[Either[String, String]] = {
    pedigreeMatchesRepository.getMatchById(matchId) flatMap { opt =>
      val matchResult = opt.get
      val result = if (isSuperUser || ( matchResult.pedigree.assignee == userId)) {
        pedigreeMatchesRepository.discardProfile(matchId) flatMap {
          case Left(error) => Future.successful(Left(error))
          case Right(_) => pedigreeMatchesRepository.discardPedigree(matchId)
        }
      } else {
        Future.successful(Left(Messages("error.E0642")))
      }
      result.foreach {
            
        case Right(_) =>
          pedigreeDataRepository
            .getPedigreeDescriptionById(matchResult.pedigree.idPedigree)
            .map {
              case (pedName, courtCaseName) => traceService.add(
                Trace(
                  matchResult.profile.globalCode,
                  userId,
                  new Date(),
                  PedigreeDiscardInfo(
                    matchResult._id.id,
                    matchResult.pedigree.idPedigree,
                    matchResult.pedigree.assignee,
                    matchResult.`type`,
                    courtCaseName,
                    pedName
                  )
                )
              )
            }
          traceService
            .addTracePedigree(
              TracePedigree(
                matchResult.pedigree.idPedigree,
                userId,
                new Date(),
                PedigreeDiscardInfo2(
                  matchResult._id.id,
                  matchResult.pedigree.idPedigree,
                  matchResult.profile.globalCode.text,
                  matchResult.pedigree.assignee,
                  matchResult.`type`
                )
              )
            )
          val notification = PedigreeMatchingInfo(
            matchResult.profile.globalCode,
            Some(matchResult.pedigree.caseType),
            Some(matchResult.pedigree.idCourtCase.toString)
          )
          notificationService
            .solve(matchResult.pedigree.assignee, notification)
        case Left(error) => ()
      }
      result
    }

  }

  override def confirm(matchId: String, userId: String, isSuperUser: Boolean): Future[Either[String, String]] = {
    pedigreeMatchesRepository.getMatchById(matchId) flatMap { opt =>
      val matchResult = opt.get

/*      val result = if (isSuperUser || (matchResult.profile.assignee == userId && matchResult.pedigree.assignee == userId)) {
        pedigreeMatchesRepository.confirmProfile(matchId) flatMap {
          case Left(error) => Future.successful(Left(error))
          case Right(_) => pedigreeMatchesRepository.confirmPedigree(matchId)
        }
      } else if (matchResult.profile.assignee == userId) {
        pedigreeMatchesRepository.confirmProfile(matchId)
      } else if (matchResult.pedigree.assignee == userId) {
        pedigreeMatchesRepository.confirmPedigree(matchId)
      } else {
        Future.successful(Left(Messages("error.E0204")))
      }*/ //SACO PERMISOS REVISAR
      val result = pedigreeMatchesRepository.confirmProfile(matchId) flatMap {
          case Left(error) => Future.successful(Left(error))
          case Right(_) => pedigreeMatchesRepository.confirmPedigree(matchId)
        }

      result.foreach {
        case Right(_) =>
          traceService.addTracePedigree(TracePedigree(matchResult.pedigree.idPedigree, userId, new Date(),
            PedigreeConfirmInfo2(matchResult._id.id, matchResult.pedigree.idPedigree,matchResult.profile.globalCode.text,
              matchResult.pedigree.assignee, matchResult.`type`)))
          pedigreeDataRepository.getPedigreeDescriptionById(matchResult.pedigree.idPedigree).map{
            case (pedName,courtCaseName) => traceService.add(Trace(matchResult.profile.globalCode, userId, new Date(),
              PedigreeConfirmInfo(matchResult._id.id, matchResult.pedigree.idPedigree,
                matchResult.pedigree.assignee, matchResult.`type`,courtCaseName,pedName)))
          }

          val notification = PedigreeMatchingInfo(matchResult.profile.globalCode, Some(matchResult.pedigree.caseType), Some(matchResult.pedigree.idCourtCase.toString))
            notificationService.solve(matchResult.pedigree.assignee, notification)
        case Left(error) => ()
      }
      result
    }

  }

  override def deleteMatches(idPedigree: Long): Future[Either[String, Long]] = {
    pedigreeMatchesRepository.deleteMatches(idPedigree)
  }

  override def masiveGroupDiscardByGroup(id: String, group: String, isSuperUser: Boolean, userId: String, replicate:Boolean = true) : Future[Either[String, String]] = {
    val matches = Await.result(pedigreeMatchesRepository.getAllMatchNonDiscardedByGroup(id, group), Duration.Inf)
    if(matches.isEmpty) {
      Future.successful(Left("Sin matches"))
    } else {
      val futures = for (matchResult <- matches) yield discard(matchResult._id.id, userId, isSuperUser )

      Future.sequence(futures) flatMap { result => {
          if (result.forall(_.isRight)) {
            Future(Right(id))
          } else {
            Future(Left(result.filter(_.isLeft).map(x => x.left.get).mkString("", ",", "")))
          }
        }
      }
    }
  }

  override def exportMatchesByGroup(matchesToExport : Seq[PedigreeMatchResultData]) = {
    MPIMatchesExporter.createMatchLimsArchive(matchesToExport, exportProfilesPath/*"/home/pdg/ExportacionPerfiles"*/, pedigreeDataRepository)
  }

  override def getMatchesExportFile(): File = {
    val file = new java.io.File(s"$exportProfilesPath${File.separator}MatchesMPIFile.csv")
    file
  }


}