package pedigree

import java.util.Calendar
import java.util.Date
import javax.inject.{Inject, Singleton}
import configdata.CategoryService
import matching.{CollapseRequest, MatchingService}
import models.Tables.CourtCaseProfiles
import org.postgresql.util.PSQLException
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import trace.{PedigreeEditInfo, TraceService,TracePedigree,PedigreeStatusChangeInfo}

import scala.concurrent.{Await, Future}
import play.api.i18n.Messages
import profile.ProfileService
import profiledata._
import search.FullTextSearchService
import types.SampleCode
import services.{CacheService, Keys}
import trace.{PedigreeEditInfo, TraceService,TracePedigree,PedigreeStatusChangeInfo,PedigreeCopyInfo}

import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.{Failure, Success}

trait PedigreeService {
  def getAllCourtCases(pedigreeSearch: PedigreeSearch): Future[Seq[CourtCaseModelView]]
  def getTotalCourtCases(pedigreeSearch: PedigreeSearch): Future[Int]
  def createCourtCase(courtCase: CourtCaseAttempt): Future[Either[String, Long]]
  def createMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Long]]
  def addGenogram(genogram: PedigreeGenogram): Future[Either[String, Long]]
  def getCourtCase(courtCaseId: Long, userId: String, isSuperUser: Boolean): Future[Option[CourtCaseFull]]
  def getMetadata(personDataSearch:PersonDataSearch): Future[List[PersonData]]
  def getPedigree(pedigreeId: Long): Future[Option[PedigreeDataCreation]]
  def updateCourtCase(id: Long, courtCase: CourtCaseAttempt, isSuperUser: Boolean): Future[Either[String, Long]]
  def updateMetadata(idCourtCase: Long, assigne: String, personData: PersonData, isSuperUser: Boolean): Future[Either[String, Long]]
  def changePedigreeStatus(pedigreeId: Long, status: PedigreeStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, Long]]
  def getCaseTypes() : Future[Seq[CaseType]]
  def getProfiles(courtCasePedigreeSearch:CourtCasePedigreeSearch, isReference : Boolean):Future[List[CourtCasePedigree]]
  def getProfilesNodeAssociation(courtCasePedigreeSearch:CourtCasePedigreeSearch, isReference: Boolean, profilesCod: List[String]):Future[List[ProfileNodeAssociation]]
  def getProfilesInactive(courtCasePedigreeSearch:CourtCasePedigreeSearch):Future[List[CourtCasePedigree]]
  def getTotalProfiles(courtCasePedigreeSearch:CourtCasePedigreeSearch,isReference: Boolean):Future[Long]
  def getTotalProfilesInactive(courtCasePedigreeSearch:CourtCasePedigreeSearch):Future[Long]
  def getTotalProfilesOccurenceInCase(globalCode:SampleCode):Future[Int]
  def getTotalProfilesPedigreeMatches(globalCode:SampleCode):Future[Int]
  def getTotalProfilesNodeAssociation(courtCasePedigreeSearch:CourtCasePedigreeSearch,isReference: Boolean, profilesCod : List[String]):Future[Long]
  def getTotalMetadata(personDataSearch:PersonDataSearch):Future[Long]
  def addProfiles(courtCaseProfiles:List[CaseProfileAdd], isReference: Boolean):Future[Either[String, Unit]]
  def removeProfiles(courtCaseProfiles:List[CaseProfileAdd]):Future[Either[String, Unit]]
  def removeMetadata(idCourtCase: Long, personData: PersonData):Future[Either[String, Unit]]
  def filterProfileDatasWithFilter(input: String,idCase:Long)(filter: ProfileData => Boolean): Future[Seq[ProfileDataView]]
  def filterProfileDatasWithFilterPaging(input: String,idCase:Long,page:Int,pageSize:Int)(filter: ProfileData => Boolean): Future[Seq[ProfileDataView]]
  def addBatches(courtCaseProfiles:CaseBatchAdd):Future[Either[String, Unit]]
  def filterProfileDatasWithFilterNodeAssociation(input: String,idCase:Long)(filter: ProfileDataWithBatch => Boolean): Future[Seq[ProfileDataView]]
  def getCourtCasePedigrees(courtCasePedigreeSearch:CourtCasePedigreeSearch):Future[Seq[PedigreeMetaDataView]]
  def getTotalCourtCasePedigrees(courtCasePedigreeSearch:CourtCasePedigreeSearch):Future[Int]
  def createOrUpdatePedigreeMetadata(pedigreeMetaData: PedigreeMetaData) : Future[Either[String, Long]]
  def createPedigree(pedigreeDataCreation: PedigreeDataCreation,userId:String,copiedFrom:Option[Long] = None) : Future[Either[String, Long]]
  def fisicalDeletePredigree(pedigreeId: Long, userId: String, isSuperUser: Boolean): Future[Either[String, Long]]
  def changeCourtCaseStatus(courtCaseId: Long, status: PedigreeStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, Long]]
  def getProfilesNodo(id: Long,codigo: String):Future[Boolean]
  def doesntHaveGenotification(pedigreeId: Long) : Future[Boolean]
  def clonePedigree(pedigreeId : Long,userId:String) : Future[Either[String, Long]]
  def doesntHavePedigrees(courtCaseId: Long) : Future[Boolean]
  def doesntHaveActivePedigrees(courtCaseId: Long) : Future[Boolean]
  def hasPendingPedigreeMatches(courtCaseId: Long) : Future[Boolean]
  def hasPedigreeMatches(courtCaseId: Long) : Future[Boolean]
  def closeAllPedigrees(courtCaseId: Long,userId:String) : Future[Either[String, Long]]
  def countPendingCourCaseMatches(courtCaseId: Long) : Future[Int]
  def getProfilesToDelete(courtCaseId: Long) : Future[Seq[SampleCode]]
  def countPendingScenariosByProfile(globalCode: String): Future[Int]
  def countActivePedigreesByProfile(globalCode: String): Future[Int]
  def getTotalProfileNumberOfMatches(globalCode:SampleCode):Future[Int]
  def collapse(idCourtCase:Long,user:String):Unit
  def getProfilesForCollapsing(idCourtCase:Long):Future[List[(String,Boolean)]]
  def disassociateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]):Future[Either[String,Unit]]
  def collapseGroup(collapseRequest:CollapseRequest):Future[Either[String, Unit]]
  def areAssignedToPedigree(globalCodes:List[String],courtCaseId:Long):Future[Either[String, Unit]]
  def getPedigreeCoincidencia(id: Long): Future[PedigreeMatchCard]
  def profileNumberOfPendingMatches(globalCode: String): Future[Int]
  def countProfilesHitPedigrees(globalCodes:String):Future[Int]
  def countProfilesDiscardedPedigrees(globalCodes:String):Future[Int]
  def getMatchByProfile(globalCode: String): Future[ProfileDataViewCoincidencia]
  def getPedigreeByCourtCase(courtCaseId: Long): Future[List[PedigreeGenogram]]

}

