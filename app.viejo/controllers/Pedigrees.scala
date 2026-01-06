package controllers

import javax.inject.{Inject, Singleton}

import com.sun.org.apache.xpath.internal.functions.FuncTrue
import profiledata.{DeletedMotive, ProfileDataService, ProfileDataView}
import configdata.CategoryService
import matching.CollapseRequest
import pedigree._
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc._
import profile.ProfileService
import scenarios.ScenarioStatus
import trace.TraceService
import types.{MongoId, SampleCode}
import trace.{PedigreeEditInfo, PedigreeNewScenarioInfo, PedigreeStatusChangeInfo, TracePedigree, TraceService}
import java.util.Date

import play.api.libs.iteratee.Enumerator
import user.UserService

import scala.concurrent.Future
import scala.util.{Left, Right}

@Singleton
class Pedigrees @Inject() (
  pedigreeService: PedigreeService,
  pedigreeScenarioService: PedigreeScenarioService,
  pedigreeMatchesService: PedigreeMatchesService,
  bayesianNetworkService: BayesianNetworkService,
  pedigreeGenotypificationService: PedigreeGenotypificationService,
  categoryService: CategoryService = null,
  profileDataService: ProfileDataService,
  pedCheckService: PedCheckService = null,
  traceService:TraceService = null,
  userService: UserService = null) extends Controller with JsonSqlActions with JsonActions {

  def getCourtCases = Action.async(BodyParsers.parse.json) { request =>
    val pedigreeSearch = request.body.validate[PedigreeSearch]
    pedigreeSearch.fold(errors => {
      Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
    }, search => {
      userService.isSuperUser(search.user).flatMap(isSuperUser => {
        val newSearch = new PedigreeSearch(search.page, search.pageSize, search.user, isSuperUser, search.code, search.profile, search.status,
          search.sortField, search.ascending, search.caseType)

        pedigreeService.getAllCourtCases(newSearch).map { cases =>
          Ok(Json.toJson(cases))
        }
      })
    })
  }

  def getTotalCourtCases = Action.async(BodyParsers.parse.json) { request =>
    val pedigreeSearch = request.body.validate[PedigreeSearch]
    pedigreeSearch.fold(errors => {
      Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
    }, search => {
      userService.isSuperUser(search.user).flatMap(isSuperUser => {
        val newSearch = new PedigreeSearch(search.page, search.pageSize, search.user, isSuperUser, search.code, search.profile, search.status,
          search.sortField, search.ascending, search.caseType)

        pedigreeService.getTotalCourtCases(newSearch).map { size =>
          Ok("").withHeaders("X-PEDIGREES-LENGTH" -> size.toString)
        }
      })
    })
  }

  def createCourtCase = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[CourtCaseAttempt]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      courtCase => pedigreeService.createCourtCase(courtCase) map {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
        case Left(error) => BadRequest(Json.toJson(error))
      }
    )
  }

  def createMetadata(idCourtCase: Long) = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[PersonData]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      metadata => pedigreeService.createMetadata(idCourtCase, metadata) map {
        case Right(id) => Ok(Json.toJson(idCourtCase)).withHeaders("X-CREATED-ID" -> id.toString)
        case Left(error) => BadRequest(Json.toJson(error))
      }
    )
  }

  def getPedigree(pedigreeId: Long) = Action.async { request =>
    pedigreeService.getPedigree(pedigreeId).map ( _.fold(NoContent)(f => Ok(Json.toJson(f))))
  }

  def getPedigreeCoincidencia(id: Long): Action[AnyContent] = Action.async {
    request =>
      pedigreeService
        .getPedigreeCoincidencia(id)
        .map (f => Ok(Json.toJson(f)))
  }

  def getMatchByProfile(globalCodes: String) = Action.async { request =>
    pedigreeService.getMatchByProfile(globalCodes).map (f => Ok(Json.toJson(f)))
  }

  def getCourtCaseFull(courtCaseId: Long) = Action.async { request =>
    val userId = request.headers.get("X-USER").get

    userService.isSuperUser(userId).flatMap(isSuperUser => {
      pedigreeService.getCourtCase(courtCaseId, userId, isSuperUser).map(_.fold(NoContent)(f => Ok(Json.toJson(f))))
    })
  }

  
  def getByCourtCase(courtCaseId: Long): Action[AnyContent] = Action.async {
    request =>
      pedigreeService
        .getPedigreeByCourtCase(courtCaseId)
        .map(x => Ok(Json.toJson(x)))
  }


  def getMetadata(input: String,pageSize: Int,page : Int, idCourtCase: Long) = Action.async { request =>
    pedigreeService.getMetadata(PersonDataSearch(page,pageSize,idCourtCase,input)).map (f => Ok(Json.toJson(f)))
  }


  def updateCourtCase(courtCaseId: Long) = Action.async(BodyParsers.parse.json) { request =>
    val userId = request.headers.get("X-USER").get

    val input = request.body.validate[CourtCaseAttempt]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      courtCase => {
        userService.isSuperUser(userId).flatMap(isSuperUser => {
          pedigreeService.updateCourtCase(courtCaseId, courtCase, isSuperUser) map {
            case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
            case Left(error) => BadRequest(Json.toJson(error))
          }
        })
      }
    )
  }

  def updateMetadata(courtCaseId: Long , assignee : String) = Action.async(BodyParsers.parse.json) { request =>
    val userId = request.headers.get("X-USER").get

    val input = request.body.validate[PersonData]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      metadata => {
        userService.isSuperUser(userId).flatMap(isSuperUser => {
          pedigreeService.updateMetadata(courtCaseId, assignee, metadata, isSuperUser) map {
            case Right(id) => Ok(Json.toJson(courtCaseId)).withHeaders("X-CREATED-ID" -> id.toString)
            case Left(error) => BadRequest(Json.toJson(error))
          }
        })
      }
    )
  }

  def createGenogram = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[PedigreeGenogram]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      genogram => pedigreeService.addGenogram(genogram) map {
        case Right(id) => {
          val userId = request.headers.get("X-USER").get
//          traceService.addTracePedigree(TracePedigree(genogram._id, userId, new Date(),
//            PedigreeStatusChangeInfo(PedigreeStatus.UnderConstruction.toString)))
          Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
        }
        case Left(error) => BadRequest(Json.toJson(error))
      }
    )
  }

  def changePedigreeStatus(id: Long, status: String) = Action.async(BodyParsers.parse.json) { request =>
    val userId = request.headers.get("X-USER").get

      userService.isSuperUser(userId).flatMap(isSuperUser => {
        try {
          val pedigreeStatus = PedigreeStatus.withName(status)
          pedigreeStatus match {
            case PedigreeStatus.Active => activatePedigree(userId, isSuperUser, request)
            case PedigreeStatus.UnderConstruction => deactivatePedigree(id, userId, isSuperUser)
            case PedigreeStatus.Deleted => deletePedigree(id, userId, isSuperUser)
            case PedigreeStatus.Closed => closePedigree(id, userId, isSuperUser)
          }
      }
      catch {
         case e: NoSuchElementException => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Estado inválido")))
        }
      })
  }

  private def closePedigree(id: Long, userId: String, isSuperUser: Boolean) = {
    pedigreeService.changePedigreeStatus(id, PedigreeStatus.Closed, userId, isSuperUser) flatMap {
        case Right(id) => Future.successful(Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString))
        case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
      }
  }


  def fisicalDeletePedigree(id: Long) = Action.async(BodyParsers.parse.json) { request =>
    val userId = request.headers.get("X-USER").get

    userService.isSuperUser(userId).flatMap(isSuperUser => {
      pedigreeService.fisicalDeletePredigree(id, userId, isSuperUser) map {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
        case Left(error) => BadRequest(Json.toJson(error))
      }
    })

  }

  private def activatePedigree(userId: String, isSuperUser: Boolean, request: Request[JsValue]) = {

    val input = request.body.validate[PedigreeGenogram]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      genogram => {
        pedigreeService.addGenogram(genogram) flatMap {
          case Right(id) =>
            pedigreeService.changePedigreeStatus(id, PedigreeStatus.Active, userId, isSuperUser) flatMap {
              case Right(id) => {
                pedigreeGenotypificationService.generateGenotypificationAndFindMatches(id) map {
                  case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
                  case Left(error) => BadRequest(Json.toJson(error))
                }
              }
              case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
            }
          case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
        }
      }
    )
  }

  private def deletePedigree(id: Long, userId: String, isSuperUser: Boolean) = {
    pedigreeService.changePedigreeStatus(id, PedigreeStatus.Deleted, userId, isSuperUser) flatMap {
      case Right(id) => {
        pedigreeMatchesService.deleteMatches(id) map {
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
          case Left(error) => BadRequest(Json.toJson(error))
        }
      }
      case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
    }
  }

  private def deactivatePedigree(id: Long, userId: String, isSuperUser: Boolean) = {

    pedigreeService.changePedigreeStatus(id, PedigreeStatus.UnderConstruction, userId, isSuperUser) flatMap {
      case Right(id) => {
        pedigreeScenarioService.deleteAllScenarios(id) map {
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
          case Left(error) => BadRequest(Json.toJson(error))
        }
      }
      case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
    }
  }


  def findMatchesPedigree() = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[PedMatchCardSearch]

    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      search => {
        userService.isSuperUser(search.user).flatMap(isSuperUser => {
          val newSearch = new PedMatchCardSearch(search.user, isSuperUser, search.group, search.page, search.pageSize, search.profile, search.hourFrom,
            search.hourUntil, search.category, search.caseType, search.status, search.idCourtCase, search.sortField, search.ascending, search.pageMatch, search.pageSizeMatch)
          pedigreeMatchesService.getMatchesPedigree(newSearch).map {
            matches => Ok(Json.toJson(matches))
          }
        })
      }
    )
  }

  def findMatches() = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[PedigreeMatchCardSearch]

    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      search => {
        userService.isSuperUser(search.user).flatMap(isSuperUser => {
          val newSearch = new PedigreeMatchCardSearch(search.user, isSuperUser, search.group, search.page, search.pageSize, search.profile, search.hourFrom,
            search.hourUntil, search.category, search.caseType, search.status, search.idCourtCase)
          pedigreeMatchesService.getMatches(newSearch).map {
            matches => Ok(Json.toJson(matches))
          }
        })
      }
    )
  }

  def countMatches(): Action[JsValue] = Action.async(BodyParsers.parse.json) { 
    request =>
      val input = request.body.validate[PedigreeMatchCardSearch]
      input.fold(
        errors => {
          Future.successful(BadRequest(JsError.toFlatJson(errors)))
        },
        search => {
          userService
            .isSuperUser(search.user)
            .flatMap(
              isSuperUser => {
                val newSearch = new PedigreeMatchCardSearch(
                  search.user,
                  isSuperUser,
                  search.group,
                  search.page,
                  search.pageSize,
                  search.profile,
                  search.hourFrom,
                  search.hourUntil,
                  search.category,
                  search.caseType,
                  search.status,
                  search.idCourtCase
                )
                pedigreeMatchesService
                  .countMatches(newSearch)
                  .map {
                    size => Ok("")
                      .withHeaders("X-MATCHES-LENGTH" -> size.toString)
                  }
              }
            )
        }
      )
  }

  def getMatchesByGroup = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[PedigreeMatchGroupSearch]

    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      search => {
        userService.isSuperUser(search.user).flatMap(isSuperUser => {
          val newSearch = new PedigreeMatchGroupSearch(search.user, isSuperUser, search.id, search.groupBy, search.kind, search.page, search.pageSize,
            search.sortField, search.ascending, search.status, search.idCourCase)
          pedigreeMatchesService.getMatchesByGroup(newSearch).map {
            matches => Ok(Json.toJson(matches))
          }
        })
      }
    )

  }

  def countMatchesByGroup = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[PedigreeMatchGroupSearch]

    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      search => {
        userService.isSuperUser(search.user).flatMap(isSuperUser => {
          val newSearch = new PedigreeMatchGroupSearch(search.user, isSuperUser, search.id, search.groupBy, search.kind, search.page, search.pageSize,
            search.sortField, search.ascending, search.status, search.idCourCase)
          pedigreeMatchesService.countMatchesByGroup(newSearch) map { size =>
            Ok("").withHeaders("X-MATCHES-LENGTH" -> size.toString)
          }
        })
      })

  }


  def canEdit(pedigreeId: Long) = Action.async { request =>
    pedigreeMatchesService.allMatchesDiscarded(pedigreeId) map { editable =>
      Ok(Json.toJson(editable))
    }
  }

  def canDelete(pedigreeId: Long) = Action.async { request =>
    pedigreeService.doesntHaveGenotification(pedigreeId) map { editable =>
      Ok(Json.toJson(editable))
    }
  }

  def createScenario = Action.async(BodyParsers.parse.json) { request =>
    val s = request.body.validate[PedigreeScenario]
    val userId = request.headers.get("X-USER").get

    s.fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      scenario => pedigreeScenarioService.createScenario(scenario).map {
        case Right(id) => {
          traceService.addTracePedigree(TracePedigree(scenario.pedigreeId, userId, new Date(),
            PedigreeNewScenarioInfo(scenario._id.id,scenario.name)))
          Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.id)
        }
        case Left(error) => BadRequest(Json.toJson(error))
      }
    )
  }

  def changeScenarioStatus(status: String) = Action.async(BodyParsers.parse.json) { request =>
    val userId = request.headers.get("X-USER").get
    val input = request.body.validate[PedigreeScenario]

    input.fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      scenario => {
        userService.isSuperUser(userId).flatMap(isSuperUser => {
            try {
              val scenarioStatus = ScenarioStatus.withName(status)
              pedigreeScenarioService.changeScenarioStatus(scenario, scenarioStatus, userId, isSuperUser).map {
                case Right(idScenario) => Ok(Json.toJson(idScenario)).withHeaders("X-CREATED-ID" -> idScenario.id)
                case Left(error) => BadRequest(Json.toJson(error))
              }
            } catch {
              case e: NoSuchElementException => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Estado inválido")))
            }
        })
      }
    )
  }

  def confirmEscenarioScenario(status: String, pedigreeActivo: Boolean) = Action.async(BodyParsers.parse.json) { request =>
    val userId = request.headers.get("X-USER").get
    val input = request.body.validate[PedigreeScenario]

    input.fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      scenario => {
        userService.isSuperUser(userId).flatMap(isSuperUser => {
            try {
              val scenarioStatus = ScenarioStatus.withName(status)
              validateScenario(userId, isSuperUser, scenario, scenarioStatus, pedigreeActivo)
            } catch {
              case e: NoSuchElementException => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Estado inválido")))
            }
        })
      }
    )
  }

  private def validateScenario(userId: String, isSuperUser: Boolean, scenario: PedigreeScenario, scenarioStatus: ScenarioStatus.Value, pedigreeActivo: Boolean) = {
    pedigreeScenarioService.changeScenarioStatus(scenario, scenarioStatus, userId, isSuperUser).map {
      case Right(idScenario) => {
        pedigreeMatchesService.confirm(scenario.matchId.get, userId, isSuperUser).map {
          case Right(id) => {

            if (pedigreeActivo) {
              //generar copia del pedigree y activarlo
              pedigreeService.clonePedigree(scenario.pedigreeId,userId) map {
                case  Right(id) => activateClonePedigree(id,userId)
              }
            }
            Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
          }
          case Left(error) => BadRequest(Json.toJson(error))
        }
       Ok(Json.toJson(idScenario)).withHeaders("X-CREATED-ID" -> idScenario.id)
      }
      case Left(error) => BadRequest(Json.toJson(error))
    }
  }

  private def activateClonePedigree(id : Long,userId:String) : Unit = {

    pedigreeService.changePedigreeStatus(id, PedigreeStatus.Active, userId, true) map {
      case Right(id) => {
        pedigreeGenotypificationService.generateGenotypificationAndFindMatches(id)
      }
      case Left(error) => Future.successful(Left(error))
    }
  }

  private def deleteFindProfile(scenario: PedigreeScenario, userId: String): Unit = {
    val unknownProfile = scenario.genogram.collectFirst{case ind if ind.unknown && ind.globalCode.nonEmpty => ind}
    val deletedMotive = DeletedMotive(userId, "Baja por confirmación de pedigree", 2)
    profileDataService.deleteProfile(unknownProfile.get.globalCode.get, deletedMotive, userId)
  }

  def getScenarios(pedigreeId: Long) = Action.async { request =>
    pedigreeScenarioService.getScenarios(pedigreeId).map { scenarios => Ok(Json.toJson(scenarios)) }
  }

  def getScenario(scenarioId: String) = Action.async { request =>
    pedigreeScenarioService.getScenario(MongoId(scenarioId)).map ( _.fold(NoContent)(f => Ok(Json.toJson(f))))
  }

  def updateScenario = Action.async(BodyParsers.parse.json) { request =>
    val s = request.body.validate[PedigreeScenario]

    s.fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      scenario => pedigreeScenarioService.updateScenario(scenario).map {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.id)
        case Left(error) => BadRequest(Json.toJson(error))
      }
    )
  }

  def discard(matchId: String) = Action.async { request =>
    val userId = request.headers.get("X-USER").get

    userService.isSuperUser(userId).flatMap(isSuperUser => {
      pedigreeMatchesService.discard(matchId, userId, isSuperUser) map {
        case Right(result) => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString())
        case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
      }
    })
  }

  def getLR = Action.async(BodyParsers.parse.json) { request =>
    val s = request.body.validate[PedigreeScenario]

    s.fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      scenario => bayesianNetworkService.calculateProbability(scenario).map { lr => Ok(Json.toJson(lr)) }
    )
  }

  def getCaseTypes = Action.async {
    pedigreeService.getCaseTypes map {
      caseTypes => Ok(Json.toJson(caseTypes))
    }
  }

  def getProfiles(id: Long,pageSize:Int,page:Int,input: Option[String], isReference: Boolean, statusProfile: Option[String]) = Action.async { request =>
    statusProfile match {
      case None =>pedigreeService.getProfiles(CourtCasePedigreeSearch(page, pageSize, id, input), isReference).map {
        case result => Ok(Json.toJson(result))
      }
      case Some(statusPro) =>{
        val statusP = CollapsingStatus.withName(statusPro)

        pedigreeService.getProfiles(CourtCasePedigreeSearch(page, pageSize, id, input, None, None, None, Some(statusP)), isReference).map {
          case result => Ok(Json.toJson(result))
        }
      }
    }

  }

  def profileNodo(idCourtCase: Long, codigoGlobal: String) = Action.async { request =>
    pedigreeService.getProfilesNodo(idCourtCase,codigoGlobal).map {
      case result => Ok(Json.toJson(result))
    }
  }

  def getProfilesNodeAssociation(id: Long,pageSize:Int,page:Int,input: Option[String], isReference: Boolean, profilesCod : List[String],statusProfile:Option[String]) = Action.async { request =>
    pedigreeService.getProfilesNodeAssociation(CourtCasePedigreeSearch(page,pageSize,id,input,None,None,None,statusProfile.map(statusProfile =>pedigree.CollapsingStatus.withName(statusProfile)),None),isReference, profilesCod).map {
      case result => Ok(Json.toJson(result))
    }
  }

  def getTotalProfiles(id: Long,pageSize:Int,page:Int,input: Option[String], isReference: Boolean, statusProfile: Option[String]) = Action.async { request =>
      statusProfile match{
        case None =>  pedigreeService.getTotalProfiles(CourtCasePedigreeSearch(page,pageSize,id,input),isReference).map {
                          case result => Ok(Json.toJson(result))
                        }
        case Some(statusPro)=>{
          val statusP = CollapsingStatus.withName(statusPro)
          pedigreeService.getTotalProfiles(CourtCasePedigreeSearch(page,pageSize,id,input,None,None,None, Some( statusP)),isReference).map {
              case result => Ok(Json.toJson(result))
            }
        }
      }

  }

  def getTotalProfilesNodeAssociation(id: Long,pageSize:Int,page:Int,input: Option[String], isReference: Boolean, profilesCod : List[String],statusProfile:Option[String]) = Action.async { request =>
    pedigreeService.getTotalProfilesNodeAssociation(CourtCasePedigreeSearch(page,pageSize,id,input,None,None,None,statusProfile.map(statusProfile =>pedigree.CollapsingStatus.withName(statusProfile)),None),isReference, profilesCod).map {
      case result => Ok(Json.toJson(result))
    }
  }

  def getTotalMetadata(idCourtCase: Long,pageSize:Int,page:Int,input: String) = Action.async { request =>
    pedigreeService.getTotalMetadata(PersonDataSearch(page,pageSize,idCourtCase,input)).map {
      case result => Ok(Json.toJson(result))
    }
  }

  def addProfiles() = Action.async(BodyParsers.parse.json){
    request =>{
      val input = request.body.validate[AssociateProfile]
      input.fold(errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
        courtCaseProfiles => {
          pedigreeService.addProfiles(courtCaseProfiles.profiles, courtCaseProfiles.isReference).map{
            case Left(e) => BadRequest(Json.obj("message" -> e))
            case Right(()) => Ok.withHeaders("X-CREATED-ID" -> (courtCaseProfiles.profiles).headOption.map(_.courtcaseId).getOrElse(0).toString)
          } }
      )
    }
  }
  def removeProfiles() = Action.async(BodyParsers.parse.json){
    request =>{
      val input = request.body.validate[List[CaseProfileAdd]]
      input.fold(errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
        courtCaseProfiles => {
          pedigreeService.removeProfiles(courtCaseProfiles).map{
            case Left(e) => BadRequest(Json.obj("message" -> e))
            case Right(()) => Ok.withHeaders("X-CREATED-ID" -> courtCaseProfiles.headOption.map(_.courtcaseId).getOrElse(0).toString)
          }
        }
      )

    }
  }

  def removeMetadata(idCourtCase: Long) = Action.async(BodyParsers.parse.json){
    request =>{
      val input = request.body.validate[PersonData]
      input.fold(errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
        personData => {
          pedigreeService.removeMetadata(idCourtCase, personData).map{
            case Left(e) => BadRequest(Json.obj("message" -> e))
            case Right(()) => Ok.withHeaders("X-CREATED-ID" -> personData.alias)
          }
        }
      )

    }
  }

  def filterProfilesForPedigree(input: String,idCase:Long, isReference: Boolean, tipo : Int,page:Int,pageSize:Int) = Action.async {
    val categories = categoryService.listCategories
    pedigreeService.filterProfileDatasWithFilter(input,idCase){ profile =>
      categories(profile.category).pedigreeAssociation  && (categories(profile.category).isReference != isReference) &&
      categories(profile.category).tipo.getOrElse(1) == tipo
    }.map { profileDatas => Ok(Json.toJson(profileDatas.drop(page*pageSize).take(pageSize))) }

  }
  def countProfilesForPedigree(input: String,idCase:Long, isReference: Boolean, tipo : Int) = Action.async {
    val categories = categoryService.listCategories
    pedigreeService.filterProfileDatasWithFilter(input,idCase){ profile =>
      categories(profile.category).pedigreeAssociation  && (categories(profile.category).isReference != isReference) &&
        categories(profile.category).tipo.getOrElse(1) == tipo
    }.map { profileDatas => Ok(Json.toJson(profileDatas.length)) }
  }

  def addBatches() = Action.async(BodyParsers.parse.json){
    request =>{
      val input = request.body.validate[CaseBatchAdd]
      input.fold(errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
        batchesToImport => {
          pedigreeService.addBatches(batchesToImport).map{
            case Left(e) => BadRequest(Json.obj("message" -> e))
            case Right(()) => Ok
          }
        }
      )

    }
  }

