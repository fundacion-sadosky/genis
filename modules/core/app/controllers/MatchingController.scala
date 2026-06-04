package controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.{JsError, Json}
import play.api.mvc.*
import matching.{LRRequest, MatchCardSearch, MatchGroupSearch, MatchingService}
import pedigree.PedigreeMatchesService
import profile.ProfileService
import services.UserService
import types.SampleCode

@Singleton
class MatchingController @Inject()(
  cc: ControllerComponents,
  matchingService: MatchingService,
  pedigreeMatchService: PedigreeMatchesService,
  profileService: ProfileService,
  userService: UserService
)(using ec: ExecutionContext) extends AbstractController(cc):

  def findMatchesByCode(globalCode: SampleCode) = Action.async { _ =>
    matchingService.findMatchingResults(globalCode).map {
      case None    => NotFound
      case Some(r) => Ok(Json.toJson(r))
    }
  }

  def getTotalMatchesByGroup = Action.async(parse.json) { request =>
    request.body.validate[MatchGroupSearch].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      search =>
        userService.isSuperUser(search.user).flatMap { isSuperUser =>
          val s = search.copy(isSuperUser = isSuperUser)
          matchingService.getTotalMatchesByGroup(s).map { size =>
            Ok("").withHeaders("X-MATCHES-GROUP-LENGTH" -> size.toString)
          }
        }
    )
  }

  def getMatchesByGroup = Action.async(parse.json) { request =>
    request.body.validate[MatchGroupSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search =>
        userService.isSuperUser(search.user).flatMap { isSuperUser =>
          val s = search.copy(isSuperUser = isSuperUser)
          matchingService.getMatchesByGroup(s).map(groups => Ok(Json.toJson(groups)))
        }
    )
  }

  def getTotalMatches = Action.async(parse.json) { request =>
    request.body.validate[MatchCardSearch].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      search =>
        userService.isSuperUser(search.user).flatMap { isSuperUser =>
          val s = search.copy(isSuperUser = isSuperUser)
          matchingService.getTotalMatches(s).map { size =>
            Ok("").withHeaders("X-MATCHES-LENGTH" -> size.toString)
          }
        }
    )
  }

  def getMatches = Action.async(parse.json) { request =>
    request.body.validate[MatchCardSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search =>
        userService.isSuperUser(search.user).flatMap { isSuperUser =>
          val s = search.copy(isSuperUser = isSuperUser)
          matchingService.getMatches(s).map(groups => Ok(Json.toJson(groups)))
        }
    )
  }

  def searchMatchesProfile(globalCode: String) = Action.async { _ =>
    matchingService.searchMatchesProfile(globalCode).map(groups => Ok(Json.toJson(groups)))
  }

  def convertDiscard(matchId: String, firingCode: SampleCode, userName: String) = Action.async { request =>
    val userId = request.headers.get("X-USER").getOrElse(userName)
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      matchingService.convertDiscard(matchId, firingCode, isSuperUser, replicate = true, userName).map {
        case Right(result) => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString)
        case Left(error)   => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
      }
    }
  }

  def uploadStatus(matchId: String, firingCode: SampleCode, userName: String) = Action.async { request =>
    val userId = request.headers.get("X-USER").getOrElse(userName)
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      matchingService.uploadStatus(matchId, firingCode, isSuperUser, userName).map { message =>
        Ok(Json.toJson(message)).withHeaders("X-CREATED-ID" -> message.toString)
      }
    }
  }

  def canUploadMatchStatus(matchId: String) = Action.async { _ =>
    matchingService.canUploadMatchStatus(matchId, None, None).map(r => Ok(Json.toJson(r)))
  }

  def convertHit(matchId: String, firingCode: SampleCode, userName: String) = Action.async { _ =>
    matchingService.convertHit(matchId, firingCode, replicate = true, userName).map {
      case Right(result) => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString)
      case Left(error)   => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
    }
  }

  def getByMatchedProfileId(
    matchingId: String,
    isPedigreeMatch: Boolean,
    isCollapsing: Option[Boolean],
    isScreening: Option[Boolean]
  ) = Action.async { _ =>
    val matchFuture =
      if isPedigreeMatch then pedigreeMatchService.getMatchById(matchingId)
      else matchingService.getByMatchedProfileId(matchingId, isCollapsing, isScreening)
    matchFuture.map {
      case Some(m) => Ok(Json.toJson(m))
      case None    => NotFound
    }
  }

  def getComparedMixtureGene(
    globalCodes: String,
    matchId: String,
    isCollapsing: Option[Boolean],
    isScreening: Option[Boolean]
  ) = Action.async { _ =>
    val codes = globalCodes.split(",").map(_.trim).filter(_.nonEmpty).map(SampleCode(_)).toList
    matchingService.getComparedMixtureGenotypification(codes, matchId, isCollapsing, isScreening)
      .map(j => Ok(Json.toJson(j)))
  }

  def getLR() = Action.async(parse.json) { _ =>
    // LR calculation service not yet migrated — return empty result
    Future.successful(Ok(Json.obj()))
  }

  def deleteByLeftProfile(globalCode: String, courtCaseId: Long) = Action.async { _ =>
    matchingService.discardCollapsingByLeftProfile(globalCode, courtCaseId).map { _ =>
      Ok(Json.toJson(globalCode)).withHeaders("X-CREATED-ID" -> globalCode)
    }
  }

  def masiveDiscardByGlobalCode(firingCode: SampleCode, userName: String) = Action.async { request =>
    val userId = request.headers.get("X-USER").getOrElse(userName)
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      matchingService.masiveGroupDiscardByGlobalCode(firingCode, isSuperUser, replicate = true, userName).map {
        case Right(result) => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString)
        case Left(error)   => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
      }
    }
  }

  def masiveDiscardByMatchesList(firingCode: SampleCode, matches: List[String], userName: String) = Action.async { request =>
    val userId = request.headers.get("X-USER").getOrElse(userName)
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      matchingService.masiveGroupDiscardByMatchesList(firingCode, matches, isSuperUser, replicate = true, userName).map {
        case Right(result) => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> result.toString)
        case Left(error)   => BadRequest(Json.obj("status" -> "KO", "message" -> Json.toJson(error)))
      }
    }
  }