@Singleton
class PedigreeServiceImpl @Inject() (
    pedigreeDataRepository: PedigreeDataRepository,
    pedigreeRepository: PedigreeRepository,
    cache: CacheService,
    profileService: ProfileService = null,
    fullTextSearch:FullTextSearchService = null,
    categoryService: CategoryService = null,
    pedigreeGenotificationRepository: PedigreeGenotypificationRepository = null,
    pedigreeMatchesRepository: PedigreeMatchesRepository = null,
    pedigreeScenarioRepository: PedigreeScenarioRepository = null,
    matchingService:MatchingService = null,
    pedCheckService: PedCheckService = null,
    traceService:TraceService = null) extends PedigreeService {

  val errorPf: PartialFunction[Throwable, Either[String, Long]] = {
    case psql: PSQLException => {
      psql.getSQLState match {
        case "23505" => Left(Messages("error.E0901"))
        case _ => Left(Messages("error.E0630"))
      }
    }
  }

  override def getAllCourtCases(pedigreeSearch: PedigreeSearch): Future[Seq[CourtCaseModelView]] = {
    getPedigreesFromProfile(pedigreeSearch) flatMap { case pedigreeIds =>
      pedigreeIds match {
        case Some(ids) =>
          if (ids.isEmpty) Future.successful(Seq.empty)
          else pedigreeDataRepository.getAllCourtCases(Some(ids), pedigreeSearch).flatMap(fillPendingMatches)
        case None => pedigreeDataRepository.getAllCourtCases(None, pedigreeSearch).flatMap(fillPendingMatches)
      }
    }
  }
  def fillPendingMatches(list:Seq[CourtCaseModelView]):Future[Seq[CourtCaseModelView]] = {
    Future.sequence(list.map(cc => this.countPendingCourCaseMatches(cc.id).map(result => {
      cc.copy(numberOfPendingMatches = result)
    })))
  }

  override def getTotalCourtCases(pedigreeSearch: PedigreeSearch): Future[Int] = {
    getPedigreesFromProfile(pedigreeSearch) flatMap { case pedigreeIds =>
      pedigreeIds match {
        case Some(ids) =>
          if (ids.isEmpty) Future.successful(0)
          else pedigreeDataRepository.getTotalCourtCases(Some(ids), pedigreeSearch)
        case None => pedigreeDataRepository.getTotalCourtCases(None, pedigreeSearch)
      }
    }
  }

  private def getPedigreesFromProfile(pedigreeSearch: PedigreeSearch): Future[Option[Seq[Long]]] = {
    if (pedigreeSearch.profile.isDefined) {
      pedigreeRepository.findByProfile(pedigreeSearch.profile.get).map { pedigreeIds => Some(pedigreeIds) }
    } else {
      Future.successful(None)
    }
  }

  override def createCourtCase(courtCase: CourtCaseAttempt): Future[Either[String, Long]] = {
    pedigreeDataRepository.createCourtCase(courtCase).recover(errorPf)
  }

  override def createMetadata(idCourtCase: Long, personData:  PersonData): Future[Either[String, Long]] = {
    pedigreeDataRepository.createMetadata(idCourtCase, personData).recover(errorPf)
  }


  override def addGenogram(genogram: PedigreeGenogram): Future[Either[String, Long]] = {
    val result = pedigreeRepository.addGenogram(genogram).recover(errorPf)
    result.onComplete(_=>pedCheckService.cleanConsistency(genogram._id))
    result
  }

  override def getPedigree(pedigreeId: Long): Future[Option[PedigreeDataCreation]] = {
    pedigreeId match {
      case 0 => Future.successful(None)
      case _ => {
        val pedigreeDataCreationPromise = pedigreeDataRepository.getPedigreeMetaData(pedigreeId);
        val genogramPromise = pedigreeRepository.get(pedigreeId);

        val result = for {
          dataCreation <- pedigreeDataCreationPromise
          genogram <-genogramPromise
        } yield (dataCreation, genogram)

        result.flatMap{ x =>
          x._1.fold[Future[Option[PedigreeDataCreation]]] (Future.successful(None))({pedd =>
            Future.successful(
              Some(PedigreeDataCreation(pedd.pedigreeMetaData, x._2))
            )
          })
        }
      }
    }

  }


  override def getPedigreeCoincidencia(id: Long): Future[PedigreeMatchCard] = {
 pedigreeMatchesRepository.getMatchByPedigree(id)
  .flatMap{ matches => matches match{
              case Some(m) =>{ m._id match {
                        case Left(idPedigree) => {
                          pedigreeDataRepository.getPedigreeMetaData(idPedigree).flatMap {
                            pedigreeOpt => {
                              val pedigree = pedigreeOpt.get
                              (for {
                                dis <- pedigreeMatchesRepository.numberOfDiscardedMatches(idPedigree)
                                pending <- pedigreeMatchesRepository.numberOfPendingMatches(idPedigree)
                                hit <- pedigreeMatchesRepository.numberOfHitMatches(idPedigree)
                                caseType <- pedigreeMatchesRepository.getTypeCourtCasePedigree(idPedigree)
                              } yield (dis, pending, hit, caseType))
                                .map {
                                  case (dis, pen, hit, caseType) => {
                                    PedigreeMatchCard(pedigree.pedigreeMetaData.name, idPedigree.toString, pedigree.pedigreeMetaData.assignee,
                                      m.lastMatchDate.date, (dis+pen+hit), "pedigree", pedigree.pedigreeMetaData.courtCaseId.toString,
                                      pedigree.pedigreeMetaData.courtCaseName, caseType , hit, pen, dis)
                                  }
                                }
                            }
                          }
                        }
              } }
    } }
  }


  override def getCourtCase(courtCaseId: Long, userId: String, isSuperUser: Boolean): Future[Option[CourtCaseFull]] = {
    pedigreeDataRepository.getCourtCase(courtCaseId).flatMap {
      _.fold[Future[Option[CourtCaseFull]]](Future.successful(None))({ cc =>
        if (cc.assignee == userId || isSuperUser) {
          Future.successful(
            Some(CourtCaseFull(
              cc.id,
              cc.internalSampleCode,
              cc.attorney,
              cc.court,
              cc.assignee,
              cc.crimeInvolved,
              cc.crimeType,
              cc.criminalCase,
              cc.status,
              cc.personData,
              cc.caseType)))
        } else Future.successful(None)
      })
    }
  }

  override def getPedigreeByCourtCase(courtCaseId: Long): Future[List[PedigreeGenogram]] = {
    pedigreeRepository
      .getPedigreeByCourtCaseId(courtCaseId)
  }


override def getMetadata( personDataSearch:PersonDataSearch): Future[List[PersonData]] = {

  pedigreeDataRepository.getMetadata(personDataSearch)

}

  override def updateCourtCase(id: Long, courtCase: CourtCaseAttempt, isSuperUser: Boolean): Future[Either[String, Long]] = {
    getCourtCase(id, courtCase.assignee, isSuperUser) flatMap { cc =>
      if (cc.isDefined) {
        pedigreeDataRepository.runInTransactionAsync { implicit session =>
          pedigreeDataRepository.updateCourtCase(id, courtCase)
          Right(id)
        }.recover(errorPf)
      } else Future.successful(Left(Messages("error.E0643", courtCase.assignee)))
    }

  }

  override def updateMetadata(idCourtCase: Long, assignee: String, personData: PersonData, isSuperUser: Boolean): Future[Either[String, Long]] = {
    getCourtCase(idCourtCase, assignee, isSuperUser) flatMap { cc =>
      if (cc.isDefined) {
        pedigreeDataRepository.runInTransactionAsync { implicit session =>
          pedigreeDataRepository.updateMetadata(idCourtCase, personData)
          Right(idCourtCase)
        }.recover(errorPf)
      } else Future.successful(Left(Messages("error.E0643", assignee)))
    }

  }

  override def changePedigreeStatus(pedigreeId: Long, status: PedigreeStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, Long]] = {
    pedigreeDataRepository.getPedigreeMetaData(pedigreeId) flatMap { pedigreeOpt =>
      val pedigree = pedigreeOpt.get
      if (pedigree.pedigreeMetaData.assignee == userId || isSuperUser) {
        if (validTransition(pedigree.pedigreeMetaData.status, status)) {
          // update mongo
          pedigreeRepository.changeStatus(pedigreeId, status) flatMap {
            // update postgresql
            case Right(id) => pedigreeDataRepository.changePedigreeStatus(pedigreeId, status) flatMap {
              case Right(cc) => {
                traceService.addTracePedigree(TracePedigree(pedigreeId, userId, new Date(),
                  PedigreeStatusChangeInfo(status.toString))).map(_=> {
                  Right(cc)
                })
              }
              case Left(error) => {
                // revert mongo
                pedigreeRepository.changeStatus(pedigreeId, pedigree.pedigreeMetaData.status) map { _ => Left(error) }
              }
            }

            case Left(error) => Future.successful(Left(error))
          }
        } else {
          if (pedigree.pedigreeMetaData.status != PedigreeStatus.Validated) {
            Future.successful(Left(Messages("error.E0930",pedigree.pedigreeMetaData.status ,status )))
          } else {
            Future.successful(Right(pedigreeId))
          }
        }
      } else {
        Future.successful(Left(Messages("error.E0644",  userId )))
      }
    }
  }

