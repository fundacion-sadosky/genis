package controllers

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.{JsError, Json}
import play.api.mvc.{AbstractController, ControllerComponents}

import audit.{OperationLogSearch, OperationLogService}

@Singleton
class OperationLogsController @Inject()(
  cc: ControllerComponents,
  opLogService: OperationLogService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def getLogLots(page: Int, pageSize: Int) = Action.async {
    opLogService.listLotsView(page, pageSize).map { lots =>
      Ok(Json.toJson(lots))
    }
  }

  def getTotalLots = Action.async {
    opLogService.getLotsLength().map { size =>
      Ok("").withHeaders("X-LOTS-LENGTH" -> size.toString)
    }
  }

  def getTotalLogs = Action.async(parse.json) { request =>
    request.body.validate[OperationLogSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search => opLogService.getLogsLength(search).map { size =>
        Ok("").withHeaders("X-LOGS-LENGTH" -> size.toString)
      }
    )
  }

  def searchLogs = Action.async(parse.json) { request =>
    request.body.validate[OperationLogSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      search => opLogService.searchLogs(search).map { entries =>
        Ok(Json.toJson(entries))
      }
    )
  }

  def checkLogLot(id: Long) = Action.async {
    opLogService.checkLot(id).map {
      case Left((signedEntry, expectedSignature)) =>
        Ok(Json.obj(
          "signedEntry"       -> signedEntry,
          "expectedSignature" -> expectedSignature.asHexaString()
        ))
      case Right(_) => Ok
    }
  }
}
