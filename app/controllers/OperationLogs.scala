package controllers

import javax.inject.{Inject, Singleton}

import audit.{OperationLogSearch, OperationLogService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, Json}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.Future

@Singleton
class OperationLogs @Inject() (opLogService: OperationLogService) extends Controller {

  def getLogLots(page: Int, pageSize: Int) = Action.async {
    opLogService.listLotsView(page, pageSize) map { ops =>
      Ok(Json.toJson(ops))
    }
  }

  def getTotalLots = Action.async {
    opLogService.getLotsLength map { size =>
      Ok("").withHeaders("X-LOTS-LENGTH" -> size.toString)
    }
  }

  def getTotalLogs = Action.async(BodyParsers.parse.json) { request =>
    val search = request.body.validate[OperationLogSearch]
    search.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        opLogService.getLogsLength(input) map { size =>
          Ok("").withHeaders("X-LOGS-LENGTH" -> size.toString)
        }
      })

  }

  def searchLogs = Action.async(BodyParsers.parse.json) { request =>
    val search = request.body.validate[OperationLogSearch]
    search.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        opLogService.searchLogs(input) map { ops =>
          Ok(Json.toJson(ops))
        }
      })
  }

  def checkLogLot(id: Long) = Action.async {
    opLogService.checkLot(id) map { result =>
      result.fold({
        case (signedEntry, expectedSignature) => Ok(Json.obj(
          "signedEntry" -> signedEntry,
          "expectedSignature" -> expectedSignature.asHexaString()))
      }, {
        _ => Ok
      })
    }
  }

}
