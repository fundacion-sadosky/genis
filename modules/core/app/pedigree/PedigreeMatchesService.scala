package pedigree

import java.io.File
import java.util.Date

import configdata.CategoryService
import inbox.{NotificationService, PedigreeMatchingInfo}
import matching.MatchingService
import play.api.libs.json.{JsArray, JsValue, Json}
import profiledata.ProfileDataRepository
import trace.*
import types.{AlphanumericId, SampleCode}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

// ---------------------------------------------------------------------------
// PedigreeMatchesService trait
// ---------------------------------------------------------------------------

trait PedigreeMatchesService:
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
  def masiveGroupDiscardByGroup(id: String, group: String, isSuperUser: Boolean, userId: String, replicate: Boolean = true): Future[Either[String, String]]
  def exportMatchesByGroup(matchesToExport: Seq[PedigreeMatchResultData]): Unit
  def getMatchesExportFile(): File

// ---------------------------------------------------------------------------
// Stub
// ---------------------------------------------------------------------------

@jakarta.inject.Singleton
class PedigreeMatchesServiceStub extends PedigreeMatchesService:
  override def getMatches(search: PedigreeMatchCardSearch): Future[Seq[MatchCardPedigree]] = Future.successful(Seq.empty)
  override def countMatches(search: PedigreeMatchCardSearch): Future[Int] = Future.successful(0)
  override def getMatchById(matchingId: String): Future[Option[JsValue]] = Future.successful(None)
  override def getMatchesByGroup(search: PedigreeMatchGroupSearch): Future[Seq[PedigreeMatchResultData]] = Future.successful(Seq.empty)
  override def countMatchesByGroup(search: PedigreeMatchGroupSearch): Future[Int] = Future.successful(0)
  override def allMatchesDiscarded(pedigreeId: Long): Future[Boolean] = Future.successful(false)
  override def discard(matchId: String, userId: String, isSuperUser: Boolean): Future[Either[String, String]] = Future.successful(Left("Not implemented"))
  override def deleteMatches(idPedigree: Long): Future[Either[String, Long]] = Future.successful(Left("Not implemented"))
  override def confirm(matchId: String, userId: String, isSuperUser: Boolean): Future[Either[String, String]] = Future.successful(Left("Not implemented"))
  override def getMatchesPedigree(search: PedMatchCardSearch): Future[Seq[MatchCardPedigrees]] = Future.successful(Seq.empty)
  override def masiveGroupDiscardByGroup(id: String, group: String, isSuperUser: Boolean, userId: String, replicate: Boolean = true): Future[Either[String, String]] = Future.successful(Left("Not implemented"))
  override def exportMatchesByGroup(matchesToExport: Seq[PedigreeMatchResultData]): Unit = ()
  override def getMatchesExportFile(): File = new File("MatchesMPIFile.csv")

// ---------------------------------------------------------------------------
// Implementation
// ---------------------------------------------------------------------------

