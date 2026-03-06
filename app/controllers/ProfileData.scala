package controllers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, MINUTES, SECONDS}
import javax.inject.Inject
import javax.inject.Singleton
import models.Tables
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{Format, JsError, JsPath, JsValue, Json, Reads, Writes, __}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, BodyParsers, Controller, ResponseHeader, Result, Results}
import profile.ProfileService
import profile.Profile
import profiledata._
import configdata.CategoryService
import matching.{MatchJobEndend, MatchJobFail, MatchJobStatus, MatchingProcessStatus, MatchingService}
import connections.InterconnectionService
import org.apache.spark.sql.catalyst.dsl.expressions.booleanToLiteral
import play.api.data.validation.ValidationError
import types._
import profiledata.ProfileDataService
import play.api.mvc._
import play.api.libs.json._
import play.api.data.validation.ValidationError
import play.api.libs.iteratee.{Enumeratee, Iteratee}

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global // O tu propio ExecutionContext implícito


@Singleton
class ProfileData @Inject() (
                              profiledataService: ProfileDataService,
                              profileService: ProfileService,
                              categoryService: CategoryService,
                              matchingService: MatchingService,
                              interconnectionService: InterconnectionService,
                              matchingProcessStatus: MatchingProcessStatus // Inyecta esto si no está en el constructor
                            ) extends Controller {

  def update(globalCode: SampleCode): Action[JsValue] = Action.async(BodyParsers.parse.json) {
    request =>
      val profileDataJson = request.body.validate[ProfileDataAttempt]
      profileDataJson
        .fold(
          errors => {
            Future
              .successful(
                BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))
              )
          },
          profileData =>
            profiledataService
              .updateProfileData(globalCode,profileData) map { result => Ok(Json.toJson(result)) }
        )
  }

  def isReadOnly(globalCode: SampleCode): Action[AnyContent] =
    Action.async {
      request => Future.successful(Ok(Json.obj("data" -> true)));
    }

  def modifyCategory(
                      globalCode: SampleCode,
                      replicate: Boolean,
                      userName: String
                    )(
                    ): Action[JsValue] = Action
    .async(BodyParsers.parse.json) {
      request =>
        val profileDataJson = request.body.validate[ProfileDataAttempt]
        val errorWhileReadingBody = (error: Seq[(JsPath, Seq[ValidationError])]) =>
          Future
            .successful(
              BadRequest(Json
                .obj(
                  "status" -> "KO",
                  "message" -> JsError.toFlatJson(error)
                )
              )
            )
        val getProfileId = (updateResult: Option[String]) => {
          updateResult
          match {
            case None => Right(globalCode)
            case Some(error) => Left(error)
          }
        }
        val getProfile = (profileIdOrError: Either[String, SampleCode]) => {
          profileIdOrError
          match {
            case Left(error) => Future
              .successful(Left(error))
            case Right(profileId) =>
              profileService
                .get(profileId)
                .map {
                  case None => Left(Messages("error.E0101"))
                  case Some(profile) => Right(profile)
                }
          }
        }
        val copyAndModifyProfile = (profileData: ProfileDataAttempt) =>
          (profileOrError: Either[String, Profile]) => {
            profileOrError
              .right
              .map(_.copy(categoryId = profileData.category))
          }
        val updateProfile = (newProfileOrError: Either[String, Profile]) => {
          newProfileOrError match {
            case Left(error) => Future
              .successful(Left(error))
            case Right(prof) =>
              try {
                profileService
                  .updateProfile(prof)
                  .map(_ => Right(prof))
              } catch {
                case e: Exception => Future
                  .successful(Left(Messages("error.E0132")))
              }
          }
        }
        val checkForReplicatedMatches = (prof: Profile) => {
          matchingService.findMatchingResults(prof.globalCode).map { maybeMatchingResults =>
            maybeMatchingResults match {
              case Some(matchingResults) =>
                val replicatedMatches = matchingResults.results.exists { result =>
                  profiledataService.getIsProfileReplicatedInternalCode(result.internalSampleCode)
                }
                if (replicatedMatches) {
                  Left(Messages("error.E0732"))  // Error if any match is replicated
                } else {
                  Right(prof)
                }
              case None =>
                Right(prof)  // No matches found, so no replicated matches
            }
          }
        }
        val uploadModifiedProfile = (prof: Profile) => {
          profiledataService
            .get(globalCode) // Asumimos que esto devuelve Future[Option[ProfileData]]
            .flatMap { // flatMap espera que la función pasada devuelva un Future
              case Some(pdata) =>
                // Si uploadProfileToSuperiorInstance devuelve Unit (síncrono), lo envolvemos en un Future
                Future.successful(
                  interconnectionService
                    .uploadProfileToSuperiorInstance(prof, pdata, userName)
                )
              case None =>
                // Si no hay pdata, simplemente devolvemos un Future completado con Unit
                Future.successful(())
            }
        }

        val findMatchestoJson = (result: Either[String, Profile]) => {
          result match {
            case Left(error) => Json
              .obj(
                "status" -> "error",
                "message" -> error
              )
            case Right(p) => Json
              .obj(
                "status" -> "OK",
                "message" -> Messages("success.S0602", p.globalCode.text)
              )
          }
        }

        val updateCategoryInProfile = (profileData: ProfileDataAttempt) => {
          val processMatchingAndReplication = (profileOrError: Either[String, Profile]) => {
            profileOrError match {
              case Left(error) => Future.successful(
                Json.arr(Json.obj("status" -> "error", "message" -> error))
              )
              case Right(prof) =>
                // 1. Lanzar el proceso de match.
                // Si fireMatching devuelve Unit, simplemente lo ejecutamos.
                // Asumimos que al ejecutarlo, el sistema empezará a emitir MatchJobStatus.
                profileService.fireMatching(globalCode) // <-- Ya no es un Future, solo se ejecuta

                // 2. Esperar MatchingProcessStatus que sea MatchJobEndend.
                // Se usa un Promise para convertir el evento de MatchJobStatus en un Future.
                val matchEndPromise = Promise[MatchJobStatus]()

                // Iniciar el consumo del enumerator para escuchar los estados.
                // IMPORTANTE: Esta lógica asume que el próximo MatchJobEndend (o Fail)
                // que se emita en el stream de broadcast corresponde a la tarea
                // que acabamos de iniciar para este globalCode.
                matchingProcessStatus.getJobStatus()
                  .apply(
                    Iteratee.foreach[MatchJobStatus] { status =>
                      // Solo completa el promise la primera vez que se recibe un estado final
                      if (!matchEndPromise.isCompleted) {
                        status match {
                          case MatchJobEndend => matchEndPromise.success(status)
                          case MatchJobFail => matchEndPromise.success(status) // Fallo el promise con el estado de fallo
                          case _ => // Otros estados, ignorar y seguir esperando
                        }
                      }
                    }
                  ) // Esto lanza el consumo del stream.

                // Ahora, esperamos directamente el Future del promise.
                matchEndPromise.future.flatMap {
                  case MatchJobEndend =>
                    // El proceso de matching terminó exitosamente.
                    if (replicate) {
                      // 3. Cuando termina verificar que el perfil no tenga matches con otro ya replicado.
                      checkForReplicatedMatches(prof).flatMap {
                        case Left(error) => Future.successful(Json.arr(Json.obj("status" -> "error", "message" -> error)))
                        case Right(cleanProf) =>
                          // 4. Si (replicar) y (no posee matches antes replicados) -> replicar
                          uploadModifiedProfile(cleanProf).map { _ =>
                            Json.arr(
                              Json.obj("status" -> "OK", "message" -> Messages("success.S0100", prof.globalCode.text)),
                              findMatchestoJson(Right(prof))
                            )
                          }
                      }
                    } else {
                      // No se solicitó replicación, por lo que no se verifica replicados ni se sube.
                      Future.successful(
                        Json.arr(
                          Json.obj("status" -> "OK", "message" -> Messages("success.S0100", prof.globalCode.text)),
                          findMatchestoJson(Right(prof))
                        )
                      )
                    }
                  case MatchJobFail =>
                    // El proceso de matching falló
                    Future.successful(Json.arr(Json.obj("status" -> "error", "message" -> Messages("error.E_MATCHING_FAILED"))))
                  case otherStatus => // Manejar otros estados inesperados si es necesario
                    Future.successful(Json.arr(Json.obj("status" -> "error", "message" -> Messages("error.E_UNEXPECTED_MATCH_STATUS", otherStatus.toString))))
                }
            }
          }

          profiledataService
            .updateProfileCategoryData(globalCode, profileData, userName)
            .map(getProfileId)
            .flatMap(getProfile)
            .map(copyAndModifyProfile(profileData))
            .flatMap(updateProfile)
            .flatMap(processMatchingAndReplication) // Hooked in here
            .map(Ok(_))
        }
        profileDataJson
          .fold(
            errorWhileReadingBody,
            updateCategoryInProfile
          )
    }



  def getByCode(sampleCode: SampleCode) = Action.async { request =>
    profiledataService.get(sampleCode) map { result =>
      result.map { profileData => Ok(Json.toJson(profileData)) }.getOrElse {
        NotFound
      }
    }
  }

  def deleteProfile(profileId: SampleCode) = Action.async(BodyParsers.parse.json) { request =>
    val motive = request.body.validate[DeletedMotive]
    val userId = request.headers.get("X-USER").get

    motive.fold(errors =>
    { Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))) },
      motive => profiledataService.deleteProfile(profileId, motive, userId) map { result =>
        result.fold(error => BadRequest(Json.obj("status" -> "KO", "message" -> error)),
          sampleCode => {
            Ok(Json.toJson(sampleCode))

          })
      })
  }

  def getDeleteMotive(sampleCode: SampleCode) = Action.async {
    profiledataService.getDeleteMotive(sampleCode).map { opt =>
      opt.fold(Results.NoContent)(mot => Ok(Json.toJson(mot)))
    }
  }

  def isEditable(sampleCode: SampleCode) = Action.async { request =>
    profiledataService.isEditable(sampleCode: SampleCode) map {
      result => Ok(Json.toJson(result))
    }
  }

  def create = Action.async(BodyParsers.parse.json) { request =>
    val profileDataJson = request.body.validate[ProfileDataAttempt]

    profileDataJson.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
      },
      profileData => {
        profiledataService.create(profileData).map {
          case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> error))
          case Right(sampleCode) => Ok(Json.obj("sampleCode" -> sampleCode)).withHeaders("X-CREATED-ID" -> sampleCode.text)
        }
      })
  }

  def removeAll = Action.async { request => ???}
  //    profiledataService.removeAll() map { result =>
  //      Ok(Json.toJson(result))
  //    }
  //  }

  def removeProfile(globalCode: SampleCode) = Action.async { request =>
    profiledataService.removeProfile(globalCode) map {
      case Left(error) => BadRequest(Json.obj("status" -> "KO", "message" -> error))
      case Right(_) => Ok(Json.obj("status" -> "OK", "message" -> Messages("success.S0100", globalCode.text)))
    }
  }

  def get(id: Long) = Action.async(BodyParsers.parse.json) { request =>
    profiledataService.get(id) map {
      profileData =>
        Ok(Json.obj(
          "profile" -> profileData._1,
          "category" -> profileData._2))
    }
  }

  def getResources(imageType: String, id: Long) = Action.async { request =>
    profiledataService.getResource(imageType, id) map {
      case None => Results.NotFound
      case Some(bytes) =>
        val fileContent: Enumerator[Array[Byte]] = Enumerator(bytes)
        Result(
          header = ResponseHeader(200),
          body = fileContent)
    }
  }

  def getDesktopProfiles = Action.async { request =>
    profiledataService.getDesktopProfiles map { result =>
      Ok(Json.toJson(result))
    }
  }

  def findByCode(globalCode: SampleCode) = Action.async { request =>
    profiledataService.findByCode(globalCode) map { result =>
      result.map { profileData => Ok(Json.toJson(profileData)) }.getOrElse {
        NotFound
      }
    }
  }

  def findByCodes(
                   globalCodes: List[SampleCode]
                 ): Action[AnyContent] = Action.async { request =>
    profiledataService
      .findByCodes(globalCodes)
      .map(
        l => Ok(Json.toJson(l))
      )
  }

  def findByCodeWithAssociations(globalCode: SampleCode) = Action.async { request =>
    profiledataService.findByCodeWithAssociations(globalCode) map {
      case Some((profileData, group, category)) =>
        Ok(Json.obj(
          "profileData" -> profileData,
          "group" -> group,
          "category" -> category))
      case None => BadRequest
    }
  }


  def getIsProfileReplicatedInternalCode(internalCode: String): Action[AnyContent] = Action.async { request =>
    Future { // Wrap the synchronous call in a Future
      val isReplicated = profiledataService.getIsProfileReplicatedInternalCode(internalCode)
      Ok(Json.toJson(isReplicated)) // Return the boolean as JSON
    }
  }

  def countProfiles(): Action[AnyContent] = Action.async { request =>
    profiledataService.countProfiles() map { count =>
      Ok(Json.toJson(count))
    }
  }

}