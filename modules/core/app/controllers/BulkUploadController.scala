package controllers

import bulkupload.{BulkUploadService, ProtoProfileMatchingQuality, ProtoProfileStatus}
import jakarta.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import search.PaginationSearch
import services.UserService
import types.AlphanumericId

import java.nio.file.Files
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BulkUploadController @Inject()(
    cc: ControllerComponents,
    bulkUploadService: BulkUploadService,
    userService: UserService
)(implicit ec: ExecutionContext) extends AbstractController(cc):

  def getBatchesStep1(page: Int, pageSize: Int): Action[AnyContent] = Action.async { request =>
    val userId = request.headers.get("X-USER").get
    val offset = (page - 1) * pageSize
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      bulkUploadService.getBatchesStep1(userId, isSuperUser, offset, pageSize).map(batches => Ok(Json.toJson(batches)))
    }
  }

  def countBatchesStep1: Action[AnyContent] = Action.async { request =>
    val userId = request.headers.get("X-USER").get
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      bulkUploadService.countBatchesStep1(userId, isSuperUser).map(total => Ok(Json.obj("total" -> total)))
    }
  }

  def getBatchesStep2(geneMapperId: String, page: Int, pageSize: Int): Action[AnyContent] = Action.async { request =>
    val userId = request.headers.get("X-USER").get
    val offset = (page - 1) * pageSize
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      bulkUploadService.getBatchesStep2(userId, geneMapperId, isSuperUser, offset, pageSize).map(batch => Ok(Json.toJson(batch)))
    }
  }

  def countBatchesStep2(geneMapperId: String): Action[AnyContent] = Action.async { request =>
    val userId = request.headers.get("X-USER").get
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      bulkUploadService.countBatchesStep2(userId, geneMapperId, isSuperUser).map(total => Ok(Json.obj("total" -> total)))
    }
  }

  def countAllProtoProfilesInBatch(batchId: Long): Action[AnyContent] = Action.async {
    bulkUploadService.countAllProtoProfilesInBatch(batchId).map(total => Ok(Json.obj("total" -> total)))
  }

  def getProtoProfileById(id: Long): Action[AnyContent] = Action.async {
    bulkUploadService.getProtoProfileWithBatchId(id).map {
      _.fold(NotFound(id.toString)) { pp =>
        val jpp = Json.toJson(pp._1).as[play.api.libs.json.JsObject]
        Ok(jpp ++ Json.obj("batchId" -> pp._2))
      }
    }
  }

  def getProtoProfilesStep1(batchId: Long, page: Int, pageSize: Int): Action[AnyContent] = Action.async {
    bulkUploadService.getProtoProfilesStep1(batchId, Some(PaginationSearch(page, pageSize))).map(profiles => Ok(Json.toJson(profiles)))
  }

  def getProtoProfilesStep2(geneMapperId: String, batchId: Long, page: Int, pageSize: Int): Action[AnyContent] = Action.async {
    userService.isSuperUserByGeneMapper(geneMapperId).flatMap { isSuperUser =>
      bulkUploadService.getProtoProfilesStep2(batchId, geneMapperId, isSuperUser, Some(PaginationSearch(page, pageSize))).map(profiles => Ok(Json.toJson(profiles)))
    }
  }

  def uploadProtoProfiles(label: Option[String], analysisType: String) = Action.async(parse.multipartFormData) { request =>
    val user = request.session("X-USER")
    request.body.file("file").fold(Future.successful(NotFound("Missing file"))) { csvFile =>
      val fileType = Files.probeContentType(csvFile.ref.file.toPath)
      if fileType == "text/plain" then
        bulkUploadService.uploadProtoProfiles(user, csvFile.ref, label, analysisType).map {
          _.fold(
            error   => BadRequest(Json.obj("message" -> error)),
            batchId => Ok(Json.toJson(batchId)).withHeaders("X-CREATED-ID" -> batchId.toString)
          )
        }
      else
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "Bad File Type")))
    }
  }

  def rejectProtoProfile(id: Long, motive: String, idMotive: Long): Action[AnyContent] = Action.async { request =>
    val userId = request.headers.get("X-USER").get
    bulkUploadService.rejectProtoProfile(id, motive, userId, idMotive).map(errors => Ok(Json.toJson(errors)))
  }

  def updateProtoProfileStatus(id: Long, status: String, replicate: Boolean, desktopSearch: Boolean = false): Action[AnyContent] = Action.async { request =>
    val userId = request.headers.get("X-USER").get
    bulkUploadService.updateProtoProfileStatus(id, ProtoProfileStatus.withName(status), userId, replicate, desktopSearch).map(errors => Ok(Json.toJson(errors)))
  }

  def updateProtoProfileData(id: Long, category: AlphanumericId): Action[AnyContent] = Action.async { request =>
    val userId = request.headers.get("X-USER").get
    bulkUploadService.updateProtoProfileData(id, category, userId).map {
      case Left(e)   => BadRequest(Json.obj("message" -> e))
      case Right(pp) => Ok(Json.toJson(pp))
    }
  }

  def updateBatchStatus(idBatch: Long, status: String, replicateAll: Boolean): Action[JsValue] = Action.async(parse.json) { request =>
    val idsToReplicate = request.body.validate[List[Long]].getOrElse(Nil)
    val userId = request.headers.get("X-USER").get
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      bulkUploadService.updateBatchStatus(idBatch, ProtoProfileStatus.withName(status), userId, isSuperUser, replicateAll, idsToReplicate).map {
        case Right(id)    => Ok(Json.toJson(id))
        case Left(error)  => BadRequest(Json.toJson(error))
      }
    }
  }

  def updateProtoProfileRulesMismatch(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[ProtoProfileMatchingQuality].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      ppq    =>
        bulkUploadService.updateProtoProfileRulesMismatch(ppq.id, ppq.matchingRules, ppq.mismatches).map {
          case false => Ok(Json.toJson("error"))
          case true  => Ok(Json.toJson(ppq.id)).withHeaders("X-CREATED-ID" -> ppq.id.toString)
        }
    )
  }

  def deleteBatch(id: Long): Action[AnyContent] = Action.async {
    bulkUploadService.deleteBatch(id).map {
      case Left(e)   => BadRequest(Json.obj("message" -> e))
      case Right(pp) => Ok(Json.toJson(pp)).withHeaders("X-CREATED-ID" -> id.toString)
    }
  }

  def searchBatch(filter: String): Action[AnyContent] = Action.async { request =>
    val userId = request.headers.get("X-USER").get
    userService.isSuperUser(userId).flatMap { isSuperUser =>
      bulkUploadService.searchBatch(userId, isSuperUser, filter).map(batch => Ok(Json.toJson(batch)))
    }
  }

  def getBatchSearchModalViewByIdOrLabel(input: String, idCase: Long): Action[AnyContent] = Action.async {
    bulkUploadService.getBatchSearchModalViewByIdOrLabel(input, idCase).map(result => Ok(Json.toJson(result)))
  }