/*  def getCourtCasePedigrees(id: Long,pageSize:Int,page:Int,input: Option[String]) = Action.async { request =>
    pedigreeService.getCourtCasePedigrees(CourtCasePedigreeSearch(page,pageSize,id,input)).map {
      case result => Ok(Json.toJson(result))
    }
  }*/

  def getCourtCasePedigrees = Action.async(BodyParsers.parse.json) { request =>
    val pedigreesSearch = request.body.validate[CourtCasePedigreeSearch]
    pedigreesSearch.fold(errors => {
      Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
    }, search => {
      pedigreeService.getCourtCasePedigrees(search).map {
        case result => Ok(Json.toJson(result))
      }
    })
  }


  def getTotalCourtCasePedigrees(id: Long,pageSize:Int,page:Int,input: Option[String]) = Action.async { request =>
    pedigreeService.getTotalCourtCasePedigrees(CourtCasePedigreeSearch(page,pageSize,id,input)).map {
      case result => Ok(Json.toJson(result))
    }
  }

  def createOrUpdatePedigreeMetadata = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[PedigreeMetaData]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      pedigreeMetaData => pedigreeService.createOrUpdatePedigreeMetadata(pedigreeMetaData) map {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
        case Left(error) => BadRequest(Json.toJson(error))
      }
    )
  }

  def createPedigree = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[PedigreeDataCreation]
    val userId = request.headers.get("X-USER").get
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      pedigreData => pedigreeService.createPedigree(pedigreData,userId,pedigreData.copiedFrom) map {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
        case Left(error) => BadRequest(Json.toJson(error))
      }
    )
  }

  def collapse = Action.async(BodyParsers.parse.json) { request =>
    val userId = request.headers.get("X-USER").get
    val input = request.body.validate[CollapsingRequest]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      req => {
        pedigreeService.collapse(req.courtcaseId,userId)
        Future.successful(Ok(Json.toJson(req.courtcaseId)).withHeaders("X-CREATED-ID" -> req.courtcaseId.toString))
      }
    )
  }

  def changeCourtCaseStatus(id: Long, status: String, closeProfiles: Boolean) = Action.async(BodyParsers.parse.json) { request =>
    val userId = request.headers.get("X-USER").get

    userService.isSuperUser(userId).flatMap(isSuperUser => {
      try {
        val pedigreeStatus = PedigreeStatus.withName(status)
        pedigreeStatus match {
          case PedigreeStatus.Deleted => deleteCourtCase(id, userId, isSuperUser, closeProfiles)
          case PedigreeStatus.Closed => closeCourtCase(id, userId, isSuperUser, closeProfiles)
        }
      }
      catch {
        case e: NoSuchElementException => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Estado inválido")))
      }
    })
  }

  private def deleteCourtCase(idCourtCase: Long, userId: String, isSuperUser: Boolean, closeProfiles: Boolean) = {
    pedigreeService.changeCourtCaseStatus(idCourtCase, PedigreeStatus.Deleted, userId, isSuperUser) flatMap {
      case Right(id) => {
        //se deben cerrar tambien los pedigrees del caso
        pedigreeService.closeAllPedigrees(idCourtCase,userId).map( result => {
          if( closeProfiles ) {
            //dar de baja todos los perfiles del caso
            closeAllProfiles(idCourtCase, userId);
          }
          result
        }).map(result => {
          Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
        })
//        Future.successful()
      }
      case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
    }
  }

  def canDeleteCourtCase(courtCaseId: Long) = Action.async { request =>
    pedigreeService.hasPedigreeMatches(courtCaseId) map { deleteable =>
      Ok(Json.toJson(!deleteable))
    }
  }

  def canCloseCourtCase(courtCaseId: Long) = Action.async { request =>
    pedigreeService.hasPendingPedigreeMatches(courtCaseId) map { closeable =>
      Ok(Json.toJson(!closeable))
    }
  }

  private def closeCourtCase(idCourtCase: Long, userId: String, isSuperUser: Boolean, closeProfiles: Boolean) = {
    pedigreeService.changeCourtCaseStatus(idCourtCase, PedigreeStatus.Closed, userId, isSuperUser) flatMap {
      case Right(id) => {
        //se deben cerrar tambien los pedigrees del caso
        pedigreeService.closeAllPedigrees(idCourtCase,userId)

        if( closeProfiles ) {
            //dar de baja todos los perfiles del caso
            closeAllProfiles(idCourtCase, userId);
        }

        Future.successful(Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString))
      }
      case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
    }
  }

  private def closeAllProfiles(idCourtCase: Long, userId: String) : Future[Unit] = {
    val deletedMotive = DeletedMotive(userId, "Baja por cierre de caso", 2)
    pedigreeService.getProfilesToDelete(idCourtCase) flatMap { referenceProfiles =>{
      Future.sequence(
        referenceProfiles.map( globalCode => {
          profileDataService.deleteProfile(globalCode, deletedMotive, userId,false)
        })).map(result => ()).recoverWith{
        case _:Exception => Future.successful(())
        }
      }
    }
  }

  def confirmSelectedCollapsing() = Action.async(BodyParsers.parse.json) { request =>
    val collapsingRequestJson = request.body.validate[CollapseRequest]

    collapsingRequestJson.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("message" -> JsError.toFlatJson(errors))))
      },
      collapsingRequest => {
        pedigreeService.collapseGroup(collapsingRequest).map{
          case Right(_) => Ok(Json.toJson(collapsingRequest.globalCodeParent))
                        .withHeaders("X-CREATED-ID" -> collapsingRequest.globalCodeParent)
          case Left(error) => BadRequest(Json.toJson(error))
        }
      })
  }

  def disassociateGroupedProfiles()=  Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[List[CaseProfileAdd]]
    input.fold(errors => {
      Future.successful(BadRequest(JsError.toFlatJson(errors)))
    },
      courtCaseProfiles => {
      pedigreeService.disassociateGroupedProfiles(courtCaseProfiles) map {
      case Left(error) => BadRequest(Json.toJson(error))
      case Right(_) => Ok
    }
  }
    )
  }

  def doesntHaveActivePedigrees(idCourtCase: Long)=  Action.async { request =>
        pedigreeService.doesntHaveActivePedigrees(idCourtCase) map { result =>
          Ok(Json.toJson(result))
        }
  }


  def areAssignedToPedigree()=  Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[CollapseRequest]
    input.fold(errors => {
      Future.successful(BadRequest(JsError.toFlatJson(errors)))
    },
      courtCaseProfiles => {
        pedigreeService.areAssignedToPedigree(courtCaseProfiles.globalCodeChildren,courtCaseProfiles.courtCaseId) map {
          case Left(e) => BadRequest(Json.obj("message" -> e))
          case Right(_) => Ok
        }
      }
    )
  }


  def getProfilesInactive(id: Long,pageSize:Int,page:Int,input: Option[String], statusProfile: String, groupedBy: String) = Action.async { request =>

        val statusP = CollapsingStatus.withName(statusProfile)
        pedigreeService.getProfilesInactive(CourtCasePedigreeSearch(page, pageSize, id, input, None, None, None, Some(statusP),Some(groupedBy))).map {
          case result => Ok(Json.toJson(result))
        }
      }



  def getTotalProfilesInactive(id: Long,pageSize:Int,page:Int,input: Option[String], statusProfile: String, groupedBy: String) = Action.async { request =>

        val statusP = CollapsingStatus.withName(statusProfile)
        pedigreeService.getTotalProfilesInactive(CourtCasePedigreeSearch(page,pageSize,id,input,None,None,None, Some( statusP),Some(groupedBy))).map {
          case result => Ok(Json.toJson(result))
        }
      }


  def getPedCheck(pedigreeId: Long,idCourtCase:Long) = Action.async { request =>
    pedCheckService.getPedCheckProfiles(pedigreeId,idCourtCase).map( x =>  Ok(Json.toJson(x)))
  }

  def generatePedCheck(pedigreeId: Long,idCourtCase:Long) = Action.async { request =>
    val userId = request.headers.get("X-USER").get
    pedCheckService.generatePedCheck(pedigreeId,idCourtCase,userId).map{
      case Right(x) =>  Ok(Json.toJson(x))
      case Left(error) => BadRequest(Json.toJson(error))
    }
  }

  def masiveDiscardByGroup(id: String, group: String) = Action.async { request =>
    val userId = request.headers.get("X-USER").get

    userService.isSuperUser(userId).flatMap(isSuperUser => {
      pedigreeMatchesService.masiveGroupDiscardByGroup(id, group, isSuperUser, userId) map {
        case Right(result) => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString())
        case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
      }
    })
  }

  def exportMatchesByGroup = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[PedigreeMatchGroupSearch]

    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      search => {
        userService.isSuperUser(search.user).flatMap(isSuperUser => {
          val newSearch = new PedigreeMatchGroupSearch(search.user, isSuperUser, search.id, search.groupBy, search.kind, search.page, search.pageSize,
            search.sortField, search.ascending, search.status, search.idCourCase)
          pedigreeMatchesService.getMatchesByGroup(newSearch).map {
            matches => {
              pedigreeMatchesService.exportMatchesByGroup(matches)
              Ok(Json.toJson(matches))
            }
          }
        })
      }
    )

  }

  def getMatchesExportFile() = Action.async { request =>
    val file = pedigreeMatchesService.getMatchesExportFile()
    val fileContent: Enumerator[Array[Byte]] = Enumerator.fromFile(file)
    Future.successful(Result(header = ResponseHeader(200), body = fileContent))
  }

}