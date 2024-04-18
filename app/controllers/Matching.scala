package controllers

import javax.inject.Inject
import matching._
import pedigree.PedigreeMatchesService
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, AnyContent, BodyParsers, Controller, Results}
import probability._
import profile.ProfileService
import profiledata.ProfileDataAttempt
import types.SampleCode
import user.UserService

import scala.concurrent.Future

class Matching @Inject()(
                          matchingService: MatchingService,
                          profileService: ProfileService,
                          calculatorService: MatchingCalculatorService,
                          probabilityService: ProbabilityService,
                          pedigreeMatchService: PedigreeMatchesService,
                          userService: UserService) extends Controller {

  val logger: Logger = Logger(this.getClass)

  def findMatchesByCode(globalCode: SampleCode) = Action.async { request =>
    matchingService.findMatchingResults(globalCode) map {
      case None => Results.NotFound
      case matching => Ok(Json.toJson(matching))
    }
  }

  def getTotalMatchesByGroup = Action.async(BodyParsers.parse.json) { request =>
    val matchGroupSearch = request.body.validate[MatchGroupSearch]

    matchGroupSearch.fold(errors => {
      Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
    }, search => {
      userService.isSuperUser(search.user).flatMap(isSuperUser => {
        val newSearch = new MatchGroupSearch(search.user, isSuperUser, search.globalCode, search.kind, search.page,
          search.pageSize, search.sortField, search.ascending, search.isCollapsing, search.courtCaseId, search.status, search.tipo)
        matchingService.getTotalMatchesByGroup(newSearch).map { size =>
          Ok("").withHeaders("X-MATCHES-GROUP-LENGTH" -> size.toString)
        }
      })
    })
  }

  def getMatchesByGroup = Action.async(BodyParsers.parse.json) { request =>
    val matchGroupSearch = request.body.validate[MatchGroupSearch]

    matchGroupSearch.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      search => {
        userService.isSuperUser(search.user).flatMap(isSuperUser => {
          val newSearch = new MatchGroupSearch(search.user, isSuperUser, search.globalCode, search.kind, search.page,
            search.pageSize, search.sortField, search.ascending, search.isCollapsing, search.courtCaseId, search.status, search.tipo)

          matchingService.getMatchesByGroup(newSearch) map { groups => Ok(Json.toJson(groups)) }
        })
      })
  }

  def getTotalMatches = Action.async(BodyParsers.parse.json) { request =>
    val matchCardSearch = request.body.validate[MatchCardSearch]

    matchCardSearch.fold(errors => {
      Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
    }, search => {
      userService.isSuperUser(search.user).flatMap(isSuperUser => {
        val newSearch = new MatchCardSearch(search.user, isSuperUser, search.page, search.pageSize, search.ascending, search.profile, search.hourFrom, search.hourUntil,
          search.status, search.laboratoryCode, search.isCollapsing, search.courtCaseId, search.categoria)
        matchingService.getTotalMatches(newSearch).map { size =>
          Ok("").withHeaders("X-MATCHES-LENGTH" -> size.toString)
        }
      })
    })
  }

  def getMatches = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[MatchCardSearch]

    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      search => {
        userService.isSuperUser(search.user).flatMap(isSuperUser => {
          val newSearch = new MatchCardSearch(search.user, isSuperUser, search.page, search.pageSize, search.ascending, search.profile, search.hourFrom, search.hourUntil,
            search.status, search.laboratoryCode, search.isCollapsing, search.courtCaseId, search.categoria)
          matchingService.getMatches(newSearch) map { groups => Ok(Json.toJson(groups)) }
        })
      })
  }

  def searchMatchesProfile(globalCode: String) = Action.async { request =>
    matchingService.searchMatchesProfile(globalCode) map { groups => Ok(Json.toJson(groups)) }
  }


  def convertDiscard(matchId: String, firingCode: SampleCode) = Action.async { request =>
    val userId = request.headers.get("X-USER").get

    userService.isSuperUser(userId).flatMap(isSuperUser => {
      matchingService.convertDiscard(matchId, firingCode, isSuperUser) map {
        case Right(result) => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString())
        case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
      }
    })
  }
  def uploadStatus(matchId: String, firingCode: types.SampleCode) = Action.async { request =>
    val userId = request.headers.get("X-USER").get

    userService.isSuperUser(userId).flatMap(isSuperUser => {
      matchingService.uploadStatus(matchId, firingCode, isSuperUser) map { message =>
        Ok(Json.toJson(message)).withHeaders("X-CREATED-ID" -> message.toString())
      }
    })
  }

  def canUploadMatchStatus(matchId: String) = Action.async { request =>
    matchingService.canUploadMatchStatus(matchId) map { uploadable =>
      Ok(Json.toJson(uploadable))
    }
  }

  //  def uploadStatus(matchId: String, firingCode: types.SampleCode) = play.mvc.Results.TODO

  def convertHit(matchId: String, firingCode: SampleCode) = Action.async { request =>

    matchingService.convertHit(matchId, firingCode) map {
      case Right(result) => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString())
      case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
    }
  }

  def getByMatchedProfileId(
    matchingId: String,
    isPedigreeMatch: Boolean,
    isCollapsing: Option[Boolean],
    isScreening: Option[Boolean]
  ): Action[AnyContent] = Action.async {
    request =>
      val matchFuture = if (!isPedigreeMatch) {
        matchingService.getByMatchedProfileId(
          matchingId,
          isCollapsing,
          isScreening
        )
      } else {
        pedigreeMatchService.getMatchById(matchingId)
      }
      matchFuture.map {
        case Some(m) => Ok(Json.toJson(m))
        case _ => Results.NotFound
      }
  }

  def getComparedMixtureGene(globalCodes: List[String], matchId: String, isCollapsing: Option[Boolean], isScreening: Option[Boolean]) = Action.async { request =>

    val globalCodesT = globalCodes map {
      SampleCode(_)
    }

    matchingService.getComparedMixtureGenotypification(globalCodesT, matchId, isCollapsing, isScreening) map { j => Ok(Json.toJson(j)) }
  }

  def getLR() = Action.async(BodyParsers.parse.json) { request =>
    val lrRequest = request.body.validate[LRRequest]

    lrRequest.fold(errors => {
      Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
    }, lrParams => {

      val statsFut =
        if (lrParams.stats.isEmpty) {
          probabilityService.getStats(lrParams.firingCode)
        } else {
          Future.successful(lrParams.stats)
        }

      val parametersFut = for {
        stats <- statsFut
        profile1 <- profileService.findByCode(lrParams.firingCode)
        profile2 <- profileService.findByCode(lrParams.matchingCode)
        matchResult <- matchingService.getMatchResultById(lrParams.matchingId)
      } yield (profile1, profile2, stats, matchResult)

      parametersFut flatMap { parameters =>
        if (parameters._3.isDefined) {
          val superiorProfileOpt = parameters._4.flatMap(_.superiorProfileInfo)
          calculatorService.getLRByAlgorithm(parameters._1.getOrElse(superiorProfileOpt.get.profile), parameters._2.getOrElse(superiorProfileOpt.get.profile), parameters._3.get, parameters._4.flatMap(_.result.allelesRanges)) map { lr =>
            Ok(Json.toJson(lr))
          }
        } else {
          Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "No default parameters configured")))
        }
      }

    })

  }

  def deleteByLeftProfile(globalCode: String, courtCaseId: Long) = Action.async {
    matchingService.discardCollapsingByLeftProfile(globalCode, courtCaseId).map(_ => {
      Ok(Json.toJson(globalCode)).withHeaders("X-CREATED-ID" -> globalCode)
    })
  }

  //  def deleteByLeftAndRightProfile(globalCode: String,courtCaseId:Long) = Action.async {
  //    matchingService.discardCollapsingByLeftAndRightProfile(globalCode,courtCaseId).map( _ => {
  //      Ok(Json.toJson(globalCode)).withHeaders("X-CREATED-ID" -> globalCode)
  //    })
  //  }

  def masiveDiscardByGlobalCode(firingCode: SampleCode) = Action.async { request =>
    val userId = request.headers.get("X-USER").get

    userService.isSuperUser(userId).flatMap(isSuperUser => {
      matchingService.masiveGroupDiscardByGlobalCode(firingCode, isSuperUser) map {
        case Right(result) => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString())
        case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
      }
    })
  }

  def masiveDiscardByMatchesList(firingCode: types.SampleCode, matches: List[String]) = Action.async { request =>
    val userId = request.headers.get("X-USER").get

    userService.isSuperUser(userId).flatMap(isSuperUser => {
      matchingService.masiveGroupDiscardByMatchesList(firingCode, matches, isSuperUser) map {
        case Right(result) => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString())
        case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
      }
    })
  }

}