/*  override def changeStatus(pedigreeId: Long, status: PedigreeStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, Long]] = {
    pedigreeDataRepository.getCourtCase(pedigreeId) flatMap { courtCaseOpt =>
      val courtCase = courtCaseOpt.get
      if (courtCase.assignee == userId || isSuperUser) {
        if (validTransition(courtCase.status, status)) {
          // update mongo
          pedigreeRepository.changeStatus(pedigreeId, status) flatMap {
            // update postgresql
            case Right(id) => pedigreeDataRepository.changeStatus(pedigreeId, status) flatMap {
              case Right(cc) => {
                Future.successful(Right(cc))
              }
              case Left(error) => {
                // revert mongo
                pedigreeRepository.changeStatus(pedigreeId, courtCase.status) map { _ => Left(error) }
              }
            }

            case Left(error) => Future.successful(Left(error))
          }
        } else {
          if (courtCase.status != PedigreeStatus.Validated) {
            Future.successful(Left(Messages("error.E0930",courtCase.status ,status )))
          } else {
            Future.successful(Right(pedigreeId))
          }
        }
      } else {
        Future.successful(Left(Messages("error.E0644",  userId )))
      }
    }
  }*/

  private def validTransition(originalStatus: PedigreeStatus.Value, newStatus: PedigreeStatus.Value) = {
    (originalStatus, newStatus) match {
      case (PedigreeStatus.UnderConstruction, PedigreeStatus.Active) => true
      case (PedigreeStatus.UnderConstruction, PedigreeStatus.Deleted) => true
      case (PedigreeStatus.UnderConstruction, PedigreeStatus.Closed) => true
      case (PedigreeStatus.Active, _) => true
      case (PedigreeStatus.Validated, PedigreeStatus.Validated) => true
      case (_, _) => false
    }
  }

  override def getCaseTypes(): Future[Seq[CaseType]] = {
    cache.asyncGetOrElse(Keys.caseTypes)(pedigreeDataRepository.getCaseTypes())
  }
  def getGenotipification(list:List[CourtCasePedigree]):Future[List[CourtCasePedigree]] = {
    profileService.findByCodes(list.map(x => SampleCode(x.globalCode))).map(profiles =>{
      list.map(element => {
        val profile = profiles.find(p => p.globalCode.text == element.globalCode)
        profile.map(x => element.copy(genotypification = x.genotypification))
      }).flatten
    })
//    Future.sequence(list.map(element => {
//        profileService.findByCode(SampleCode(element.globalCode)).map{
//          case None => element
//          case Some(profile) => element.copy(genotypification = profile.genotypification)
//        }
//    }))
  }
  override def getProfiles(courtCasePedigreeSearch:CourtCasePedigreeSearch, isReference : Boolean):Future[List[CourtCasePedigree]] = {

    val typeProfile = if(isReference) "Referencia" else "Resto"

    val fut = courtCasePedigreeSearch.input match {
      case None => pedigreeDataRepository.getProfiles(courtCasePedigreeSearch, Nil, Some(typeProfile)).flatMap(getGenotipification)
        case Some(input) =>   {
        val categories = categoryService.listCategories
        val profiledatas = filterProfileDatasWithFilter(input,courtCasePedigreeSearch.idCourtCase) { profile =>
          categories(profile.category).pedigreeAssociation && (categories(profile.category).isReference == isReference)
        }

        profiledatas.flatMap( list => {
          if(list.isEmpty){
            Future.successful(Nil)
          }else{
            pedigreeDataRepository.getProfiles(courtCasePedigreeSearch,list.toList.map(_.globalCode.text),Some(typeProfile)).flatMap(getGenotipification)
          }
        })
      }
    }
    fut.map(result =>{
      result.groupBy(_.internalCode).map(x => {
        x._2.sortBy(_.idBatch).reverse.head
      }).toList
    })
  }

  override def getProfilesNodo(idCourtCase: Long, codigoGlobal: String):Future[Boolean] = {
    val categories = categoryService.listCategories
        val profile = filterProfileDatasWithFilter(codigoGlobal,idCourtCase) { p => categories(p.category).pedigreeAssociation }
        profile.map(b=>  categories(b.head.category).isReference  )
  }

  override def getProfilesNodeAssociation(courtCasePedigreeSearch:CourtCasePedigreeSearch, isReference: Boolean, profilesCod: List[String]):Future[List[ProfileNodeAssociation]] = {
    val typeProfile = if(isReference) "Referencia" else "Resto"
    courtCasePedigreeSearch.input match {
      case None => {
        if (!isReference){ pedigreeDataRepository.getProfilesNodeAssociation(courtCasePedigreeSearch,Nil,Some(typeProfile) , profilesCod) }
        else {  pedigreeDataRepository.getProfilesNodeAssociation(courtCasePedigreeSearch,Nil,None, profilesCod)  }
      }
      case Some(input) =>   {
        val categories = categoryService.listCategories

        (for{profiledatas <- filterProfileDatasWithFilter(input,courtCasePedigreeSearch.idCourtCase){ profile =>
          if (courtCasePedigreeSearch.idCourtCase!=0 && !isReference) categories(profile.category).pedigreeAssociation &&   categories(profile.category).isReference == isReference
          else { if(isReference){categories(profile.category).pedigreeAssociation }
            else{ categories(profile.category).pedigreeAssociation && profile.category.text != "IR"  && categories(profile.category).tipo.getOrElse(1) == 2 }
          }
          }
          profiledatalabel <- filterProfileDatasWithFilterNodeAssociation(input,courtCasePedigreeSearch.idCourtCase) { profile =>
            if (courtCasePedigreeSearch.idCourtCase != 0 && !isReference) categories(profile.category).pedigreeAssociation && categories(profile.category).isReference == isReference
            else { if(isReference){categories(profile.category).pedigreeAssociation }
            else{ categories(profile.category).pedigreeAssociation && profile.category.text != "IR" && categories(profile.category).tipo.getOrElse(1) == 2 }
            }          }
          } yield (profiledatas , profiledatalabel)
          ).flatMap{
          case (profiledatas, profiledataLabel) => {
            val profileSearch = (profiledatas.toSet.++(profiledataLabel.toSet)).filterNot(x => profilesCod.contains(x.globalCode.text))



              if(profileSearch.isEmpty){
                Future.successful(Nil)
              }else{
                if(!isReference){
                  pedigreeDataRepository.getProfilesNodeAssociation(courtCasePedigreeSearch,profileSearch.toList.map(_.globalCode.text),Some(typeProfile), Nil) }
                else{
                  pedigreeDataRepository.getProfilesNodeAssociation(courtCasePedigreeSearch,profileSearch.toList.map(_.globalCode.text),None,Nil) }
              }

          }
        }
      }
    }
  }

  override def getTotalProfiles(courtCasePedigreeSearch:CourtCasePedigreeSearch, isReference :Boolean):Future[Long] = {
    val typeProfile = if(isReference) "Referencia" else "Resto"

    courtCasePedigreeSearch.input match {
      case None => {pedigreeDataRepository.getTotalProfiles(courtCasePedigreeSearch,Nil,Some(typeProfile)).map(count => count.toLong)
      }
      case Some(input)  => {
        val categories= categoryService.listCategories
        val profiledatas = filterProfileDatasWithFilter(input,courtCasePedigreeSearch.idCourtCase){ profile =>
          categories(profile.category).pedigreeAssociation &&   categories(profile.category).isReference == isReference
        }
        profiledatas.flatMap( list => {
            if(list.isEmpty){
              Future.successful(0)
            }else {
               pedigreeDataRepository.getTotalProfiles(courtCasePedigreeSearch, list.toList.map(_.globalCode.text),Some(typeProfile)).map(count => count.toLong)
            }
          })
      }
    }
  }
  override def getTotalProfilesPedigreeMatches(globalCode:SampleCode):Future[Int] = {
    pedigreeRepository.findByProfile(globalCode.text).flatMap(pedigreesIds => {
      Future.sequence(pedigreesIds.map(id => {
        pedigreeMatchesRepository.numberOfPendingMatches(id)
      })).map(result => {
        result.sum
      })
    })
  }
  override def getTotalProfileNumberOfMatches(globalCode:SampleCode):Future[Int] = {
    pedigreeMatchesRepository.profileNumberOfPendingMatches(globalCode.text)
  }
  override def getTotalProfilesOccurenceInCase(globalCode:SampleCode):Future[Int] = {
    pedigreeDataRepository.getTotalProfilesOccurenceInCase(globalCode.text)
  }

  override def getTotalProfilesNodeAssociation(courtCasePedigreeSearch:CourtCasePedigreeSearch, isReference :Boolean, profilesCod : List[String]):Future[Long] = {
    val typeProfile = if (isReference) "Referencia" else "Resto"

    courtCasePedigreeSearch.input match {
      case None => {
        if (isReference) {
          pedigreeDataRepository.getTotalProfilesNodeAssociation(courtCasePedigreeSearch, Nil, None , profilesCod).map(count => count.toLong)
        }
        else {
          pedigreeDataRepository.getTotalProfilesNodeAssociation(courtCasePedigreeSearch, Nil, Some(typeProfile), profilesCod).map(count => count.toLong)
        }
      }
      case Some(input) => {
        val categories = categoryService.listCategories

        (for {profiledatas <- filterProfileDatasWithFilter(input, courtCasePedigreeSearch.idCourtCase) { profile =>
          if (courtCasePedigreeSearch.idCourtCase != 0 && !isReference) categories(profile.category).pedigreeAssociation && categories(profile.category).isReference == isReference
          else { if(isReference){categories(profile.category).pedigreeAssociation }
          else{ categories(profile.category).pedigreeAssociation && profile.category.text != "IR" }
          }        }
              profiledatalabel <- filterProfileDatasWithFilterNodeAssociation(input, courtCasePedigreeSearch.idCourtCase) { profile =>
                if (courtCasePedigreeSearch.idCourtCase != 0 && !isReference) categories(profile.category).pedigreeAssociation && categories(profile.category).isReference == isReference
                else { if(isReference){categories(profile.category).pedigreeAssociation }
                else{ categories(profile.category).pedigreeAssociation && profile.category.text != "IR" }
                }              }
        } yield (profiledatas, profiledatalabel)
          ).flatMap {
          case (profiledatas, profiledataLabel) => {
            val profileSearch = (profiledatas.toSet.++(profiledataLabel.toSet)).filterNot(x => profilesCod.contains(x.globalCode.text))

            if (profileSearch.isEmpty) {
              Future.successful(0L)
            } else {
              if (isReference) {
                pedigreeDataRepository.getTotalProfilesNodeAssociation(courtCasePedigreeSearch, profileSearch.toList.map(_.globalCode.text), None, Nil).map(count => count.toLong)
              }
              else {
                pedigreeDataRepository.getTotalProfilesNodeAssociation(courtCasePedigreeSearch, profileSearch.toList.map(_.globalCode.text), Some(typeProfile), Nil).map(count => count.toLong)
              }
            }
          }
        }
      }
    }
  }

  override def getTotalMetadata(personDataSearch:PersonDataSearch):Future[Long] = {

    pedigreeDataRepository.getTotalMetadata(personDataSearch).map(count => count.toLong)
  }




  override def addProfiles(courtCaseProfiles:List[CaseProfileAdd], isReference: Boolean):Future[Either[String, Unit]] = {
   val x = courtCaseProfiles.map( cpp => isReference match {
     case true =>   CaseProfileAdd(cpp.courtcaseId,cpp.globalCode,Some(ProfileType.Referencia.toString))
     case false => CaseProfileAdd(cpp.courtcaseId,cpp.globalCode,Some(ProfileType.Resto.toString)
    ) } )
    courtCaseProfiles.head.courtcaseId
    val restos = x.filter(p => p.profileType.isDefined && p.profileType.get == ProfileType.Resto.toString)
    if(!restos.isEmpty){
      doesntHaveActivePedigrees(courtCaseProfiles.head.courtcaseId).flatMap {
        case false => {
          Future.successful(Left(Messages("error.E0211")))
        }
        case true => {
          pedigreeDataRepository.addProfiles(x)
        }
      }
    }else{
      pedigreeDataRepository.addProfiles(x)
    }

  }
  override def removeProfiles(courtCaseProfiles:List[CaseProfileAdd]):Future[Either[String, Unit]] = {
    pedigreeDataRepository.removeProfiles(courtCaseProfiles)
  }

  override def  removeMetadata(idCourtCase: Long, personData:PersonData):Future[Either[String, Unit]] = {
    pedigreeDataRepository.removeMetadata(idCourtCase ,personData)
  }

  override def filterProfileDatasWithFilter(input: String,idCase:Long)(filter: ProfileData => Boolean): Future[Seq[ProfileDataView]] = {
    fullTextSearch.searchProfileDatasWithFilter(input)(filter)
      .flatMap(list => {
       pedigreeDataRepository.getProfiles(CourtCasePedigreeSearch(0,Int.MaxValue,idCase,None),list.toList.map(_.globalCode.text),None)
         .map(associatedProfiles => {
           list.map(pd => {
             ProfileDataView(pd.globalCode,pd.category,pd.internalSampleCode,pd.assignee,!associatedProfiles.filter(_.globalCode==pd.globalCode.text).isEmpty)
           }).sortBy(r => (r.associated.toString, r.globalCode.text))
         })
      })
  }
  override def filterProfileDatasWithFilterPaging(input: String,idCase:Long,page:Int,pageSize:Int)(filter: ProfileData => Boolean): Future[Seq[ProfileDataView]] = {
    fullTextSearch.searchProfileDatasWithFilterPaging(input,page,pageSize)(filter)
      .flatMap(list => {
        pedigreeDataRepository.getProfiles(CourtCasePedigreeSearch(0,Int.MaxValue,idCase,None),list.toList.map(_.globalCode.text),None)
          .map(associatedProfiles => {
            list.map(pd => {
              ProfileDataView(pd.globalCode,pd.category,pd.internalSampleCode,pd.assignee,!associatedProfiles.filter(_.globalCode==pd.globalCode.text).isEmpty)
            }).sortBy(r => (r.associated.toString, r.globalCode.text))
          })
      })
  }

  override def filterProfileDatasWithFilterNodeAssociation(input: String,idCase:Long)(filter: ProfileDataWithBatch => Boolean): Future[Seq[ProfileDataView]] = {
    fullTextSearch.searchProfileDatasWithFilterNodeAssociation(input)(filter)
      .flatMap(list => {
        pedigreeDataRepository.getProfiles(CourtCasePedigreeSearch(0,Int.MaxValue,idCase,None),list.toList.map(_.globalCode.text),None)
          .map(associatedProfiles => {
            list.map(pd => {
              ProfileDataView(pd.globalCode,pd.category,pd.internalSampleCode,pd.assignee,!associatedProfiles.filter(_.globalCode==pd.globalCode.text).isEmpty)
            }).sortBy(r => (r.associated.toString, r.globalCode.text))
          })
      })
  }




  override def addBatches(courtCaseProfiles:CaseBatchAdd):Future[Either[String, Unit]] = {
    pedigreeDataRepository
      .getProfilesFromBatches(
        courtCaseProfiles.courtcaseId,
        courtCaseProfiles.batches,
        courtCaseProfiles.tipo
      ).flatMap {
      case Nil => {
        Future.successful(Left(Messages("error.E0203")))
      }
      case (globalCodes) => {
        this.pedigreeDataRepository.getCourtCase(courtCaseProfiles.courtcaseId).flatMap {
          case None => Future.successful(Left(Messages("error.E0203")))
          case Some(courtCase) => {
            val profiles = if (courtCase.caseType == "DVI") {
              val resto = globalCodes.map {
                case (globCode, true) => CaseProfileAdd(courtCaseProfiles.courtcaseId, globCode, Some(ProfileType.Referencia.toString))
                case (globCode, false) => CaseProfileAdd(courtCaseProfiles.courtcaseId, globCode, Some(ProfileType.Resto.toString))
              }
              var perfiles = resto
              val restos = resto.filter(p => p.profileType.isDefined && p.profileType.get == ProfileType.Resto.toString)

              if (!restos.isEmpty) {
               val bool = Await.result( doesntHaveActivePedigrees(courtCaseProfiles.courtcaseId),Duration(10,SECONDS)  )
                if(!bool)
                    perfiles = resto.filter(p => p.profileType.isDefined && p.profileType.get == ProfileType.Referencia.toString)
              }
              perfiles

            } else {
              globalCodes.filter(_._2).map {
                case (globCode, _) => CaseProfileAdd(courtCaseProfiles.courtcaseId, globCode, Some(ProfileType.Referencia.toString))
              }

            }
            pedigreeDataRepository.addProfiles(profiles)
          }
        }
      }
      }
    }


  override def getCourtCasePedigrees(courtCasePedigreeSearch:CourtCasePedigreeSearch):Future[Seq[PedigreeMetaDataView]] = {
    pedigreeDataRepository.getPedigrees(courtCasePedigreeSearch)
  }

  override def getTotalCourtCasePedigrees(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Int] = {
    pedigreeDataRepository.getTotalPedigrees(courtCasePedigreeSearch)
  }

  override def createOrUpdatePedigreeMetadata(pedigreeMetaData: PedigreeMetaData): Future[Either[String, Long]] = {
    pedigreeDataRepository.createOrUpdatePedigreeMetadata(pedigreeMetaData)
  }

  override def createPedigree(pedigreeDataCreation: PedigreeDataCreation,userId:String,copiedFrom:Option[Long] = None): Future[Either[String, Long]] = {
    val addPromise = pedigreeDataRepository.createOrUpdatePedigreeMetadata(pedigreeDataCreation.pedigreeMetaData)
    addPromise.flatMap {
      pedigreeId => {
        pedigreeId.fold[Future[Either[String, Long]]](
          error => {
            Future.successful(Left(error))
          },
          id => {
            if(pedigreeDataCreation.pedigreeMetaData.id == 0){
              traceService.addTracePedigree(TracePedigree(id, userId, new Date(),
                PedigreeStatusChangeInfo(PedigreeStatus.UnderConstruction.toString)))
              if(copiedFrom.isDefined){
                  traceService.addTracePedigree(TracePedigree(copiedFrom.get, userId, new Date(),
                    PedigreeCopyInfo(copiedFrom.get,pedigreeDataCreation.pedigreeMetaData.name)))
              }
            }else{
              traceService.addTracePedigree(TracePedigree(id, userId, new Date(),
                PedigreeEditInfo(id)))
            }
            val genogram = pedigreeDataCreation.pedigreeGenogram.get
            val pedigreeGenogram = PedigreeGenogram(id, genogram.assignee, genogram.genogram, genogram.status, genogram.frequencyTable, genogram.processed, genogram.boundary, genogram.executeScreeningMitochondrial,genogram.numberOfMismatches, genogram.caseType,genogram.mutationModelId,genogram.idCourtCase)
            this.addGenogram(pedigreeGenogram)
          })
      }
    }

  }

  override def fisicalDeletePredigree(pedigreeId: Long, userId: String, isSuperUser: Boolean): Future[Either[String, Long]] = {
    pedigreeDataRepository.getPedigreeMetaData(pedigreeId) flatMap { pedigreeOpt =>
      val pedigree = pedigreeOpt.get
      if (pedigree.pedigreeMetaData.assignee == userId || isSuperUser) {
          // update postgresql
          pedigreeDataRepository.deleteFisicalPedigree(pedigreeId) flatMap {
            // update mongo
            case Right(id) =>  pedigreeRepository.deleteFisicalPedigree(pedigreeId) flatMap {
              case Right(cc) => {
                Future.successful(Right(cc))
              }
              case Left(error) => {
                // revert postgresql Lo crearia de nuevo en postgree??
                Future.successful(Left(error))
              }
            }

            case Left(error) => Future.successful(Left(error))
          }

      } else {
        Future.successful(Left(Messages("error.E0644", userId)))
      }
    }
  }

  private def validToDelete(pedigreeId : Long) : Boolean = true

  override def changeCourtCaseStatus(courtCaseId: Long, status: PedigreeStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, Long]] = {
    pedigreeDataRepository.getCourtCase(courtCaseId) flatMap { courtCaseOpt =>
      val courtCase = courtCaseOpt.get
      if (courtCase.assignee == userId || isSuperUser) {
        if (validCourtCaseTransition(courtCase.status, status)) {
          pedigreeDataRepository.changeCourCaseStatus(courtCaseId, status) flatMap {
            case Right(cc) => {
              Future.successful(Right(cc))
            }
            case Left(error) => {
              Future.successful(Left(error))
            }
          }
        } else {
            Future.successful(Left(Messages("error.E0930",courtCase.status ,status )))
        }
      } else {
        Future.successful(Left(Messages("error.E0644",  userId )))
      }
    }
  }

  private def validCourtCaseTransition(originalStatus: PedigreeStatus.Value, newStatus: PedigreeStatus.Value) : Boolean = {
    (originalStatus, newStatus) match {
      case (PedigreeStatus.Open, PedigreeStatus.Deleted) => true
      case (PedigreeStatus.Open, PedigreeStatus.Closed) => true
      case (_, _) => false
    }
  }

  override def doesntHaveGenotification(pedigreeId: Long) : Future[Boolean] = {
    pedigreeGenotificationRepository.doesntHaveGenotification(pedigreeId);
  }

  override def clonePedigree(pedigreeId: Long,userId:String) : Future[Either[String, Long]] = {
    val now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime)
    getPedigree(pedigreeId) flatMap { optPed =>
      val pedigreeAClonar = optPed.get

      val newPedigreeMetadata = PedigreeMetaData(0, pedigreeAClonar.pedigreeMetaData.courtCaseId, "Copia" + pedigreeAClonar.pedigreeMetaData.name, now,
        PedigreeStatus.UnderConstruction, pedigreeAClonar.pedigreeMetaData.assignee)
      val newPedigreeGenogram = PedigreeGenogram(0, pedigreeAClonar.pedigreeGenogram.get.assignee, pedigreeAClonar.pedigreeGenogram.get.genogram, PedigreeStatus.UnderConstruction,
        pedigreeAClonar.pedigreeGenogram.get.frequencyTable, pedigreeAClonar.pedigreeGenogram.get.processed, pedigreeAClonar.pedigreeGenogram.get.boundary,
        pedigreeAClonar.pedigreeGenogram.get.executeScreeningMitochondrial, pedigreeAClonar.pedigreeGenogram.get.numberOfMismatches, pedigreeAClonar.pedigreeGenogram.get.caseType ,pedigreeAClonar.pedigreeGenogram.get.mutationModelId, pedigreeAClonar.pedigreeGenogram.get.idCourtCase)
      val newPedigreeDataCreation = PedigreeDataCreation(newPedigreeMetadata, Some(newPedigreeGenogram))

      createPedigree(newPedigreeDataCreation,userId,Some(pedigreeId))
    }
  }

  override def doesntHavePedigrees(courtCaseId: Long): Future[Boolean] = {
    val searchCard = CourtCasePedigreeSearch(idCourtCase=courtCaseId)
    val response = pedigreeDataRepository.getPedigrees(searchCard) map { list => list.size == 0 }
    response
  }
  override def doesntHaveActivePedigrees(courtCaseId: Long): Future[Boolean] = {
    val searchCard = CourtCasePedigreeSearch(idCourtCase=courtCaseId,status = Some(PedigreeStatus.Active))
    val response = pedigreeDataRepository.getPedigrees(searchCard) map { list => list.size == 0 }
    response
  }
  override def hasPendingPedigreeMatches(courtCaseId: Long) : Future[Boolean] = {
    pedigreeDataRepository.getPedigrees(CourtCasePedigreeSearch(idCourtCase = courtCaseId))
      .map(list =>list.map( ped => this.pedigreeMatchesRepository.hasPendingMatches(ped.id)))
      .map(f => Future.sequence(f))
      .flatMap( x => x)
      .map(x => x.filter( x => x))
      .map(list => !list.isEmpty)
  }
  override def countPendingCourCaseMatches(courtCaseId: Long) : Future[Int] = {
    pedigreeDataRepository.getPedigrees(CourtCasePedigreeSearch(idCourtCase = courtCaseId))
      .map(list =>list.map( ped => this.pedigreeMatchesRepository.numberOfPendingMatches(ped.id)))
      .map(f => Future.sequence(f))
      .flatMap( x => x)
      .map(x => x.sum)
  }
  override def hasPedigreeMatches(courtCaseId: Long) : Future[Boolean] = {
    pedigreeDataRepository.getPedigrees(CourtCasePedigreeSearch(idCourtCase = courtCaseId))
      .map(list =>list.map( ped => this.pedigreeMatchesRepository.hasMatches(ped.id)))
      .map(f => Future.sequence(f))
      .flatMap( x => x)
      .map(x => x.filter( x => x))
      .map(list => !list.isEmpty)
  }
  override def closeAllPedigrees(courtCaseId: Long,userId:String) : Future[Either[String, Long]] = {
    val searchCard = CourtCasePedigreeSearch(idCourtCase = courtCaseId)
    pedigreeDataRepository.getPedigrees(searchCard) map { pedigrees =>
        pedigrees.filter { ped =>
          ped.status == PedigreeStatus.UnderConstruction || ped.status == PedigreeStatus.Active
        } map { pedigree =>
          changePedigreeStatus(pedigree.id, PedigreeStatus.Closed, userId, true)
        }
    }
    Future.successful(Right(courtCaseId))
  }
  override def getProfilesToDelete(courtCaseId: Long) : Future[Seq[SampleCode]] = {
    pedigreeDataRepository.getProfilesToDelete(courtCaseId)
  }
  override def countPendingScenariosByProfile(globalCode: String): Future[Int] = {
    pedigreeScenarioRepository.countByProfile(globalCode)
  }
  override def countActivePedigreesByProfile(globalCode: String): Future[Int] = {
    pedigreeRepository.countByProfile(globalCode)
  }
  override def collapse(idCourtCase:Long,user:String):Unit = {
    matchingService.collapse(idCourtCase,user)
  }
  override def getProfilesForCollapsing(idCourtCase:Long):Future[List[(String,Boolean)]] = {
    pedigreeDataRepository.getProfilesForCollapsing(idCourtCase)
  }

  override def disassociateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] = {
    doesntHaveActivePedigrees(courtCaseProfiles.head.courtcaseId).flatMap{
      case false => {
        Future.successful(Left(Messages("error.E0211")))
      }
      case true => {
        pedigreeDataRepository.disassociateGroupedProfiles(courtCaseProfiles).flatMap(result => {
          getTotalProfilesInactive(CourtCasePedigreeSearch
          (0,1,courtCaseProfiles.head.courtcaseId,None,None,None,None,None,courtCaseProfiles.head.groupedBy)).map( count => {
            if(count == 0 && courtCaseProfiles.head.groupedBy.isDefined ){
              pedigreeDataRepository.disassociateGroupedProfiles(
                List(CaseProfileAdd(courtCaseProfiles.head.courtcaseId,courtCaseProfiles.head.groupedBy.get,None,None)))
            }else{
              Future.successful(result)
            }
          }).flatMap(x => {
            Future.successful(result)
          })
        } )
      }
    }

  }

  override def getProfilesInactive(courtCasePedigreeSearch:CourtCasePedigreeSearch):Future[List[CourtCasePedigree]] = {
      val typeProfile =  "Resto"

      val fut =courtCasePedigreeSearch.input match {
        case None => pedigreeDataRepository.getProfilesInactive(courtCasePedigreeSearch, Nil).flatMap(getGenotipification)
        case Some(input) =>   {
          val categories = categoryService.listCategories
          val profiledatas = filterProfileDatasWithFilter(input,courtCasePedigreeSearch.idCourtCase) { profile =>
            categories(profile.category).pedigreeAssociation && (!categories(profile.category).isReference)
          }

          profiledatas.flatMap( list => {
            if(list.isEmpty){
              Future.successful(Nil)
            }else{
              pedigreeDataRepository.getProfilesInactive(courtCasePedigreeSearch,list.toList.map(_.globalCode.text)).flatMap(getGenotipification)
            }
          })
        }
      }
    fut.map(list => {
      if(courtCasePedigreeSearch.groupedBy.isDefined){
        list.filter(_.globalCode != courtCasePedigreeSearch.groupedBy.get)
      }else{
        list
      }
    })
    }


