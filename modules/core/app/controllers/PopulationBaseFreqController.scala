package controllers

import javax.inject.{Inject, Singleton}
import java.nio.file.Files
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import play.api.libs.json.{Json, JsError}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import stats.{PopulationBaseFrequencyService, Fmins}
import stats.ProbabilityModel

@Singleton
class PopulationBaseFreqController @Inject()(
  val controllerComponents: ControllerComponents,
  populationBaseFrequencyService: PopulationBaseFrequencyService
)(implicit ec: ExecutionContext) extends BaseController:

  private val logger: Logger = Logger(this.getClass)

  /** GET /api/v2/populationBaseFreq — lista todos los nombres de bases */
  def getAllBaseNames: Action[AnyContent] = Action.async {
    populationBaseFrequencyService.getAllNames().map(names => Ok(Json.toJson(names)))
  }

  /** GET /api/v2/populationBaseFreqCharacteristics — mapa name -> view */
  def getAllBasesCharacteristics: Action[AnyContent] = Action.async {
    populationBaseFrequencyService.getAllNames().map { names =>
      val map = names.map(bd => bd.name -> bd).toMap
      Ok(Json.toJson(map))
    }
  }

  /** GET /api/v2/populationBaseFreq/:name — datos por nombre */
  def getByName(name: String): Action[AnyContent] = Action.async {
    populationBaseFrequencyService.getByNamePV(name).map(data => Ok(Json.toJson(data)))
  }

  /** PUT /api/v2/populationBaseFreq/:name — activa / desactiva una base */
  def toggleStateBase(name: String): Action[AnyContent] = Action.async {
    populationBaseFrequencyService.toggleStateBase(name).map(data => Ok(Json.toJson(data)))
  }

  /** PUT /api/v2/populationBaseFreqDefault/:name — marca como default */
  def setBaseAsDefault(name: String): Action[AnyContent] = Action.async {
    populationBaseFrequencyService.setAsDefault(name).map(result => Ok(Json.toJson(result)))
  }

  /** PUT /api/v2/populationBaseFreq-fmin/:id — inserta los fmins calculados */
  def insertFmin(id: String): Action[play.api.libs.json.JsValue] =
    Action.async(parse.json) { request =>
      request.body.validate[Fmins].fold(
        err  => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(err)))),
        fmin => populationBaseFrequencyService.insertFmin(id, fmin).map(r => Ok(Json.toJson(r)))
      )
    }

  /**
   * POST /api/v2/populationBaseFreq — carga un archivo CSV de frecuencias.
   *
   * Partes del form: baseName (String), baseModel (String), baseTheta (Double),
   *                  file (CSV text/plain)
   */
  def uploadPopulationFile: Action[play.api.mvc.MultipartFormData[play.api.libs.Files.TemporaryFile]] =
    Action.async(parse.multipartFormData) { request =>

      val name  = request.body.dataParts.get("baseName").flatMap(_.headOption)
      val model = request.body.dataParts.get("baseModel").flatMap(_.headOption)
      val theta = request.body.dataParts.get("baseTheta").flatMap(_.headOption).flatMap { s =>
        try Some(s.toDouble) catch case _: NumberFormatException => None
      }

      if name.isEmpty || theta.isEmpty then
        val missing = Seq(
          if name.isEmpty  then " name"  else "",
          if theta.isEmpty then " theta" else ""
        ).mkString
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> s"Missing Parameters:$missing")))
      else
        request.body.file("file") match
          case None =>
            Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Missing File")))
          case Some(csvFile) =>
            val fileType = Files.probeContentType(csvFile.ref.path)
            if fileType == "text/plain" || fileType == "text/csv" then
              val probabilityModel = model
                .map(ProbabilityModel.withName)
                .getOrElse(ProbabilityModel.HardyWeinberg)
              populationBaseFrequencyService
                .parseFile(name.get, theta.get, probabilityModel, csvFile.ref.path.toFile)
                .map(r => Ok(Json.toJson(r)).withHeaders("X-CREATED-ID" -> name.get))
            else
              Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> s"Bad File Type: $fileType")))
    }
