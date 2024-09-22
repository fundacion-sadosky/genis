package controllers

import javax.inject.{Inject, Singleton}
import matching.{MatchingCalculatorService, MatchingService}
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import play.api.libs.json.{JsArray, JsError, JsValue, Json}
import play.api.mvc.{Action, BodyParsers, Controller, Results}
import scenarios._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import profile.ProfileService
import scenarios.ScenarioStatus._
import types.{MongoId, SampleCode, StatOption}
import user.UserService
import util.JsValidationError

import scala.concurrent.Future

@Singleton
class Scenarios @Inject()(
   scenarioService: ScenarioService,
   matchingService: MatchingService,
   matchingCalculator: MatchingCalculatorService,
   profileService: ProfileService,
   userService: UserService = null) extends Controller {

  def getDefaultScenario(firingProfile: SampleCode, matchingProfile: SampleCode) = Action.async(BodyParsers.parse.json) { request =>
    val statsOpt = request.body.validate[StatOption]
    statsOpt.fold(errors => {
      Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
    }, stats => {
      val scenarioFut = for {
        profile <- profileService.findByCode(firingProfile)
        matcher <- profileService.findByCode(matchingProfile)
      } yield matchingCalculator.createDefaultScenario(profile.get, matcher.get, stats)
      scenarioFut map { scenario => Ok(Json.toJson(scenario)) }
    })
  }

  def findMatches(scenarioId: Option[String], firingCode: Option[SampleCode], matchingCode: Option[SampleCode]) = Action.async { request =>
    val userId = request.headers.get("X-USER").get

    userService.isSuperUser(userId).flatMap(isSuperUser => {
      val options =
        if (scenarioId.isDefined) {
          scenarioService.get(userId, MongoId(scenarioId.get), isSuperUser).flatMap {
            case Some(scenario) => scenarioService.findExistingMatches(scenario)
            case None => Future.successful(Seq())
          }

        } else if (firingCode.isDefined && matchingCode.isDefined) {
          scenarioService.findMatchesForRestrainedScenario(userId, firingCode.get, matchingCode.get, isSuperUser)
        } else {
          scenarioService.findMatchesForScenario(userId, firingCode.get, isSuperUser)
        }

      options map {
        case opts => Ok(Json.toJson(opts))
      }
    })
  }

  def calculateLRMix = Action.async(BodyParsers.parse.json) { request =>
    val scenario = request.body.validate[CalculationScenario]

    scenario.fold(errors => {
      Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
    }, s => {
      val result = scenarioService.getLRMix(s)
      val f = result map { lr =>
        Ok(Json.toJson(lr))
      }
      f
    })
  }


  def search = Action.async(BodyParsers.parse.json) { request =>
    val userId = request.headers.get("X-USER").get
    val search = request.body.validate[ScenarioSearch]

    search.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        userService.isSuperUser(userId).flatMap(isSuperUser => {
          scenarioService.search(userId, input, isSuperUser) map { ops =>
            Ok(Json.toJson(ops))
          }
        })
      })
  }

  def create = Action.async(BodyParsers.parse.json) { request =>
    val s = request.body.validate[Scenario]

    s.fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      scenario => scenarioService.add(scenario).map {
        case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
        case Left(error) => BadRequest(Json.toJson(error))
      }
    )
  }

  def delete = Action.async(BodyParsers.parse.json) { request =>
    val userId = request.headers.get("X-USER").get
    val s = request.body.validate[MongoId]

    s.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
      },
      id => {
        userService.isSuperUser(userId).flatMap(isSuperUser => {
          scenarioService.delete(userId, id, isSuperUser).map {
            case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
            case Left(error) => BadRequest(Json.toJson(error))
          }
        })
      }
    )
  }

  def get = Action.async(BodyParsers.parse.json) { request =>
    val userId = request.headers.get("X-USER").get
    val s = request.body.validate[MongoId]

    s.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
      },
      id => {
        userService.isSuperUser(userId).flatMap(isSuperUser => {
          scenarioService.get(userId, id, isSuperUser).map {
            case Some(scenario) => Ok(Json.toJson(scenario))
            case None => Results.NotFound
          }
        })
      }
    )
  }

  def validate = Action.async(BodyParsers.parse.json) { request =>
    val userId = request.headers.get("X-USER").get
    val s = request.body.validate[Scenario]

    s.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
      },
      scenario => {
        userService.isSuperUser(userId).flatMap(isSuperUser => {
          (for {
            scenarioUpdated <- scenarioService.validate(userId, scenario, isSuperUser)
            scenarioId <- matchingService.validate(scenario)
          } yield (scenarioUpdated, scenarioId)).map {
            case (Right(id), Right(_)) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
            case (Left(e), Left(error2)) => {
              val error = e + " " + error2
              BadRequest(Json.obj("message" -> error))
            }
            case (Left(error), Right(_)) => BadRequest(Json.obj("message" -> error))
            case (Right(id), Left(error)) => BadRequest(Json.obj("id" -> id, "message" -> error))
          }
        })
      }
    )
  }

  def update = Action.async(BodyParsers.parse.json) { request =>
    val userId = request.headers.get("X-USER").get
    val s = request.body.validate[Scenario]

    s.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
      },
      scenario => {
        userService.isSuperUser(userId).flatMap(isSuperUser => {
          scenarioService.update(userId, scenario, isSuperUser).map {
            case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id)
            case Left(error) => BadRequest(Json.toJson(error))
          }
        })
      }
    )
  }

  def getNCorrection: Action[JsValue] =
    Action
      .async(BodyParsers.parse.json) {
        request =>
          val input = request.body.validate[NCorrectionRequest]
          input.fold(
            errors => {
              val jsErrors = JsValidationError
                .toJsonResult(errors)
                .map { case (code, args) => Messages(code, args: _*) }
                .map( error => Json.obj( "status" -> "KO", "message" -> error ) )
              Future
                .successful( BadRequest(JsArray(jsErrors)) )
            },
            correctionRequest => scenarioService
              .getNCorrection(correctionRequest)
              .map {
                case Right(response) => Ok(Json.toJson(response))
                case Left(error) => BadRequest(
                  Json.arr( Json.obj( "status" -> "KO", "message" -> error ) )
                )
              }
          )
  }

}