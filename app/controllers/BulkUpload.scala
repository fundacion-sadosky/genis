package controllers

import java.nio.file.Files

import scala.concurrent.Future
import javax.inject.Inject
import javax.inject.Singleton

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.BodyParsers
import play.api.libs.json.JsError
import bulkupload.ProtoProfileMatchingQuality
import bulkupload.ProtoProfileStatus
import bulkupload.BulkUploadService
import configdata.MatchingRule
import profile.Profile
import search.PaginationSearch
import types.AlphanumericId
import user.UserService

import scala.util.{Left, Right}

@Singleton
class BulkUpload @Inject() (bulkUploadService: BulkUploadService, userService: UserService) extends Controller with JsonActions {

  def getBatchesStep1 = Action.async { request =>
    val userId = request.headers.get("X-USER").get
    userService.isSuperUser(userId).flatMap(isSuperUser => {
      bulkUploadService.getBatchesStep1(userId, isSuperUser) map { batch =>
        Ok(Json.toJson(batch))
      }
    })
  }

  def getBatchesStep2 (geneMapperId : String) = Action.async { request =>
    val userId = request.headers.get("X-USER").get

    userService.isSuperUser(userId).flatMap(isSuperUser => {
      bulkUploadService.getBatchesStep2(userId, geneMapperId, isSuperUser) map { batch =>
        Ok(Json.toJson(batch))
      }
    })
  }

  def getProtoProfileById(id: Long) = Action.async { request =>
    bulkUploadService.getProtoProfileWithBatchId(id) map { error =>
      error.fold({
        NotFound(id.toString)
      })({ pp =>
        val jpp = Json.toJson(pp._1)
        val ss = jpp.as[JsObject] ++ Json.obj("batchId" -> pp._2)
        Ok(Json.toJson(ss))
      })
    }
  }

  def getProtoProfilesStep1(batchId: Long, page: Int, pageSize: Int) = Action.async { request =>
    bulkUploadService.getProtoProfilesStep1(batchId, Some(PaginationSearch(page, pageSize))) map { profiles =>
      Ok(Json.toJson(profiles))
    }
  }

  def getProtoProfilesStep2(geneMapperId: String, batchId: Long, page: Int, pageSize: Int) = Action.async { request =>
    userService.isSuperUserByGeneMapper(geneMapperId).flatMap(isSuperUser => {
      bulkUploadService.getProtoProfilesStep2(batchId, geneMapperId, isSuperUser, Some(PaginationSearch(page, pageSize))).map { profiles =>
        Ok(Json.toJson(profiles))
      }
    })
  }

  def uploadProtoProfiles(label : Option[String], analysisType: String) = Action.async(parse.multipartFormData) { request =>
    val user = request.session("X-USER")
    val optionFile = request.body.file("file")

    optionFile.fold(Future.successful(NotFound("Missing file")))({ csvFile =>

      val fileType = Files.probeContentType(csvFile.ref.file.toPath)

      if (fileType == "text/plain") {
        bulkUploadService.uploadProtoProfiles(user, csvFile.ref, label, analysisType ).map { batchId =>
          batchId.fold(error => {
            BadRequest(Json.obj("message" -> error))
          }, batchId => {
            Ok(Json.toJson(batchId)).withHeaders("X-CREATED-ID" -> batchId.toString())
          })
        }
      } else {
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Bad File Type")))
      }
    })
  }

  def rejectProtoProfile(id: Long, motive: String,idMotive:Long) = Action.async { request =>
    val userId = request.headers.get("X-USER").get
    bulkUploadService.rejectProtoProfile(id, motive, userId,idMotive) map {errors => Ok(Json.toJson(errors)) }
  }
  
  def updateProtoProfileStatus(id: Long, status: String, replicate:Boolean) = Action.async { request =>
    val userId = request.headers.get("X-USER").get
    bulkUploadService.updateProtoProfileStatus(id, ProtoProfileStatus.withName(status), userId, replicate) map { errors =>
      Ok(Json.toJson(errors))
    }
  }

  def updateProtoProfileData(id: Long, category: AlphanumericId) = Action.async { request =>
    val userId = request.headers.get("X-USER").get
    bulkUploadService.updateProtoProfileData(id, category, userId) map {
      case Left(e) => BadRequest(Json.obj("message" -> e))
      case Right(pp) => Ok(Json.toJson(pp))
    }
  }
  def updateBatchStatus(idBatch: Long, status: String,replicateAll:Boolean) = Action.async(BodyParsers.parse.json) { request =>
    val idsToReplicate = request.body.validate[List[Long]].getOrElse(Nil)

    val userId = request.headers.get("X-USER").get

    userService.isSuperUser(userId).flatMap(isSuperUser => {
      bulkUploadService.updateBatchStatus(idBatch, ProtoProfileStatus.withName(status), userId, isSuperUser, replicateAll, idsToReplicate) map {
        case Right(id) => Ok(Json.toJson(id))
        case Left(error) => BadRequest(Json.toJson(error))
      }
    })
  }

  def updateProtoProfileRulesMismatch() = Action.async(BodyParsers.parse.json) { request =>
    val hypothesisJs = request.body.validate[ProtoProfileMatchingQuality]

    hypothesisJs.fold(errors =>
      {
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
      },
      ppq => {
        println(ppq)
        bulkUploadService.updateProtoProfileRulesMismatch(ppq.id, ppq.matchingRules, ppq.mismatches) map { success =>
          success match {
            case false => Ok(Json.toJson("error"))
            case true  => Ok(Json.toJson(ppq.id)).withHeaders("X-CREATED-ID" -> ppq.id.toString())
          }
        }
      })
  }

  def deleteBatch(id:Long) = Action.async { request =>
    bulkUploadService.deleteBatch(id) map {
      case Left(e) => BadRequest(Json.obj("message" -> e))
      case Right(pp) => Ok(Json.toJson(pp)).withHeaders("X-CREATED-ID" -> id.toString)
    }
  }

  def searchBatch(filter: String)= Action.async { request =>
    val userId = request.headers.get("X-USER").get

    userService.isSuperUser(userId).flatMap(isSuperUser => {
      bulkUploadService.searchBatch(userId, isSuperUser, filter) map { batch =>
        Ok(Json.toJson(batch))
      }
    })
  }

  def getBatchSearchModalViewByIdOrLabel(input:String,idCase:Long) = Action.async { request =>

    bulkUploadService.getBatchSearchModalViewByIdOrLabel(input,idCase) map {
      case result => Ok(Json.toJson(result))
    }
  }

}
