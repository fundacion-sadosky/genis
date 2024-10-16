package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BodyParsers, Controller}

import javax.inject.{Inject, Singleton}
import com.ning.http.client.Request
import trace.{TraceSearch, TraceService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Left, Right}
import connections._
import play.api.Logger
import types.SampleCode

@Singleton
class Interconnections @Inject()(interconnectionService : InterconnectionService) extends Controller {
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
  // instancia superior recibe el codigo de profile a borrar
  def deleteProfile(id:String) = Action.async(BodyParsers.parse.json) {
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
              interconnectionService.receiveDeleteProfile(id,motive,lio,li).map{
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
  def approveProfiles(): Action[JsValue] = Action.async(BodyParsers.parse.json){
    request =>{
      val input = request.body.validate[List[ProfileApproval]]
      input.fold(errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
        approvals => {
          interconnectionService.approveProfiles(approvals).map{
            case Left(e) => BadRequest(Json.obj("message" -> e))
            case Right(()) => Ok.withHeaders("X-CREATED-ID" -> approvals.map(a=>a.globalCode).mkString(start = "[",sep =",",end ="]"))
          }
        }
      )

    }
  }

  def getPendingProfiles(page:Int,pageSize:Int) = Action.async {
    request =>{
      interconnectionService.getPendingProfiles(ProfileApprovalSearch(page,pageSize)).map{
        list => Ok(Json.toJson(list))
      }
    }
  }

  def getTotalPendingProfiles = Action.async {
    request =>{
      interconnectionService.getTotalPendingProfiles().map{
        count => Ok(Json.toJson(count))
      }
    }
  }

  def rejectPendingProfile(id: String,motive:String,idMotive:Long) = Action.async {
      request => {
        val user = request.headers.get("X-USER")
          interconnectionService.rejectProfile(ProfileApproval(id),motive,idMotive,user).map{
            case Left(e) => BadRequest(Json.obj("message" -> e))
            case Right(()) => Ok.withHeaders("X-CREATED-ID" -> id)
          }
    }
  }

  def uploadProfile(globalCode:String) = Action.async {
    _ => {
      interconnectionService.uploadProfile(globalCode).map{
        case Left(e) => BadRequest(Json.obj("message" -> e))
        case Right(()) => Ok.withHeaders("X-CREATED-ID" -> globalCode)
      }
    }
  }
  def updateUploadStatus(globalCode: String,status:Long,motive:Option[String]) = Action.async {
    _ => {
      interconnectionService.updateUploadStatus(globalCode,status,motive).map{
        case Left(e) => BadRequest(Json.obj("message" -> e))
        case Right(()) => Ok.withHeaders("X-CREATED-ID" -> globalCode)
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
            convertStatusInterconnection.labImmediate).map{
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
}