@jakarta.inject.Singleton
class PedigreeMatchesServiceImpl @jakarta.inject.Inject() (
  pedigreeDataRepository: PedigreeDataRepository,
  pedigreeMatchesRepository: PedigreeMatchesRepository,
  bayesianNetworkService: BayesianNetworkService,
  matchingService: MatchingService,
  pedigreeGenotypificationRepository: PedigreeGenotypificationRepository,
  profileDataRepo: ProfileDataRepository,
  traceService: TraceService,
  notificationService: NotificationService,
  categoryService: CategoryService,
  @jakarta.inject.Named("exportProfilesPath") val exportProfilesPath: String = ""
)(implicit ec: ExecutionContext) extends PedigreeMatchesService:

  private val duration = Duration.Inf

  override def getMatchesPedigree(search: PedMatchCardSearch): Future[Seq[MatchCardPedigrees]] =
    profileDataRepo.getGlobalCode(search.profile.getOrElse("")).flatMap {
      case Some(globalCode) => doGetMatchesPedigree(search.copy(profile = Some(globalCode.text)))
      case None             => doGetMatchesPedigree(search)
    }

  override def getMatches(search: PedigreeMatchCardSearch): Future[Seq[MatchCardPedigree]] =
    profileDataRepo.getGlobalCode(search.profile.getOrElse("")).flatMap {
      case Some(globalCode) => doGetMatches(search.copy(profile = Some(globalCode.text)))
      case None             => doGetMatches(search)
    }

  private def doGetMatches(search: PedigreeMatchCardSearch): Future[Seq[MatchCardPedigree]] =
    pedigreeMatchesRepository.getMatches(search).flatMap { matches =>
      Future.sequence(matches.map { m =>
        m._id match
          case Right(globalCode) =>
            profileDataRepo.get(SampleCode(globalCode)).flatMap {
              case None => Future.successful(None)
              case Some(pd) =>
                (for {
                  hit     <- pedigreeMatchesRepository.countProfilesHitPedigrees(pd.globalCode.text)
                  dis     <- pedigreeMatchesRepository.countProfilesDiscardedPedigrees(pd.globalCode.text)
                  pending <- pedigreeMatchesRepository.profileNumberOfPendingMatches(pd.globalCode.text)
                  mejorLr <- pedigreeMatchesRepository.getMejorLrProf(pd.globalCode.text)
                  mejorLrData <- pedigreeDataRepository.getPedigreeMetaData(mejorLr.get.internalCode.toLong)
                } yield (hit, dis, pending, mejorLr, mejorLrData)).flatMap {
                  case (hiit, diss, pendingg, mejorLr, mejorLrData) =>
                    categoryService.getCategory(pd.category).map { categoryOpt =>
                      val mejor = MatchCardMejorLrPed(
                        mejorLrData.get.pedigreeMetaData.id.toString,
                        mejorLrData.get.pedigreeMetaData.courtCaseName,
                        mejorLrData.get.pedigreeMetaData.name,
                        Some(mejorLr.get.categoryId.get),
                        mejorLr.get.lr,
                        mejorLr.get.estado
                      )
                      val prof = PedigreeMatchCard(
                        pd.internalSampleCode,
                        globalCode,
                        m.assignee,
                        m.lastMatchDate.date,
                        m.count,
                        "profile",
                        mejorLrData.get.pedigreeMetaData.courtCaseId.toString,
                        mejorLrData.get.pedigreeMetaData.courtCaseName,
                        Some(categoryOpt.get.name),
                        hiit,
                        pendingg,
                        diss
                      )
                      Some(MatchCardPedigree(prof, mejor))
                    }
                }
            }
          case Left(idPedigree) =>
            pedigreeDataRepository.getPedigreeMetaData(idPedigree).flatMap {
              case None => Future.successful(None)
              case Some(pedigreeData) =>
                (for {
                  dis     <- pedigreeMatchesRepository.numberOfDiscardedMatches(idPedigree)
                  pending <- pedigreeMatchesRepository.numberOfPendingMatches(idPedigree)
                  hit     <- pedigreeMatchesRepository.numberOfHitMatches(idPedigree)
                  caseType <- pedigreeMatchesRepository.getTypeCourtCasePedigree(idPedigree)
                  mejorLr <- pedigreeMatchesRepository.getMejorLrPedigree(idPedigree)
                  mejorLrData <- profileDataRepo.get(SampleCode(mejorLr.get.internalCode))
                } yield (dis, pending, hit, caseType, mejorLr, mejorLrData)).flatMap {
                  case (dis, pen, hit, caseType, mejorLr, mejorLrData) =>
                    categoryService.getCategory(AlphanumericId(mejorLr.get.categoryId.get)).map { categoryOpt =>
                      val mejor = MatchCardMejorLrPed(
                        mejorLr.get.id.toString,
                        mejorLr.get.internalCode,
                        mejorLrData.get.internalSampleCode,
                        Some(categoryOpt.get.name),
                        mejorLr.get.lr,
                        mejorLr.get.estado
                      )
                      val pedi = PedigreeMatchCard(
                        pedigreeData.pedigreeMetaData.name,
                        idPedigree.toString,
                        pedigreeData.pedigreeMetaData.assignee,
                        m.lastMatchDate.date,
                        m.count,
                        "pedigree",
                        pedigreeData.pedigreeMetaData.courtCaseId.toString,
                        pedigreeData.pedigreeMetaData.courtCaseName,
                        caseType,
                        hit,
                        pen,
                        dis
                      )
                      Some(MatchCardPedigree(pedi, mejor))
                    }
                }
            }
      }).map(_.flatten)
    }

  private def doGetMatchesPedigree(search: PedMatchCardSearch): Future[Seq[MatchCardPedigrees]] =
    val status = if search.status.isEmpty then None else Some(search.status.get.toString)
    pedigreeMatchesRepository.getMatches(PedigreeMatchCardSearch(search.user, search.isSuperUser, search.group, search.page, search.pageSize, search.profile, search.hourFrom, search.hourUntil, search.category, search.caseType, search.status, search.idCourtCase)).flatMap { matches =>
      Future.sequence(matches.map { m =>
        m._id match
          case Right(globalCode) =>
            profileDataRepo.get(SampleCode(globalCode)).flatMap {
              case None => Future.successful(None)
              case Some(pd) =>
                val searchCardGroup = PedigreeMatchGroupSearch(search.user, search.isSuperUser, globalCode, search.group, PedigreeMatchKind.Compatibility, search.pageMatch, search.pageSizeMatch, search.sortField, search.ascending, status, search.idCourtCase)
                (for {
                  hit     <- pedigreeMatchesRepository.countProfilesHitPedigrees(pd.globalCode.text)
                  dis     <- pedigreeMatchesRepository.countProfilesDiscardedPedigrees(pd.globalCode.text)
                  pending <- pedigreeMatchesRepository.profileNumberOfPendingMatches(pd.globalCode.text)
                  matchP  <- pedigreeMatchesRepository.getMatchesByGroupPedigree(searchCardGroup)
                } yield (hit, dis, pending, matchP)).flatMap {
                  case (hiit, diss, pendingg, matchP) =>
                    Future.sequence(matchP.map { mr =>
                      pedigreeDataRepository.getPedigreeMetaData(mr.internalCode.toLong).map { x =>
                        mr.copy(
                          sampleCode    = x.map(_.pedigreeMetaData.name).getOrElse(""),
                          internalCode  = x.map(_.pedigreeMetaData.courtCaseName).getOrElse(""),
                          courtCaseId   = x.map(_.pedigreeMetaData.courtCaseId.toString).getOrElse(""),
                          courtCaseName = x.map(_.pedigreeMetaData.courtCaseName).getOrElse("")
                        )
                      }
                    }).flatMap { matchs =>
                      categoryService.getCategory(pd.category).map { categoryOpt =>
                        val prof = PedigreeMatchCard(
                          pd.internalSampleCode, globalCode, m.assignee, m.lastMatchDate.date, m.count,
                          "profile", "", "", Some(categoryOpt.get.name), hiit, pendingg, diss
                        )
                        Some(MatchCardPedigrees(prof, matchs))
                      }
                    }
                }
            }
          case Left(idPedigree) =>
            pedigreeDataRepository.getPedigreeMetaData(idPedigree).flatMap {
              case None => Future.successful(None)
              case Some(pedigreeData) =>
                val searchCardGroup = PedigreeMatchGroupSearch(search.user, search.isSuperUser, idPedigree.toString, search.group, PedigreeMatchKind.Compatibility, search.pageMatch, search.pageSizeMatch, search.sortField, search.ascending, status, search.idCourtCase)
                (for {
                  dis      <- pedigreeMatchesRepository.numberOfDiscardedMatches(idPedigree)
                  pending  <- pedigreeMatchesRepository.numberOfPendingMatches(idPedigree)
                  hit      <- pedigreeMatchesRepository.numberOfHitMatches(idPedigree)
                  caseType <- pedigreeMatchesRepository.getTypeCourtCasePedigree(idPedigree)
                  matchP   <- pedigreeMatchesRepository.getMatchesByGroupPedigree(searchCardGroup)
                } yield (dis, pending, hit, caseType, matchP)).flatMap {
                  case (dis, pen, hit, caseType, matchP) =>
                    Future.sequence(matchP.map { mr =>
                      profileDataRepo.get(SampleCode(mr.sampleCode)).flatMap { x =>
                        val auxCate = x.map(_.category.text).getOrElse("")
                        categoryService.getCategory(AlphanumericId(auxCate)).map { categoryOpt =>
                          mr.copy(sampleCode = x.map(_.internalSampleCode).getOrElse(""), categoryId = categoryOpt.get.name)
                        }
                      }
                    }).map { matchs =>
                      val pedi = PedigreeMatchCard(
                        pedigreeData.pedigreeMetaData.name, idPedigree.toString, pedigreeData.pedigreeMetaData.assignee,
                        m.lastMatchDate.date, m.count, "pedigree",
                        pedigreeData.pedigreeMetaData.courtCaseId.toString, pedigreeData.pedigreeMetaData.courtCaseName,
                        caseType, hit, pen, dis
                      )
                      Some(MatchCardPedigrees(pedi, matchs))
                    }
                }
            }
      }).map(_.flatten)
    }

  override def countMatches(search: PedigreeMatchCardSearch): Future[Int] =
    profileDataRepo.getGlobalCode(search.profile.getOrElse("")).flatMap {
      case Some(globalCode) => pedigreeMatchesRepository.countMatches(search.copy(profile = Some(globalCode.text)))
      case None             => pedigreeMatchesRepository.countMatches(search)
    }

  override def getMatchById(matchingId: String): Future[Option[JsValue]] =
    pedigreeMatchesRepository.getMatchById(matchingId).map {
      _.map { mr =>
        val matchResult = mr.asInstanceOf[PedigreeDirectLinkMatch]
        val result = Json.obj(
          "globalCode"     -> matchResult.profile.globalCode,
          "stringency"     -> matchResult.result.stringency,
          "matchingAlleles" -> matchResult.result.matchingAlleles,
          "totalAlleles"   -> matchResult.result.totalAlleles,
          "categoryId"     -> matchResult.result.categoryId,
          "type"           -> matchResult.`type`,
          "status"         -> Json.obj(
            matchResult.profile.globalCode.text -> matchResult.profile.status,
            matchResult.pedigree.globalCode.text -> matchResult.pedigree.status
          )
        )
        Json.obj("_id" -> matchingId, "results" -> JsArray(Seq(result)))
      }
    }

  override def getMatchesByGroup(search: PedigreeMatchGroupSearch): Future[Seq[PedigreeMatchResultData]] =
    pedigreeMatchesRepository.getMatchesByGroup(search).flatMap { xs =>
      Future.sequence(xs.map { pedigree =>
        profileDataRepo.get(pedigree.profile.globalCode).map {
          case Some(si) => PedigreeMatchResultData(pedigree, si.internalSampleCode)
          case None     => PedigreeMatchResultData(pedigree, "")
        }
      })
    }

  override def countMatchesByGroup(search: PedigreeMatchGroupSearch): Future[Int] =
    pedigreeMatchesRepository.countMatchesByGroup(search)

  override def allMatchesDiscarded(pedigreeId: Long): Future[Boolean] =
    pedigreeMatchesRepository.allMatchesDiscarded(pedigreeId)

  override def discard(matchId: String, userId: String, isSuperUser: Boolean): Future[Either[String, String]] =
    pedigreeMatchesRepository.getMatchById(matchId).flatMap { opt =>
      val matchResult = opt.get
      val result =
        if isSuperUser || matchResult.pedigree.assignee == userId then
          pedigreeMatchesRepository.discardProfile(matchId).flatMap {
            case Left(error) => Future.successful(Left(error))
            case Right(_)    => pedigreeMatchesRepository.discardPedigree(matchId)
          }
        else
          Future.successful(Left("error.E0642"))

      result.foreach {
        case Right(_) =>
          pedigreeDataRepository.getPedigreeDescriptionById(matchResult.pedigree.idPedigree).map {
            case (pedName, courtCaseName) =>
              traceService.add(Trace(matchResult.profile.globalCode, userId, new Date(),
                PedigreeDiscardInfo(matchResult._id.id, matchResult.pedigree.idPedigree, matchResult.pedigree.assignee, matchResult.`type`, courtCaseName, pedName)))
          }
          traceService.addTracePedigree(TracePedigree(matchResult.pedigree.idPedigree, userId, new Date(),
            PedigreeDiscardInfo2(matchResult._id.id, matchResult.pedigree.idPedigree, matchResult.profile.globalCode.text, matchResult.pedigree.assignee, matchResult.`type`)))
          notificationService.solve(matchResult.pedigree.assignee,
            PedigreeMatchingInfo(matchResult.profile.globalCode, Some(matchResult.pedigree.caseType), Some(matchResult.pedigree.idCourtCase.toString)))
        case Left(_) => ()
      }
      result
    }

  override def confirm(matchId: String, userId: String, isSuperUser: Boolean): Future[Either[String, String]] =
    pedigreeMatchesRepository.getMatchById(matchId).flatMap { opt =>
      val matchResult = opt.get
      val result = pedigreeMatchesRepository.confirmProfile(matchId).flatMap {
        case Left(error) => Future.successful(Left(error))
        case Right(_)    => pedigreeMatchesRepository.confirmPedigree(matchId)
      }
      result.foreach {
        case Right(_) =>
          traceService.addTracePedigree(TracePedigree(matchResult.pedigree.idPedigree, userId, new Date(),
            PedigreeConfirmInfo2(matchResult._id.id, matchResult.pedigree.idPedigree, matchResult.profile.globalCode.text, matchResult.pedigree.assignee, matchResult.`type`)))
          pedigreeDataRepository.getPedigreeDescriptionById(matchResult.pedigree.idPedigree).map {
            case (pedName, courtCaseName) =>
              traceService.add(Trace(matchResult.profile.globalCode, userId, new Date(),
                PedigreeConfirmInfo(matchResult._id.id, matchResult.pedigree.idPedigree, matchResult.pedigree.assignee, matchResult.`type`, courtCaseName, pedName)))
          }
          notificationService.solve(matchResult.pedigree.assignee,
            PedigreeMatchingInfo(matchResult.profile.globalCode, Some(matchResult.pedigree.caseType), Some(matchResult.pedigree.idCourtCase.toString)))
        case Left(_) => ()
      }
      result
    }

  override def deleteMatches(idPedigree: Long): Future[Either[String, Long]] =
    pedigreeMatchesRepository.deleteMatches(idPedigree)

  override def masiveGroupDiscardByGroup(id: String, group: String, isSuperUser: Boolean, userId: String, replicate: Boolean = true): Future[Either[String, String]] =
    pedigreeMatchesRepository.getAllMatchNonDiscardedByGroup(id, group).flatMap { matches =>
      if matches.isEmpty then
        Future.successful(Left("Sin matches"))
      else
        Future.sequence(matches.map(mr => discard(mr._id.id, userId, isSuperUser))).flatMap { results =>
          if results.forall(_.isRight) then Future.successful(Right(id))
          else Future.successful(Left(results.collect { case Left(e) => e }.mkString(",")))
        }
    }

  override def exportMatchesByGroup(matchesToExport: Seq[PedigreeMatchResultData]): Unit = ()

  override def getMatchesExportFile(): File =
    new java.io.File(s"$exportProfilesPath${File.separator}MatchesMPIFile.csv")
