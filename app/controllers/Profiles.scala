package controllers

import javax.inject.{Inject, Singleton}
import configdata.CategoryService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{JsError, Json}
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import profile.GenotypificationByType._
import profile.{GenotypificationByType => _, _}
import services.{CacheService, UploadedAnalysisKey}
import types.{AlphanumericId, _}
import java.nio.file.Files

import play.api.i18n.Messages

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Left, Right};

@Singleton
class Profiles @Inject()(categoryService: CategoryService, profileService: ProfileService, cache: CacheService, profileExportService: ProfileExporterService, limsArchivesExporterService: LimsArchivesExporterService) extends Controller with MongoController with JsonActions {

  def create = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[NewAnalysis]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        val result = profileService.create(input)
        result map { outEither =>
          outEither.fold(
            error => BadRequest(Json.toJson(error)),
            success => Ok(Json.toJson(success)).withHeaders("X-CREATED-ID" -> success.globalCode.text))
        }
      })
  }

  def findByCode(globalCode: SampleCode) = Action.async { request =>
    profileService.findByCode(globalCode).map {
      case Some(profile) => Ok(Json.toJson(profile))
      case _ => Results.NotFound
    }
  }

  def findByCodes(globalCodes: List[SampleCode]) = Action.async { request =>
    profileService.findByCodes(globalCodes).map {
      case Seq() => Results.NotFound
      case profiles => Ok(Json.toJson(profiles))
    }
  }

  def addElectropherograms(token: String, globalCode: SampleCode, idAnalysis: String, name: String) = Action.async { req =>
    profileService.saveElectropherograms(token, globalCode, idAnalysis,name) map { l =>
      val (right, left) = l.partition(elem => elem.isRight)
      if (left.isEmpty) {
        Ok
      } else {
        val f = left.map(_.left.get)
        Ok(Json.toJson(f))
      }
    }
  }

  def getElectropherogramsByCode(globalCode: SampleCode) = Action.async { request =>
    profileService.getElectropherogramsByCode(globalCode).map { lista =>
      Ok(Json.toJson(lista))
    }
  }

  def getElectropherogramImage(profileId: SampleCode, electropherogramId: String) = Action.async { request =>

    profileService.getElectropherogramImage(profileId, electropherogramId).map { image =>
      image match {
        case None => Results.NotFound
        case Some(bytes) =>
          val fileContent: Enumerator[Array[Byte]] = Enumerator(bytes)
          Result(
            header = ResponseHeader(200),
            body = fileContent)
      }
    }
  }

  def getElectropherogramsByAnalysisId(profileId: SampleCode, analysisId: String) = Action.async { request =>
    profileService.getElectropherogramsByAnalysisId(profileId, analysisId).map { listS =>
      Ok(Json.toJson(listS))
    }
  }

  def getFullProfile(globalCode: SampleCode) = Action.async { request =>
    profileService.getProfileModelView(globalCode).map { r => Ok(Json.toJson(r)) }
  }

  def storeUploadedAnalysis(uploadToken: String) = JsonAction { () =>
    val analysisOpt = cache.pop(UploadedAnalysisKey(uploadToken))
    analysisOpt.fold[Future[Either[List[String], Profile]]]({
      Future.successful(Left(
        ("Misisng entry in cache" + uploadToken) :: Nil))
    })({ analysis =>
      profileService.create(analysis)
    })
  }

  def verifyMixtureAssociation = Action.async(BodyParsers.parse.json) { request =>
    val json = request.body
    val mixGenJson = (json \ "mixtureGenotypification")
    val profileJson = (json \ "profile")
    val mixtureSubcategoryIdJson = (json \ "subcategoryId")

    val body = for {
      mixedGen <- mixGenJson.validate[GenotypificationByType]
      profile <- profileJson.validate[SampleCode]
      mixtureSubcategoryId <- mixtureSubcategoryIdJson.validate[AlphanumericId]
    } yield (mixedGen, profile, mixtureSubcategoryId)

    body.fold(
      error => Future.successful(BadRequest(Json.toJson("Error in the format request"))),
      (bodyParsed) => profileService.verifyMixtureAssociation(bodyParsed._1, bodyParsed._2, bodyParsed._3).map {
        case Right(associations) => Ok(Json.obj("result" -> true, "associations" -> associations))
        case Left(error) => Ok(Json.obj("result" -> false, "error" -> error))
      })
  }

  def saveLabels = Action.async(BodyParsers.parse.json) { request =>
    val json = request.body
    val idJson = (json \ "globalCode")
    val labelsJson = (json \ "labeledGenotypification")
    val userId = request.headers.get("X-USER").get

    val body = for {
      profileId <- idJson.validate[SampleCode]
      labels <- labelsJson.validate[Profile.LabeledGenotypification]
    } yield (profileId, labels)

    body.fold(
      error => Future.successful(BadRequest(Json.toJson("Error in the format request"))),
      bodyParsed => profileService.saveLabels(bodyParsed._1, bodyParsed._2, userId).map {
        case Right(sc) => Ok(Json.obj("result" -> true, "id" -> sc))
        case Left(error) => Ok(Json.obj("result" -> false, "error" -> error))
      })
  }

  def findSubcategoryRelationships(subcategoryId: AlphanumericId) = Action.async { request =>
    val fcs = Future.successful(categoryService.listCategories)
    fcs map { categories =>
      val matchingRules = categories(subcategoryId).matchingRules
      Ok(Json.toJson(matchingRules))
    }
  }

  def getLabels(globalCode: SampleCode) = Action.async { request =>
    profileService.getLabels(globalCode).map { labels =>
      Ok(Json.toJson(labels))
    }
  }

  def getLabelsSets() = Action.async { request =>
    Future(Ok(Json.toJson(profileService.getLabelsSets())))
  }

  def addFiles(token: String, globalCode: SampleCode, idAnalysis: String, name: String) = Action.async { req =>
    profileService.saveFile(token, globalCode, idAnalysis, name) map { l =>
      val (right, left) = l.partition(elem => elem.isRight)
      if (left.isEmpty) {
        Ok
      } else {
        val f = left.map(_.left.get)
        Ok(Json.toJson(f))
      }
    }
  }
  def removeFile(fileId:String) = Action.async { req =>
    profileService.removeFile(fileId,req.headers.get("X-USER").get) map {
      case Left(e) => BadRequest(Json.obj("error" -> e))
      case Right(pp) => Ok(Json.obj("fileId" -> pp))
    }
  }
  def removeEpg(fileId:String) = Action.async { req =>
    profileService.removeEpg(fileId,req.headers.get("X-USER").get) map {
      case Left(e) => BadRequest(Json.obj("error" -> e))
      case Right(pp) => Ok(Json.obj("fileId" -> pp))
    }
  }
  def getFilesByCode(globalCode: SampleCode) = Action.async { request =>
    profileService.getFilesByCode(globalCode).map { lista =>
      Ok(Json.toJson(lista))
    }
  }

  def getFile(profileId: SampleCode, fileId: String) = Action.async { request =>

    profileService.getFile(profileId, fileId).map { image =>
      image match {
        case None => Results.NotFound
        case Some(bytes) =>
          val fileContent: Enumerator[Array[Byte]] = Enumerator(bytes)
          Result(
            header = ResponseHeader(200),
            body = fileContent)
      }
    }
  }

  def getFilesByAnalysisId(profileId: SampleCode, analysisId: String) = Action.async { request =>
    profileService.getFilesByAnalysisId(profileId, analysisId).map { listS =>
      Ok(Json.toJson(listS))
    }
  }

  def isReadOnly(globalCode: SampleCode) = Action.async { request =>
    profileService.isReadOnlySampleCode(globalCode).map { result =>
      Ok(Json.obj("isReadOnly" -> result._1, "message" -> result._2))
    }
  }

  def exporterProfiles() = Action(BodyParsers.parse.json) { request =>
    val user = request.session("X-USER")
    request.body.validate[ExportProfileFilters].fold(
      errors => {
        BadRequest(JsError.toFlatJson(errors))
      },
      input => {
        Await.result(
          profileExportService.filterProfiles(input).flatMap {
            case Nil => Future.successful(BadRequest(Messages("error.E2000")))
            case profileList =>
              profileExportService.exportProfiles(profileList, user).map {
                case Right(resourceName) => {
                  Ok(resourceName);
                }
                case Left(error) => BadRequest(error)
              }
          }, Duration.Inf)
      }
    )
  }

  def exporterLimsFiles() = Action(BodyParsers.parse.json) { request =>
    val user = request.session("X-USER")
    request.body.validate[ExportLimsFilesFilter].fold(
      errors => {
        BadRequest(JsError.toFlatJson(errors))
      },
      input => {
        Await.result(
/*
          profileExportService.filterProfiles(input).flatMap {
            case Nil => Future.successful(BadRequest(Messages("error.E2000")))
            case profileList =>
              profileExportService.exportProfiles(profileList, user).map {
                case Right(resourceName) => {
                  Ok(resourceName);
                }
                case Left(error) => BadRequest(error)
              }
*/
          limsArchivesExporterService.exportLimsFiles(input).flatMap{
            case Right(resourceName) =>  Future.successful(Ok(resourceName))
            case Left(error) => Future.successful(BadRequest(error))
          }
          , Duration.Inf)
      }
    )
  }

  def getExportFile(user: String) = Action.async { request =>
    val file = profileExportService.getFileOf(user)
    val fileContent: Enumerator[Array[Byte]] = Enumerator.fromFile(file)
    Future.successful(Result(header = ResponseHeader(200), body = fileContent))
  }

  def getLimsAltaFile() = Action.async { request =>
    val file = limsArchivesExporterService.getFileOfAlta()
    val fileContent: Enumerator[Array[Byte]] = Enumerator.fromFile(file)
    Future.successful(Result(header = ResponseHeader(200), body = fileContent))
  }

  def getLimsMatchFile() = Action.async { request =>
    val file = limsArchivesExporterService.getFileOfMatch()
    val fileContent: Enumerator[Array[Byte]] = Enumerator.fromFile(file)
    Future.successful(Result(header = ResponseHeader(200), body = fileContent))
  }

}
