package controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.{JsError, Json}
import play.api.mvc.*
import matching.{MatchingCalculatorService, MongoId}
import profile.ProfileService
import scenarios.*
import services.UserService
import types.{SampleCode, StatOption}

@Singleton
class ScenariosController @Inject()(
  cc: ControllerComponents,
  scenarioService: ScenarioService,
  calculatorService: MatchingCalculatorService,
  profileService: ProfileService,
  userService: UserService
)(using ec: ExecutionContext) extends AbstractController(cc):

  def getDefaultScenario(firingProfile: SampleCode, matchingProfile: SampleCode) =
    Action.async(parse.json) { request =>
      request.body.validate[StatOption].fold(
        errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
        stats =>
          for
            firingOpt   <- profileService.findByCode(firingProfile)
            matchingOpt <- profileService.findByCode(matchingProfile)
          yield (firingOpt, matchingOpt) match
            case (Some(f), Some(m)) =>
              val scenario = calculatorService.createDefaultScenario(f, m, stats)
              Ok(Json.toJson(scenario))
            case _ => BadRequest(Json.obj("status" -> "KO", "message" -> "Profiles not found"))
      )
    }

  def findMatches(scenarioId: Option[String], firingCode: Option[SampleCode], matchingCode: Option[SampleCode]) =
    Action.async { request =>
      val userId = request.headers.get("X-USER").getOrElse("")
      userService.isSuperUser(userId).flatMap { isSuperUser =>
        val resultFut = scenarioId match
          case Some(sid) =>
            scenarioService.get(userId, MongoId(sid), isSuperUser).flatMap {
              case Some(scenario) => scenarioService.findExistingMatches(scenario)
              case None           => Future.successful(Seq.empty)
            }
          case None =>
            (firingCode, matchingCode) match
              case (Some(fc), Some(mc)) =>
                scenarioService.findMatchesForRestrainedScenario(userId, fc, mc, isSuperUser)
              case (Some(fc), None) =>
                scenarioService.findMatchesForScenario(userId, fc, isSuperUser)
              case _ =>
                Future.successful(Seq.empty)
        resultFut.map(opts => Ok(Json.toJson(opts)))
      }
    }

  def search = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    request.body.validate[ScenarioSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input =>
        userService.isSuperUser(userId).flatMap { isSuperUser =>
          scenarioService.search(userId, input, isSuperUser).map(ops => Ok(Json.toJson(ops)))
        }
    )
  }

  def create = Action.async(parse.json) { request =>
    request.body.validate[Scenario].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      scenario => scenarioService.add(scenario).map {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
        case Left(err) => BadRequest(Json.toJson(err))
      }
    )
  }

  def delete = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    request.body.validate[MongoId].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      id =>
        userService.isSuperUser(userId).flatMap { isSuperUser =>
          scenarioService.delete(userId, id, isSuperUser).map {
            case Right(rid) => Ok(Json.toJson(rid)).withHeaders("X-CREATED-ID" -> rid)
            case Left(err)  => BadRequest(Json.toJson(err))
          }
        }
    )
  }

  def get = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    request.body.validate[MongoId].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      id =>
        userService.isSuperUser(userId).flatMap { isSuperUser =>
          scenarioService.get(userId, id, isSuperUser).map {
            case Some(scenario) => Ok(Json.toJson(scenario))
            case None           => NotFound
          }
        }
    )
  }

  def validate = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    request.body.validate[Scenario].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      scenario =>
        userService.isSuperUser(userId).flatMap { isSuperUser =>
          scenarioService.validate(userId, scenario, isSuperUser).map {
            case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
            case Left(err) => BadRequest(Json.obj("message" -> err))
          }
        }
    )
  }

  def update = Action.async(parse.json) { request =>
    val userId = request.headers.get("X-USER").getOrElse("")
    request.body.validate[Scenario].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      scenario =>
        userService.isSuperUser(userId).flatMap { isSuperUser =>
          scenarioService.update(userId, scenario, isSuperUser).map {
            case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
            case Left(err) => BadRequest(Json.toJson(err))
          }
        }
    )
  }

  def getNCorrection = Action.async(parse.json) { request =>
    request.body.validate[NCorrectionRequest].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      correctionRequest =>
        scenarioService.getNCorrection(correctionRequest).map {
          case Right(response) => Ok(Json.toJson(response))
          case Left(error)     => BadRequest(Json.arr(Json.obj("status" -> "KO", "message" -> error)))
        }
    )
  }
