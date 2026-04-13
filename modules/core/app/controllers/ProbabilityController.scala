package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.{Json, JsError}
import probability.{FullCalculationScenario, LRMixCalculator, ProbabilityService, PValueCalculator}
import stats.PopulationBaseFrequencyService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProbabilityController @Inject()(
  cc: ControllerComponents,
  probabilityService: ProbabilityService,
  populationBaseFrequencyService: PopulationBaseFrequencyService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  /** POST /api/v2/lr-mix
   *  Body: FullCalculationScenario JSON (stats.frequencyTable = population base frequency name)
   *  Returns: LRResult JSON
   */
  def calculateLRMix: Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson match {
      case None =>
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Expecting JSON body")))
      case Some(json) =>
        json.validate[FullCalculationScenario].fold(
          errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
          scenario =>
            populationBaseFrequencyService.getByName(scenario.stats.frequencyTable).flatMap {
              case None =>
                Future.successful(NotFound(Json.obj("error" -> s"Frequency table '${scenario.stats.frequencyTable}' not found")))
              case Some(popFreq) =>
                val frequencyTable = PValueCalculator.parseFrequencyTable(popFreq)
                LRMixCalculator.calculateLRMix(scenario, frequencyTable).map(lr => Ok(Json.toJson(lr)))
            }
        )
    }
  }

  /** GET /api/v2/probability/stats/:laboratory
   *  Returns the StatOption (frequency table name, model, theta, dropIn, dropOut)
   *  populated from default population base frequency + laboratory settings.
   */
  def getStats(laboratory: String): Action[AnyContent] = Action.async { implicit request =>
    probabilityService.getStats(laboratory).map {
      case Some(stats) => Ok(Json.toJson(stats))
      case None        => NotFound(Json.obj("error" -> s"No stats found for laboratory: $laboratory"))
    }
  }
}
