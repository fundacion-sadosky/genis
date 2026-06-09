package controllers

import bulkupload.ProtoProfileRepository

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BodyParsers, Controller}

import javax.inject.{Inject, Singleton}
import com.ning.http.client.Request
import connections.HeaderInsterconnections.labCode
import trace.{TraceSearch, TraceService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Left, Right}
import connections._
import play.api.Logger
import play.api.i18n.Messages
import types.SampleCode
import profiledata.{ProfileDataRepository, ProfileDataService}
import matching.{MatchingRepository, MatchResult}

@Singleton
class Interconnections @Inject()( val protoRepo: ProtoProfileRepository,
                                  interconnectionService : InterconnectionService,
                                  profiledataService: ProfileDataService,
                                  profileDataRepository: ProfileDataRepository,
                                  matchingRepository: MatchingRepository
                                ) extends Controller {
  val logger: Logger = Logger(this.getClass())

  def getConnections = Action.async {
    request => {
      interconnectionService.getConnections().map{
        case Left(e) => InternalServerError(Json.obj("message" -> e))
        case Right(conn) => Ok(Json.toJson(conn))
      }
    }
  }

  def getConnectionStatus(url: String) = Action.async {
    request => {
      interconnectionService.getConnectionsStatus(url).map{
        case Left(e) => NotFound(Json.obj("message" -> e))
        case Right(_) => Ok.withHeaders("X-CREATED-ID" -> url)
      }
    }
  }

  def updateConnections = Action.async(BodyParsers.parse.json) { request => {
    val input = request.body.validate[Connection]
    input.fold(errors => {
      Future.successful(BadRequest(JsError.toFlatJson(errors)))
    },
      connections => {
        interconnectionService.updateConnections(connections).map{
          case Left(e) => BadRequest(Json.obj("message" -> e))
          case Right(conn) => Ok.withHeaders("X-CREATED-ID" -> connections.superiorInstance)
        }
      })
  }
  }

  def getCategoryTreeComboConsumer = Action.async {
    interconnectionService.getCategoryConsumer.map{
      case Left(e) => NotFound(Json.obj("message" -> e))
      case Right(tree) => Ok(tree)
    }
  }

  def insertConnection = Action.async {

    request =>{
      interconnectionService.connect().map{
        case Left(e) => BadRequest(Json.obj("message" -> e))
        case Right(()) => Ok
      }
    }

  }

  def insertInferiorInstanceConnection = Action.async {

    request =>{
      val url = request.headers.get(HeaderInsterconnections.url)
      val laboratory = request.headers.get(HeaderInsterconnections.laboratoryImmediateInstance)

      (url,laboratory) match {
        case (Some(urlContent),Some(laboratory)) => {
          interconnectionService.insertInferiorInstanceConnection(urlContent,laboratory).map{
            case Left(e) => BadRequest(Json.obj("message" -> e))
            case Right(()) => Ok.withHeaders("X-CREATED-ID" -> laboratory)
          }
        }
        case (_,_) => Future.successful(BadRequest(Json.obj("message" -> "Debe completar los parámetros de url ")))
      }

    }

  }

  def getInferiorInstances = Action.async {
    request =>{
      interconnectionService.getAllInferiorInstances().map{
        case Left(e) => BadRequest(Json.obj("message" -> e))
        case Right(list) => Ok(Json.toJson(list))
      }
    }
  }

  def getInferiorInstancesStatus = Action.async {
    request =>{
      interconnectionService.getAllInferiorInstanceStatus().map{
        case Left(e) => BadRequest(Json.obj("message" -> e))
        case Right(list) => Ok(Json.toJson(list))
      }
    }
  }

  def updateInferiorInstance = Action.async(BodyParsers.parse.json) {
    request =>{
      val input = request.body.validate[InferiorInstanceFull]
      input.fold(errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
        inferiorInstance => {
          interconnectionService.updateInferiorInstance(inferiorInstance).map{
            case Left(e) => BadRequest(Json.obj("message" -> e))
            case Right(()) => Ok.withHeaders("X-CREATED-ID" -> inferiorInstance.laboratory)
          }
        })
    }
  }
  // Instancia superior recibe profile
  def importProfile(): Action[JsValue] = Action.async(BodyParsers.parse.json) {

    request => {
      val labcode = request.headers.get(HeaderInsterconnections.labCode)
      val labCodeInstanceOrigin = request.headers.get(
        HeaderInsterconnections.laboratoryOrigin
      )
      val labCodeImmediateInstance = request.headers.get(
        HeaderInsterconnections.laboratoryImmediateInstance
      )
      val dateAdded = request.headers.get(HeaderInsterconnections.sampleEntryDate)
      (labcode, dateAdded, labCodeInstanceOrigin, labCodeImmediateInstance) match {
        case (
          Some(labcode),
          Some(dateAdded),
          Some(labCodeInstanceOrigin),
          Some(labCodeInmediateInstanceOrigin)
          ) =>
          val input = request.body.validate[connections.ProfileTransfer]
          input.fold(
            errors => {
              Future.successful(BadRequest(JsError.toFlatJson(errors)))
            },
            profileTransfer => {
              interconnectionService
                .importProfile(
                  profileTransfer.profile,
                  labcode,
                  dateAdded,
                  labCodeInstanceOrigin,
                  labCodeInmediateInstanceOrigin,
                  profileTransfer.profileAssociated
                )
              Future.successful(
                Ok.withHeaders("X-CREATED-ID" -> profileTransfer.profile.globalCode.text)
              )
            }
          )
        case (_,_,_,_) => {
          Future.successful(BadRequest)
        }
      }
    }
  }
  // instancia superior recibe el codigo y el usuario que borró el perfil en la instancia inferior
  def deleteProfileFromInferior(id:String, userName: String) = Action.async(BodyParsers.parse.json) {
    request =>{
      val labcode = request.headers.get(HeaderInsterconnections.labCode)
      val labCodeInstanceOrigin = request.headers.get(HeaderInsterconnections.laboratoryOrigin)
      val labCodeImmediateInstance = request.headers.get(HeaderInsterconnections.laboratoryImmediateInstance)

      (labcode,labCodeInstanceOrigin,labCodeImmediateInstance) match {
        case (Some(labcode),Some(lio),Some(li)) =>{
          val input = request.body.validate[profiledata.DeletedMotive]
          input.fold(errors => {
            Future.successful(BadRequest(JsError.toFlatJson(errors)))
          },
            motive => {
              interconnectionService.receiveDeleteProfile(id,motive,lio,li, true, userName).map{
                case Left(e) => BadRequest(Json.obj("message" -> e))
                case Right(_) => Ok.withHeaders("X-CREATED-ID" -> id.toString)
              }
            })
        }
        case (_,_,_) => {
          Future.successful(BadRequest)
        }
      }
    }
  }

  def retrieveImmediateInferiorInstanceLabCode(globalCode: String): String = {
    profileDataRepository.getImmediateInferiorInstanceLabCode(globalCode)
  }

  def approveProfiles(userName: String): Action[JsValue] = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[List[ProfileApproval]]
    input.fold(
      errors => Future.successful(BadRequest(JsError.toFlatJson(errors))),
      approvals => {
        // Llamamos directamente al servicio.
        // Se asume que interconnectionService.approveProfiles internamente desencadena
        // la lógica que lleva a notifyApprovalChangeStatus.
        interconnectionService.approveProfiles(approvals, userName).map {
          case Left(e) =>
            // Manejo de error del servicio
            BadRequest(Json.obj("message" -> e))

          case Right(()) =>
            // Éxito: Solo devolvemos el OK con los headers correspondientes.
            // La actualización de PROFILE_RECEIVED ya ocurrió en el flujo interno.
            Ok.withHeaders(
              "X-CREATED-ID" -> approvals.map(a => a.globalCode).mkString(start = "[", sep = ",", end = "]")
            )
        }
      }
    )
  }


  private def getLabCodeFromGlobalCode(globalCode: String): Option[String] = {
    val parts = globalCode.split("-")
    if (parts.length >= 4) {
      Some(parts(2))
    } else {
      None  // Or handle the case where the globalCode doesn't have the expected format
    }
  }

  def getPendingProfiles(page:Int,pageSize:Int): Action[AnyContent] = Action.async {
    request => {
      interconnectionService
        .getPendingProfiles(ProfileApprovalSearch(page, pageSize))
        .map{ list => Ok(Json.toJson(list)) }
    }
  }

  def getTotalPendingProfiles = Action.async {
    request =>{
      interconnectionService.getTotalPendingProfiles().map{
        count => Ok(Json.toJson(count))
      }
    }
  }


  def rejectPendingProfile(id: String,motive:String,idMotive:Long, userName:String, isCategoryModification: Boolean) = Action.async {
    request => {
      interconnectionService.rejectProfile(ProfileApproval(id),motive,idMotive,userName).map{
        case Left(e) => BadRequest(Json.obj("message" -> e))
        case Right(()) => {
          val labCode: Option[String] = getLabCodeFromGlobalCode(id)
          labCode match {
            //Insertar el perfil en PROFILE_RECEIVED table
            case Some(code) => {
              profiledataService.addProfileReceivedRejected(code, id, 21L,  motive,userName, isCategoryModification)
            }// Using profiledataService to access the repository
            case None => Future.successful(Left("Invalid global code format")) // Or handle the missing labCode case
          }
          Ok.withHeaders("X-CREATED-ID" -> id)
        }
      }
    }
  }



  //Cuando se recibe un perfil de una instancia inferior
  def uploadProfile(globalCode:String, userName: String) = Action.async {
    _ => {
      interconnectionService.uploadProfile(globalCode, userName).map{
        case Left(e) => BadRequest(Json.obj("message" -> e))
        case Right(()) => Ok.withHeaders("X-CREATED-ID" -> globalCode)
      }
    }
  }