override def getTotalProfilesInactive(courtCasePedigreeSearch:CourtCasePedigreeSearch):Future[Long] = {
 val typeProfile = "Resto"
  courtCasePedigreeSearch.input match {
    case None => {pedigreeDataRepository.getTotalProfilesInactive(courtCasePedigreeSearch,Nil).map(count => count.toLong)
    }
    case Some(input)  => {
      val categories= categoryService.listCategories
      val profiledatas = filterProfileDatasWithFilter(input,courtCasePedigreeSearch.idCourtCase){ profile =>
        categories(profile.category).pedigreeAssociation &&   categories(profile.category).isReference == typeProfile
      }
      profiledatas.flatMap( list => {
        if(list.isEmpty){
          Future.successful(0)
        }else {
          pedigreeDataRepository.getTotalProfilesInactive(courtCasePedigreeSearch, list.toList.map(_.globalCode.text)).map(count => count.toLong)
        }
      })
    }
  }
      }
  def areAssignedToPedigree(globalCodes:List[String],courtCaseId:Long):Future[Either[String, Unit]] = {
    pedigreeDataRepository.getPedigrees(CourtCasePedigreeSearch(0,Int.MaxValue,courtCaseId)).map(result=> {
      result.map(_.id)
    }).flatMap(idsPedigrees => {
      Future.sequence(globalCodes.map(globalCode => {
        this.pedigreeRepository.countByProfileIdPedigrees(globalCode,idsPedigrees.map(_.toString))
          .map(count => count>0)
          .map(hasPedigree => {
          if(hasPedigree){
            Left(globalCode)
          }else{
            Right(())
          }
        })
      })).map(result => {
        if(result.forall(_.isRight)){
          Right(())
        }else{
          if(result.filter(_.isLeft).size>1){
            Left(Messages("error.E0207" , result.filter(_.isLeft).map(_.left.get).mkString(",")))
          }else{
            Left(Messages("error.E0208" , result.filter(_.isLeft).map(_.left.get).mkString(",")))
          }
        }
      }).flatMap{
        case Right(()) => {
          Future.sequence(globalCodes.map(globalCode =>{
            this.pedigreeMatchesRepository.profileNumberOfPendingMatchesInPedigrees(globalCode,idsPedigrees)
              .map(count => count>0)
              .map(hasPendingMatch => {
                if(hasPendingMatch){
                  Left(globalCode)
                }else{
                  Right(())
                }
              })
            })
          ).map(result => {
            if(result.forall(_.isRight)){
              Right(())
            }else{
              if(result.filter(_.isLeft).size>1){
                Left(Messages("error.E0209" , result.filter(_.isLeft).map(_.left.get).mkString(",")))
              }else{
                Left(Messages("error.E0210" , result.filter(_.isLeft).map(_.left.get).mkString(",")))
              }
            }
            })
        }
        case Left(msg) => {
          Future.successful(Left(msg))
        }
      }
    })
  }

  override def collapseGroup(collapseRequest:CollapseRequest):Future[Either[String, Unit]] = {
    val courtCaseProfiles = collapseRequest.globalCodeChildren
      .map(child => CaseProfileAdd(collapseRequest.courtCaseId,child,None,Some(collapseRequest.globalCodeParent)))
    val parent = CaseProfileAdd(collapseRequest.courtCaseId,collapseRequest.globalCodeParent,None,Some(collapseRequest.globalCodeParent))

    val all = courtCaseProfiles :+ parent
    this.areAssignedToPedigree(courtCaseProfiles.map(_.globalCode),collapseRequest.courtCaseId).flatMap{
      case Left(m) => {
        Future.successful(Left(m))
      }
      case Right(()) => {
        this.pedigreeDataRepository.associateGroupedProfiles(all).flatMap(result => {
          Future.sequence(courtCaseProfiles.map(profile => {
            this.matchingService.discardCollapsingByLeftAndRightProfile(profile.globalCode,profile.courtcaseId)
          }) :+ this.matchingService.discardCollapsingByRightProfile(collapseRequest.globalCodeParent,collapseRequest.courtCaseId)).map( _ => result)
        })
      }
    }

  }

  override def profileNumberOfPendingMatches(globalCode: String): Future[Int] = {
    pedigreeMatchesRepository.profileNumberOfPendingMatches(globalCode)
  }
  override def countProfilesHitPedigrees(globalCodes:String):Future[Int] = {
   pedigreeMatchesRepository.countProfilesHitPedigrees(globalCodes)
 }
  override def countProfilesDiscardedPedigrees(globalCodes:String):Future[Int] = {
    pedigreeMatchesRepository.countProfilesDiscardedPedigrees(globalCodes)
  }

 override def getMatchByProfile(globalCode: String): Future[ProfileDataViewCoincidencia] = {
    pedigreeMatchesRepository.getMatchByProfile(globalCode)
      .flatMap { matches =>
        matches match {
          case Some(m) => {
            m._id match {
              case Right(globalCodes) => {
                profileService.get(SampleCode(globalCodes)).flatMap { ps =>
                  ps match {
                    case Some(pd) => {
                      var categoria = categoryService.getCategory(pd.categoryId)

                      (for {
                        dis <- countProfilesDiscardedPedigrees(globalCodes)
                        pending <- profileNumberOfPendingMatches(globalCodes)
                        hit <- countProfilesHitPedigrees(globalCodes)
                      } yield (dis, pending, hit))
                        .map {
                          case (dis, pen, hit) => {
                            ProfileDataViewCoincidencia(SampleCode(globalCodes), categoria.get.name, pd.internalSampleCode,
                              pd.assignee, m.lastMatchDate.date, hit, pen, dis)
                          }
                        }
                    }
                  }
                }
              }
            }
          }
        }
      }
  }
}