package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import configdata.BioMaterialTypeService
import scala.concurrent.Future
import types.DataAccessException
import play.api.Logger

@Singleton
class BioMaterialTypes @Inject() (bioMaterialTypeService: BioMaterialTypeService) extends Controller with JsonSimpleActions with JsonSqlActions {

  val logger: Logger = Logger(this.getClass())
  
  def list = Action.async {
    bioMaterialTypeService.list map {
      bioMaterials => Ok(Json.toJson(bioMaterials))
    }
  }

  def insert = JsonSqlAction {
    bioMaterialTypeService.insert
  }

  def update = JsonSimpleAction {
    bioMaterialTypeService.update
  }

  def remove(bmtId: String) = Action.async {

    bioMaterialTypeService.delete(bmtId)
      .map { res => Ok(Json.obj("deletes" -> res)) }
      .recover {
        case dae: DataAccessException => {
          logger.error(dae.msg, dae.error)
          BadRequest(Json.obj("error" -> dae.msg))
        }
      }

  }
}