// Acá se entra desde routes (es decir que se hace UPDATE de un perfile subido (Update de PROFILE_UPLOADED)
  def updateUploadStatus(
                          globalCode: String,
                          status:Long,
                          motive:Option[String],
                          userName:String,
                          isCategoryModification:Boolean = false,
                          operationOriginatedInInstance: Option[String]
                        ): Action[AnyContent] = Action.async {
    _ => {
      interconnectionService
        .updateUploadStatus(globalCode, status, motive, userName, isCategoryModification, operationOriginatedInInstance.getOrElse(""))
        .map{
          case Left(e) => BadRequest(Json.obj("message" -> e))
          case Right(()) => Ok.withHeaders("X-CREATED-ID" -> globalCode)
        }
    }
  }

  def getUploadStatus(globalCode: String): Action[AnyContent] = Action.async {
    _ => {
      profiledataService
        .getProfileUploadStatusByGlobalCode(SampleCode(globalCode))
        .map {
          case None => BadRequest(
            Json.obj("message" -> Messages("E0900", globalCode))
          )
          case status => Ok(Json.toJson(status))
        }
    }
  }
  def receiveMatchFromSuperior() = Action.async(BodyParsers.parse.json) {
    request =>{
      val input = request.body.validate[MatchSuperiorInstance]
      input.fold(errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
        matchSuperiorInstance => {
          interconnectionService.receiveMatchFromSuperior(matchSuperiorInstance).map{
            case Left(_) => Ok.withHeaders("X-CREATED-ID" -> "")
            case Right(_) => Ok.withHeaders("X-CREATED-ID" -> "")
          }
        }
      )
    }
  }
  def receiveMatchStatus() = Action.async(BodyParsers.parse.json) {
    request =>{
      val input = request.body.validate[ConvertStatusInterconnection]
      input.fold(errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
        convertStatusInterconnection => {
          interconnectionService.receiveMatchStatus(convertStatusInterconnection.matchId,
            convertStatusInterconnection.firingCode,
            convertStatusInterconnection.leftProfileCode,
            convertStatusInterconnection.rightProfileCode,
            convertStatusInterconnection.status,
            convertStatusInterconnection.labOrigin,
            convertStatusInterconnection.labImmediate,
            convertStatusInterconnection.userName).map{
            case _ => Ok.withHeaders("X-CREATED-ID" -> "")
          }
        }
      )
    }
  }
  def receiveFile() = Action.async(BodyParsers.parse.json(1024*1024*16)) {
    request =>{
      val input = request.body.validate[FileInterconnection]
      input.fold(errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
        fileData => {
          interconnectionService.receiveFile(fileData).map{
            case _ => Ok.withHeaders("X-CREATED-ID" -> "")
          }
        }
      )
    }
  }

  def getIsProfileReplicableInternalCode(internalCode: String) = Action.async { _ =>
    interconnectionService.isUplpoadableInternalCode(internalCode).map { isReplicable =>
      Ok(Json.toJson(isReplicable))
    }
  }
}
