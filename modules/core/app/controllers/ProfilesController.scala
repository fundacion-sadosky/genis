package controllers

import javax.inject.{Inject, Singleton}
import configdata.{CategoryService, MatchingRule}
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsError, JsValue, Json, Writes}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import profile.*
import profile.GenotypificationByType.GenotypificationByType
import services.{CacheService, UploadedAnalysisKey}
import types.{AlphanumericId, SampleCode}

import java.nio.file.Files
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProfilesController @Inject()(
    categoryService: CategoryService,
    profileService: ProfileService,
    cache: CacheService,
    profileExportService: ProfileExporterService,
    limsArchivesExporterService: LimsArchivesExporterService,
    messagesApi: MessagesApi,
    cc: ControllerComponents
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private implicit val messages: Messages = messagesApi.preferred(Seq.empty)

  def create: Action[JsValue] = Action.async(parse.json) { request =>
    val input = request.body.validate[NewAnalysis]
    input.fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input =>
        profileService.create(input).map { outEither =>
          outEither.fold(
            error => BadRequest(Json.toJson(error)),
            success => Ok(Json.toJson(success)).withHeaders("X-CREATED-ID" -> success.globalCode.text)
          )
        }
    )
  }

  def findByCode(globalCode: SampleCode): Action[AnyContent] = Action.async {
    profileService.findByCode(globalCode).map {
      case Some(profile) => Ok(Json.toJson(profile))
      case _ => NotFound
    }
  }

  def findByCodes(globalCodes: List[SampleCode]): Action[AnyContent] = Action.async {
    profileService.findByCodes(globalCodes).map {
      case Seq() => NotFound
      case profiles => Ok(Json.toJson(profiles))
    }
  }

  def addElectropherograms(token: String, globalCode: SampleCode, idAnalysis: String, name: String): Action[AnyContent] = Action.async {
    profileService.saveElectropherograms(token, globalCode, idAnalysis, name).map { l =>
      val (right, left) = l.partition(_.isRight)
      if (left.isEmpty) Ok
      else Ok(Json.toJson(left.map(_.left.get)))
    }
  }

  def getElectropherogramsByCode(globalCode: SampleCode): Action[AnyContent] = Action.async {
    profileService.getElectropherogramsByCode(globalCode).map { lista =>
      Ok(Json.toJson(lista))
    }
  }

  def getElectropherogramImage(profileId: SampleCode, electropherogramId: String): Action[AnyContent] = Action.async {
    profileService.getElectropherogramImage(profileId, electropherogramId).map {
      case None => NotFound
      case Some(bytes) => Ok(bytes).as("application/octet-stream")
    }
  }

  def getElectropherogramsByAnalysisId(profileId: SampleCode, analysisId: String): Action[AnyContent] = Action.async {
    profileService.getElectropherogramsByAnalysisId(profileId, analysisId).map { listS =>
      Ok(Json.toJson(listS))
    }
  }

  def getFullProfile(globalCode: SampleCode): Action[AnyContent] = Action.async {
    profileService.getProfileModelView(globalCode).map { r => Ok(Json.toJson(r)) }
  }

  def storeUploadedAnalysis(uploadToken: String): Action[AnyContent] = Action.async {
    val analysisOpt = cache.pop(UploadedAnalysisKey(uploadToken))
    analysisOpt.fold(
      Future.successful(BadRequest(Json.obj("error" -> s"Missing entry in cache: $uploadToken")))
    ) { analysis =>
      profileService.create(analysis).map {
        case Left(errors) => BadRequest(Json.toJson(errors))
        case Right(profile) => Ok(Json.toJson(profile))
      }
    }
  }

  def verifyMixtureAssociation: Action[JsValue] = Action.async(parse.json) { request =>
    val json = request.body
    val body = for {
      mixedGen <- (json \ "mixtureGenotypification").validate[GenotypificationByType]
      profile <- (json \ "profile").validate[SampleCode]
      mixtureSubcategoryId <- (json \ "subcategoryId").validate[AlphanumericId]
    } yield (mixedGen, profile, mixtureSubcategoryId)

    body.fold(
      _ => Future.successful(BadRequest(Json.toJson("Error in the format request"))),
      bodyParsed => profileService.verifyMixtureAssociation(bodyParsed._1, bodyParsed._2, bodyParsed._3).map {
        case Right(associations) => Ok(Json.obj("result" -> true, "associations" -> Json.toJson(associations)))
        case Left(error) => Ok(Json.obj("result" -> false, "error" -> error))
      }
    )
  }

  def saveLabels: Action[JsValue] = Action.async(parse.json) { request =>
    val json = request.body
    val userId = request.headers.get("X-USER").getOrElse("")

    val body = for {
      profileId <- (json \ "globalCode").validate[SampleCode]
      labels <- (json \ "labeledGenotypification").validate[Profile.LabeledGenotypification]
    } yield (profileId, labels)

    body.fold(
      _ => Future.successful(BadRequest(Json.toJson("Error in the format request"))),
      bodyParsed => profileService.saveLabels(bodyParsed._1, bodyParsed._2, userId).map {
        case Right(sc) => Ok(Json.obj("result" -> true, "id" -> sc.text))
        case Left(error) => Ok(Json.obj("result" -> false, "error" -> Json.toJson(error)))
      }
    )
  }

  def findSubcategoryRelationships(subcategoryId: AlphanumericId): Action[AnyContent] = Action.async {
    categoryService.listCategories.map { categories =>
      val matchingRules = categories(subcategoryId).matchingRules
      Ok(Json.toJson(matchingRules)(Writes.seq[MatchingRule]))
    }
  }

  def getLabels(globalCode: SampleCode): Action[AnyContent] = Action.async {
    profileService.getLabels(globalCode).map { labels =>
      Ok(Json.toJson(labels))
    }
  }

  def getLabelsSets(): Action[AnyContent] = Action.async {
    Future.successful(Ok(Json.toJson(profileService.getLabelsSets())))
  }

  def addFiles(token: String, globalCode: SampleCode, idAnalysis: String, name: String): Action[AnyContent] = Action.async {
    profileService.saveFile(token, globalCode, idAnalysis, name).map { l =>
      val (right, left) = l.partition(_.isRight)
      if (left.isEmpty) Ok
      else Ok(Json.toJson(left.map(_.left.get)))
    }
  }

  def removeFile(fileId: String): Action[AnyContent] = Action.async { req =>
    profileService.removeFile(fileId, req.headers.get("X-USER").getOrElse("")).map {
      case Left(e) => BadRequest(Json.obj("error" -> e))
      case Right(pp) => Ok(Json.obj("fileId" -> pp))
    }
  }

  def removeEpg(fileId: String): Action[AnyContent] = Action.async { req =>
    profileService.removeEpg(fileId, req.headers.get("X-USER").getOrElse("")).map {
      case Left(e) => BadRequest(Json.obj("error" -> e))
      case Right(pp) => Ok(Json.obj("fileId" -> pp))
    }
  }

  def removeAll(): Action[AnyContent] = Action.async {
    profileService.removeAll().map {
      case Left(e) => BadRequest(Json.obj("error" -> e))
      case Right(pp) => Ok(Json.obj("fileId" -> pp))
    }
  }

  def removeProfile(globalCode: SampleCode): Action[AnyContent] = Action.async {
    profileService.removeProfile(globalCode).map {
      case Left(e) => BadRequest(Json.obj("error" -> e))
      case Right(_) => Ok(Json.obj("message" -> "Profile removed successfully"))
    }
  }

  def getFilesByCode(globalCode: SampleCode): Action[AnyContent] = Action.async {
    profileService.getFilesByCode(globalCode).map { lista =>
      Ok(Json.toJson(lista))
    }
  }

  def getFile(profileId: SampleCode, fileId: String): Action[AnyContent] = Action.async {
    profileService.getFile(profileId, fileId).map {
      case None => NotFound
      case Some(bytes) => Ok(bytes).as("application/octet-stream")
    }
  }

  def getFilesByAnalysisId(profileId: SampleCode, analysisId: String): Action[AnyContent] = Action.async {
    profileService.getFilesByAnalysisId(profileId, analysisId).map { listS =>
      Ok(Json.toJson(listS))
    }
  }

  def isReadOnly(globalCode: SampleCode): Action[AnyContent] = Action.async {
    profileService.isReadOnlySampleCode(globalCode).map { case (ro, error) =>
      Ok(Json.obj("isReadOnly" -> ro, "message" -> error))
    }
  }

  def exporterProfiles(): Action[JsValue] = Action.async(parse.json) { request =>
    val user = request.session.get("X-USER").getOrElse("")
    request.body.validate[ExportProfileFilters].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input =>
        profileExportService.filterProfiles(input).flatMap {
          case Nil => Future.successful(BadRequest(Messages("error.E2000")))
          case profileList =>
            profileExportService.exportProfiles(profileList, user).map {
              case Right(resourceName) => Ok(resourceName)
              case Left(error) => BadRequest(error)
            }
        }
    )
  }

  def exporterLimsFiles(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[ExportLimsFilesFilter].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input =>
        limsArchivesExporterService.exportLimsFiles(input).map {
          case Right(resourceName) => Ok(resourceName)
          case Left(error) => BadRequest(error)
        }
    )
  }

  def getExportFile(user: String): Action[AnyContent] = Action {
    val file = profileExportService.getFileOf(user)
    val bytes = Files.readAllBytes(file.toPath)
    Ok(bytes).as("application/octet-stream")
  }

  def getLimsAltaFile(): Action[AnyContent] = Action {
    val file = limsArchivesExporterService.getFileOfAlta
    val bytes = Files.readAllBytes(file.toPath)
    Ok(bytes).as("application/octet-stream")
  }

  def getLimsMatchFile(): Action[AnyContent] = Action {
    val file = limsArchivesExporterService.getFileOfMatch
    val bytes = Files.readAllBytes(file.toPath)
    Ok(bytes).as("application/octet-stream")
  }
}
