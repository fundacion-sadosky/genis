package controllers

import jakarta.inject.{Inject, Singleton}
import matching.CollapseRequest
import pedigree.*
import scenarios.ScenarioStatus
import profiledata.{DeletedMotive, ProfileDataService}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.*
import services.UserService
import trace.{PedigreeNewScenarioInfo, TracePedigree, TraceService}
import matching.MongoId

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PedigreesController @Inject() (
  pedigreeService: PedigreeService,
  pedigreeScenarioService: PedigreeScenarioService,
  pedigreeMatchesService: PedigreeMatchesService,
  bayesianNetworkService: BayesianNetworkService,
  pedigreeGenotypificationService: PedigreeGenotypificationService,
  profileDataService: ProfileDataService,
  pedCheckService: PedCheckService,
  traceService: TraceService,
  userService: UserService,
  cc: ControllerComponents
)(using ec: ExecutionContext) extends AbstractController(cc):

  def getCourtCases: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[PedigreeSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search =>
        userService.isSuperUser(search.user).flatMap { isSuperUser =>
          val newSearch = search.copy(isOwnCases = !isSuperUser)
          pedigreeService.getAllCourtCases(newSearch).map(cases => Ok(Json.toJson(cases)))
        }
    )
  }

  def getTotalCourtCases: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[PedigreeSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search =>
        userService.isSuperUser(search.user).flatMap { isSuperUser =>
          val newSearch = search.copy(isOwnCases = !isSuperUser)
          pedigreeService.getTotalCourtCases(newSearch).map(size =>
            Ok("").withHeaders("X-PEDIGREES-LENGTH" -> size.toString))
        }
    )
  }

  def createCourtCase: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CourtCaseAttempt].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      courtCase =>
        pedigreeService.createCourtCase(courtCase).map {
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
          case Left(error) => BadRequest(Json.toJson(error))
        }
    )
  }

  def createMetadata(idCourtCase: Long): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[PersonData].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      metadata =>
        pedigreeService.createMetadata(idCourtCase, metadata).map {
          case Right(id) => Ok(Json.toJson(idCourtCase)).withHeaders("X-CREATED-ID" -> id.toString)
          case Left(error) => BadRequest(Json.toJson(error))
        }
    )
  }

  def getPedigree(pedigreeId: Long): Action[AnyContent] = Action.async {
    pedigreeService.getPedigree(pedigreeId).map(_.fold(NoContent)(f => Ok(Json.toJson(f))))
  }

  def getPedigreeCoincidencia(id: Long): Action[AnyContent] = Action.async {
    pedigreeService.getPedigreeCoincidencia(id).map(f => Ok(Json.toJson(f)))
  }

  def getMatchByProfile(globalCodes: String): Action[AnyContent] = Action.async {
    pedigreeService.getMatchByProfile(globalCodes).map(f => Ok(Json.toJson(f)))
  }

  def getCourtCaseFull(courtCaseId: Long): Action[AnyContent] = Action.async { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      pedigreeService.getCourtCase(courtCaseId, userId, isSuperUser).map(_.fold(NoContent)(f => Ok(Json.toJson(f))))
    }
  }

  def getByCourtCase(courtCaseId: Long): Action[AnyContent] = Action.async {
    pedigreeService.getPedigreeByCourtCase(courtCaseId).map(x => Ok(Json.toJson(x)))
  }

  def getMetadata(input: String, pageSize: Int, page: Int, idCourtCase: Long): Action[AnyContent] = Action.async {
    pedigreeService.getMetadata(PersonDataSearch(page, pageSize, idCourtCase, input)).map(f => Ok(Json.toJson(f)))
  }

  def updateCourtCase(courtCaseId: Long): Action[JsValue] = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    request.body.validate[CourtCaseAttempt].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      courtCase =>
        userService.isSuperUser(userId).flatMap { isSuperUser =>
          pedigreeService.updateCourtCase(courtCaseId, courtCase, isSuperUser).map {
            case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
            case Left(error) => BadRequest(Json.toJson(error))
          }
        }
    )
  }

  def updateMetadata(courtCaseId: Long, assignee: String): Action[JsValue] = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    request.body.validate[PersonData].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      metadata =>
        userService.isSuperUser(userId).flatMap { isSuperUser =>
          pedigreeService.updateMetadata(courtCaseId, assignee, metadata, isSuperUser).map {
            case Right(id) => Ok(Json.toJson(courtCaseId)).withHeaders("X-CREATED-ID" -> id.toString)
            case Left(error) => BadRequest(Json.toJson(error))
          }
        }
    )
  }

  def createGenogram: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[PedigreeGenogram].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      genogram =>
        pedigreeService.addGenogram(genogram).map {
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
          case Left(error) => BadRequest(Json.toJson(error))
        }
    )
  }

  def changePedigreeStatus(id: Long, status: String): Action[JsValue] = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      try
        val pedigreeStatus = PedigreeStatus.withName(status)
        pedigreeStatus match
          case PedigreeStatus.Active => activatePedigree(userId, isSuperUser, request)
          case PedigreeStatus.UnderConstruction => deactivatePedigree(id, userId, isSuperUser)
          case PedigreeStatus.Deleted => deletePedigree(id, userId, isSuperUser)
          case PedigreeStatus.Closed => closePedigree(id, userId, isSuperUser)
      catch
        case _: NoSuchElementException =>
          Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Estado inválido")))
    }
  }

  private def closePedigree(id: Long, userId: String, isSuperUser: Boolean) =
    pedigreeService.changePedigreeStatus(id, PedigreeStatus.Closed, userId, isSuperUser).flatMap {
      case Right(id) => Future.successful(Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString))
      case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
    }

  private def activatePedigree(userId: String, isSuperUser: Boolean, request: Request[JsValue]) =
    request.body.validate[PedigreeGenogram].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      genogram =>
        pedigreeService.addGenogram(genogram).flatMap {
          case Right(id) =>
            pedigreeService.changePedigreeStatus(id, PedigreeStatus.Active, userId, isSuperUser).flatMap {
              case Right(id) =>
                pedigreeGenotypificationService.generateGenotypificationAndFindMatches(id).map {
                  case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
                  case Left(error) => BadRequest(Json.toJson(error))
                }
              case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
            }
          case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
        }
    )

  private def deletePedigree(id: Long, userId: String, isSuperUser: Boolean) =
    pedigreeService.changePedigreeStatus(id, PedigreeStatus.Deleted, userId, isSuperUser).flatMap {
      case Right(id) =>
        pedigreeMatchesService.deleteMatches(id).map {
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
          case Left(error) => BadRequest(Json.toJson(error))
        }
      case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
    }

  private def deactivatePedigree(id: Long, userId: String, isSuperUser: Boolean) =
    pedigreeService.changePedigreeStatus(id, PedigreeStatus.UnderConstruction, userId, isSuperUser).flatMap {
      case Right(id) =>
        pedigreeScenarioService.deleteAllScenarios(id).map {
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
          case Left(error) => BadRequest(Json.toJson(error))
        }
      case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
    }

  def fisicalDeletePedigree(id: Long): Action[JsValue] = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      pedigreeService.fisicalDeletePredigree(id, userId, isSuperUser).map {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
        case Left(error) => BadRequest(Json.toJson(error))
      }
    }
  }

  def findMatchesPedigree(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[PedMatchCardSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search =>
        userService.isSuperUser(search.user).flatMap { isSuperUser =>
          val newSearch = PedMatchCardSearch(search.user, isSuperUser, search.group, search.page, search.pageSize,
            search.profile, search.hourFrom, search.hourUntil, search.category, search.caseType, search.status,
            search.idCourtCase, search.sortField, search.ascending, search.pageMatch, search.pageSizeMatch)
          pedigreeMatchesService.getMatchesPedigree(newSearch).map(matches => Ok(Json.toJson(matches)))
        }
    )
  }

  def findMatches(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[PedigreeMatchCardSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search =>
        userService.isSuperUser(search.user).flatMap { isSuperUser =>
          val newSearch = PedigreeMatchCardSearch(search.user, isSuperUser, search.group, search.page, search.pageSize,
            search.profile, search.hourFrom, search.hourUntil, search.category, search.caseType, search.status, search.idCourtCase)
          pedigreeMatchesService.getMatches(newSearch).map(matches => Ok(Json.toJson(matches)))
        }
    )
  }

  def countMatches(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[PedigreeMatchCardSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search =>
        userService.isSuperUser(search.user).flatMap { isSuperUser =>
          val newSearch = PedigreeMatchCardSearch(search.user, isSuperUser, search.group, search.page, search.pageSize,
            search.profile, search.hourFrom, search.hourUntil, search.category, search.caseType, search.status, search.idCourtCase)
          pedigreeMatchesService.countMatches(newSearch).map(size =>
            Ok("").withHeaders("X-MATCHES-LENGTH" -> size.toString))
        }
    )
  }

  def getMatchesByGroup: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[PedigreeMatchGroupSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search =>
        userService.isSuperUser(search.user).flatMap { isSuperUser =>
          val newSearch = PedigreeMatchGroupSearch(search.user, isSuperUser, search.id, search.groupBy, search.kind,
            search.page, search.pageSize, search.sortField, search.ascending, search.status, search.idCourCase)
          pedigreeMatchesService.getMatchesByGroup(newSearch).map(matches => Ok(Json.toJson(matches)))
        }
    )
  }

  def countMatchesByGroup: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[PedigreeMatchGroupSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search =>
        userService.isSuperUser(search.user).flatMap { isSuperUser =>
          val newSearch = PedigreeMatchGroupSearch(search.user, isSuperUser, search.id, search.groupBy, search.kind,
            search.page, search.pageSize, search.sortField, search.ascending, search.status, search.idCourCase)
          pedigreeMatchesService.countMatchesByGroup(newSearch).map(size =>
            Ok("").withHeaders("X-MATCHES-LENGTH" -> size.toString))
        }
    )
  }

  def masiveDiscardByGroup(id: String, group: String): Action[AnyContent] = Action.async { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      pedigreeMatchesService.masiveGroupDiscardByGroup(id, group, isSuperUser, userId).map {
        case Right(result) => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString)
        case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
      }
    }
  }

  def canEdit(pedigreeId: Long): Action[AnyContent] = Action.async {
    pedigreeService.doesntHaveGenotification(pedigreeId).map(editable => Ok(Json.toJson(editable)))
  }

  def canDelete(pedigreeId: Long): Action[AnyContent] = Action.async {
    pedigreeService.doesntHaveGenotification(pedigreeId).map(editable => Ok(Json.toJson(editable)))
  }

  def createScenario: Action[JsValue] = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    request.body.validate[PedigreeScenario].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      scenario =>
        pedigreeScenarioService.createScenario(scenario).map {
          case Right(id) =>
            traceService.addTracePedigree(TracePedigree(scenario.pedigreeId, userId, new Date(),
              PedigreeNewScenarioInfo(scenario._id.id, scenario.name)))
            Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.id)
          case Left(error) => BadRequest(Json.toJson(error))
        }
    )
  }

  def changeScenarioStatus(status: String): Action[JsValue] = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    request.body.validate[PedigreeScenario].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      scenario =>
        userService.isSuperUser(userId).flatMap { isSuperUser =>
          try
            val scenarioStatus = ScenarioStatus.withName(status)
            pedigreeScenarioService.changeScenarioStatus(scenario, scenarioStatus, userId, isSuperUser).map {
              case Right(idScenario) => Ok(Json.toJson(idScenario)).withHeaders("X-CREATED-ID" -> idScenario.id)
              case Left(error) => BadRequest(Json.toJson(error))
            }
          catch
            case _: NoSuchElementException =>
              Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Estado inválido")))
        }
    )
  }

  def confirmEscenarioScenario(status: String, pedigreeActivo: Boolean): Action[JsValue] = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    request.body.validate[PedigreeScenario].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      scenario =>
        userService.isSuperUser(userId).flatMap { isSuperUser =>
          try
            val scenarioStatus = ScenarioStatus.withName(status)
            validateScenario(userId, isSuperUser, scenario, scenarioStatus, pedigreeActivo)
          catch
            case _: NoSuchElementException =>
              Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Estado inválido")))
        }
    )
  }

  private def validateScenario(userId: String, isSuperUser: Boolean, scenario: PedigreeScenario,
    scenarioStatus: ScenarioStatus.Value, pedigreeActivo: Boolean) =
    pedigreeScenarioService.changeScenarioStatus(scenario, scenarioStatus, userId, isSuperUser).flatMap {
      case Right(idScenario) =>
        pedigreeMatchesService.confirm(scenario.matchId.get, userId, isSuperUser).map {
          case Right(id) =>
            if pedigreeActivo then
              pedigreeService.clonePedigree(scenario.pedigreeId, userId).map {
                case Right(id) => activateClonePedigree(id, userId)
                case _ =>
              }
            Ok(Json.toJson(idScenario)).withHeaders("X-CREATED-ID" -> idScenario.id)
          case Left(error) => BadRequest(Json.toJson(error))
        }
      case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
    }

  private def activateClonePedigree(id: Long, userId: String): Unit =
    pedigreeService.changePedigreeStatus(id, PedigreeStatus.Active, userId, true).map {
      case Right(id) => pedigreeGenotypificationService.generateGenotypificationAndFindMatches(id)
      case Left(_) =>
    }

  def getScenarios(pedigreeId: Long): Action[AnyContent] = Action.async {
    pedigreeScenarioService.getScenarios(pedigreeId).map(scenarios => Ok(Json.toJson(scenarios)))
  }

  def getScenario(scenarioId: String): Action[AnyContent] = Action.async {
    pedigreeScenarioService.getScenario(MongoId(scenarioId)).map(_.fold(NoContent)(f => Ok(Json.toJson(f))))
  }

  def updateScenario: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[PedigreeScenario].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      scenario =>
        pedigreeScenarioService.updateScenario(scenario).map {
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.id)
          case Left(error) => BadRequest(Json.toJson(error))
        }
    )
  }

  def discard(matchId: String): Action[AnyContent] = Action.async { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      pedigreeMatchesService.discard(matchId, userId, isSuperUser).map {
        case Right(result) => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString)
        case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
      }
    }
  }

  def getLR: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[PedigreeScenario].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      scenario => bayesianNetworkService.calculateProbability(scenario).map(lr => Ok(Json.toJson(lr)))
    )
  }

  def getCaseTypes: Action[AnyContent] = Action.async {
    pedigreeService.getCaseTypes().map(caseTypes => Ok(Json.toJson(caseTypes)))
  }

  def getProfiles(id: Long, pageSize: Int, page: Int, input: Option[String], isReference: Boolean, statusProfile: Option[String]): Action[AnyContent] = Action.async {
    val search = statusProfile match
      case None => CourtCasePedigreeSearch(page, pageSize, id, input)
      case Some(statusPro) => CourtCasePedigreeSearch(page, pageSize, id, input, None, None, None, Some(CollapsingStatus.withName(statusPro)))
    pedigreeService.getProfiles(search, isReference).map(result => Ok(Json.toJson(result)))
  }

  def profileNodo(idCourtCase: Long, codigoGlobal: String): Action[AnyContent] = Action.async {
    pedigreeService.getProfilesNodo(idCourtCase, codigoGlobal).map(result => Ok(Json.toJson(result)))
  }

  def getProfilesNodeAssociation(id: Long, pageSize: Int, page: Int, input: Option[String], isReference: Boolean, profilesCod: List[String], statusProfile: Option[String]): Action[AnyContent] = Action.async {
    val search = CourtCasePedigreeSearch(page, pageSize, id, input, None, None, None, statusProfile.map(s => CollapsingStatus.withName(s)), None)
    pedigreeService.getProfilesNodeAssociation(search, isReference, profilesCod).map(result => Ok(Json.toJson(result)))
  }

  def getTotalProfiles(id: Long, pageSize: Int, page: Int, input: Option[String], isReference: Boolean, statusProfile: Option[String]): Action[AnyContent] = Action.async {
    val search = statusProfile match
      case None => CourtCasePedigreeSearch(page, pageSize, id, input)
      case Some(statusPro) => CourtCasePedigreeSearch(page, pageSize, id, input, None, None, None, Some(CollapsingStatus.withName(statusPro)))
    pedigreeService.getTotalProfiles(search, isReference).map(result => Ok(Json.toJson(result)))
  }

  def getTotalProfilesNodeAssociation(id: Long, pageSize: Int, page: Int, input: Option[String], isReference: Boolean, profilesCod: List[String], statusProfile: Option[String]): Action[AnyContent] = Action.async {
    val search = CourtCasePedigreeSearch(page, pageSize, id, input, None, None, None, statusProfile.map(s => CollapsingStatus.withName(s)), None)
    pedigreeService.getTotalProfilesNodeAssociation(search, isReference, profilesCod).map(result => Ok(Json.toJson(result)))
  }

  def getTotalMetadata(idCourtCase: Long, pageSize: Int, page: Int, input: String): Action[AnyContent] = Action.async {
    pedigreeService.getTotalMetadata(PersonDataSearch(page, pageSize, idCourtCase, input)).map(result => Ok(Json.toJson(result)))
  }

  def addProfiles(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[AssociateProfile].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      courtCaseProfiles =>
        pedigreeService.addProfiles(courtCaseProfiles.profiles, courtCaseProfiles.isReference).map {
          case Left(e) => BadRequest(Json.obj("message" -> e))
          case Right(()) => Ok.withHeaders("X-CREATED-ID" -> courtCaseProfiles.profiles.headOption.map(_.courtcaseId).getOrElse(0L).toString)
        }
    )
  }

  def removeProfiles(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[List[CaseProfileAdd]].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      courtCaseProfiles =>
        pedigreeService.removeProfiles(courtCaseProfiles).map {
          case Left(e) => BadRequest(Json.obj("message" -> e))
          case Right(()) => Ok.withHeaders("X-CREATED-ID" -> courtCaseProfiles.headOption.map(_.courtcaseId).getOrElse(0L).toString)
        }
    )
  }

  def removeMetadata(idCourtCase: Long): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[PersonData].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      personData =>
        pedigreeService.removeMetadata(idCourtCase, personData).map {
          case Left(e) => BadRequest(Json.obj("message" -> e))
          case Right(()) => Ok.withHeaders("X-CREATED-ID" -> personData.alias)
        }
    )
  }

  def filterProfilesForPedigree(input: String, idCase: Long, isReference: Boolean, tipo: Int, page: Int, pageSize: Int): Action[AnyContent] = Action.async {
    pedigreeService.filterProfileDatasWithFilter(input, idCase) { profile =>
      !isReference && tipo == 1 // simplified — category info not available without async call
    }.map(profileDatas => Ok(Json.toJson(profileDatas.drop(page * pageSize).take(pageSize))))
  }

  def countProfilesForPedigree(input: String, idCase: Long, isReference: Boolean, tipo: Int): Action[AnyContent] = Action.async {
    pedigreeService.filterProfileDatasWithFilter(input, idCase) { profile =>
      !isReference && tipo == 1
    }.map(profileDatas => Ok(Json.toJson(profileDatas.length)))
  }

  def addBatches(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CaseBatchAdd].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      batchesToImport =>
        pedigreeService.addBatches(batchesToImport).map {
          case Left(e) => BadRequest(Json.obj("message" -> e))
          case Right(()) => Ok
        }
    )
  }

  def getCourtCasePedigrees: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CourtCasePedigreeSearch].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      search => pedigreeService.getCourtCasePedigrees(search).map(result => Ok(Json.toJson(result)))
    )
  }

  def getTotalCourtCasePedigrees(id: Long, pageSize: Int, page: Int, input: Option[String]): Action[AnyContent] = Action.async {
    pedigreeService.getTotalCourtCasePedigrees(CourtCasePedigreeSearch(page, pageSize, id, input)).map(result => Ok(Json.toJson(result)))
  }

  def createOrUpdatePedigreeMetadata: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[PedigreeMetaData].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      pedigreeMetaData =>
        pedigreeService.createOrUpdatePedigreeMetadata(pedigreeMetaData).map {
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
          case Left(error) => BadRequest(Json.toJson(error))
        }
    )
  }

  def createPedigree: Action[JsValue] = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    request.body.validate[PedigreeDataCreation].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      pedigreeData =>
        pedigreeService.createPedigree(pedigreeData, userId, pedigreeData.copiedFrom).map {
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
          case Left(error) => BadRequest(Json.toJson(error))
        }
    )
  }

  def collapse: Action[JsValue] = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    request.body.validate[CollapseRequest].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      req =>
        pedigreeService.collapse(req.courtCaseId, userId)
        Future.successful(Ok(Json.toJson(req.courtCaseId)).withHeaders("X-CREATED-ID" -> req.courtCaseId.toString))
    )
  }

  def changeCourtCaseStatus(id: Long, status: String, closeProfiles: Boolean): Action[JsValue] = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      try
        val pedigreeStatus = PedigreeStatus.withName(status)
        pedigreeStatus match
          case PedigreeStatus.Deleted => deleteCourtCase(id, userId, isSuperUser, closeProfiles)
          case PedigreeStatus.Closed => closeCourtCase(id, userId, isSuperUser, closeProfiles)
          case _ => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Estado inválido")))
      catch
        case _: NoSuchElementException =>
          Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Estado inválido")))
    }
  }

  private def deleteCourtCase(idCourtCase: Long, userId: String, isSuperUser: Boolean, closeProfiles: Boolean) =
    pedigreeService.changeCourtCaseStatus(idCourtCase, PedigreeStatus.Deleted, userId, isSuperUser).flatMap {
      case Right(id) =>
        pedigreeService.closeAllPedigrees(idCourtCase, userId).map { _ =>
          if closeProfiles then closeAllProfiles(idCourtCase, userId)
          Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
        }
      case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
    }

  private def closeCourtCase(idCourtCase: Long, userId: String, isSuperUser: Boolean, closeProfiles: Boolean) =
    pedigreeService.changeCourtCaseStatus(idCourtCase, PedigreeStatus.Closed, userId, isSuperUser).flatMap {
      case Right(id) =>
        pedigreeService.closeAllPedigrees(idCourtCase, userId)
        if closeProfiles then closeAllProfiles(idCourtCase, userId)
        Future.successful(Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString))
      case Left(error) => Future.successful(BadRequest(Json.toJson(error)))
    }

  private def closeAllProfiles(idCourtCase: Long, userId: String): Future[Unit] =
    val deletedMotive = DeletedMotive(userId, "Baja por cierre de caso", 2)
    pedigreeService.getProfilesToDelete(idCourtCase).flatMap { referenceProfiles =>
      Future.sequence(
        referenceProfiles.map(globalCode =>
          profileDataService.deleteProfile(globalCode, deletedMotive, userId)
        )
      ).map(_ => ()).recoverWith { case _: Exception => Future.successful(()) }
    }

  def canDeleteCourtCase(courtCaseId: Long): Action[AnyContent] = Action.async {
    pedigreeService.hasPedigreeMatches(courtCaseId).map(deleteable => Ok(Json.toJson(!deleteable)))
  }

  def canCloseCourtCase(courtCaseId: Long): Action[AnyContent] = Action.async {
    pedigreeService.hasPendingPedigreeMatches(courtCaseId).map(closeable => Ok(Json.toJson(!closeable)))
  }

  def confirmSelectedCollapsing(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CollapseRequest].fold(
      errors => Future.successful(BadRequest(Json.obj("message" -> JsError.toJson(errors)))),
      collapsingRequest =>
        pedigreeService.collapseGroup(collapsingRequest).map {
          case Right(_) => Ok(Json.toJson(collapsingRequest.globalCodeParent)).withHeaders("X-CREATED-ID" -> collapsingRequest.globalCodeParent)
          case Left(error) => BadRequest(Json.toJson(error))
        }
    )
  }

  def disassociateGroupedProfiles(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[List[CaseProfileAdd]].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      courtCaseProfiles =>
        pedigreeService.disassociateGroupedProfiles(courtCaseProfiles).map {
          case Left(error) => BadRequest(Json.toJson(error))
          case Right(_) => Ok
        }
    )
  }

  def doesntHaveActivePedigrees(idCourtCase: Long): Action[AnyContent] = Action.async {
    pedigreeService.doesntHaveActivePedigrees(idCourtCase).map(result => Ok(Json.toJson(result)))
  }

  def areAssignedToPedigree(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CollapseRequest].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      courtCaseProfiles =>
        pedigreeService.areAssignedToPedigree(courtCaseProfiles.globalCodeChildren, courtCaseProfiles.courtCaseId).map {
          case Left(e) => BadRequest(Json.obj("message" -> e))
          case Right(_) => Ok
        }
    )
  }

  def getProfilesInactive(id: Long, pageSize: Int, page: Int, input: Option[String], statusProfile: String, groupedBy: String): Action[AnyContent] = Action.async {
    val statusP = CollapsingStatus.withName(statusProfile)
    pedigreeService.getProfilesInactive(CourtCasePedigreeSearch(page, pageSize, id, input, None, None, None, Some(statusP), Some(groupedBy))).map(result => Ok(Json.toJson(result)))
  }

  def getTotalProfilesInactive(id: Long, pageSize: Int, page: Int, input: Option[String], statusProfile: String, groupedBy: String): Action[AnyContent] = Action.async {
    val statusP = CollapsingStatus.withName(statusProfile)
    pedigreeService.getTotalProfilesInactive(CourtCasePedigreeSearch(page, pageSize, id, input, None, None, None, Some(statusP), Some(groupedBy))).map(result => Ok(Json.toJson(result)))
  }

  def getPedCheck(pedigreeId: Long, idCourtCase: Long): Action[AnyContent] = Action.async {
    pedCheckService.getPedCheck(pedigreeId).map(x => Ok(Json.toJson(x)))
  }

  def generatePedCheck(pedigreeId: Long, idCourtCase: Long): Action[AnyContent] = Action.async {
    pedCheckService.cleanConsistency(pedigreeId).map {
      case Right(x) => Ok(Json.toJson(x.toString))
      case Left(error) => BadRequest(Json.toJson(error))
    }
  }
