package connections

import java.security.SecureRandom
import matching.{MatchStatus, MatchingProfile, MatchingRepository, MatchingService}

import java.util.{Calendar, Date}
import services.ProfileLabKey

import javax.inject.{Inject, Named, Singleton}
import akka.pattern.ask
import org.apache.commons.codec.binary.Base64
import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import audit.PEOSignerActor
import connections.ProfileTransfer

import scala.concurrent.duration._
import scala.concurrent.Await
import models.Tables.ExternalProfileDataRow
import configdata.{CategoryConfiguration, CategoryRepository, CategoryService}
import inbox._
import kits.{AnalysisType, StrKitService}
import play.api.Logger
import play.api.libs.json.{JsValue, Json, __}
import play.api.libs.ws.{WSBody, _}
import profile.{Profile, ProfileService}
import profiledata.{DeletedMotive, ProfileData, ProfileDataAttempt, ProfileDataService}
import types.{AlphanumericId, Permission, SampleCode}
import user.{RoleService, UserService}
import connections.MatchSuperiorInstance
import util.FutureUtils

import java.util
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.async.Async.{async, await}
import play.api.i18n.Messages

import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS}
import scala.util.Try
import inbox.DiscardInfoInbox
import inbox.HitInfoInbox
import inbox.DeleteProfileInfo
import matching.MatchResult
import services.CacheService
import trace.{CategoryChangeRejectedInSupInfo, MatchInfo, MatchTypeInfo, ProfileImportedFromInferiorInfo, ProfileRejectedInSuperiorInfo, SuperiorCategoryChangeRejectedInfo, SuperiorInstanceCategoryModificationInfo, Trace, TraceInfo, TraceService}

trait InterconnectionService {

  def getConnections(): Future[Either[String, Connection]]

  def updateConnections(connection: Connection): Future[Either[String, Connection]]

  def getConnectionsStatus(url: String): Future[Either[String, Unit]]

  def getCategoryConsumer: Future[Either[String, JsValue]]

  def insertInferiorInstanceConnection(url: String, laboratory: String): Future[Either[String, Unit]]

  def connect(): Future[Either[String, Unit]]

  def getAllInferiorInstances(): Future[Either[String, List[InferiorInstanceFull]]]

  def getAllInferiorInstanceStatus(): Future[Either[String, List[InferiorInstanceStatus]]]

  def updateInferiorInstance(row: InferiorInstanceFull): Future[Either[String, Unit]]

  def uploadProfileToSuperiorInstance(profile: Profile, pd: ProfileData): Unit

  def importProfile(
                     profile: Profile,
                     labo: String,
                     sampleEntryDate: String,
                     labCodeInstanceOrigin: String,
                     labCodeInmediateInstanceOrigin: String,
                     profileAssociated: Option[Profile] = None
                   ): Unit

  def approveProfiles(profileApprovals: List[ProfileApproval]): Future[Either[String, Unit]]

  def rejectProfile(profileApproval: ProfileApproval, motive: String, idMotive: Long, user: String): Future[Either[String, Unit]]

  def getPendingProfiles(profileApprovalSearch: ProfileApprovalSearch): Future[List[PendingProfileApproval]]

  def getTotalPendingProfiles(): Future[Long]

  def notifyChangeStatus(
                          globalCode: String,
                          labCode: String,
                          status: Long,
                          motive: Option[String] = None,
                          userName: Option[String] = None,
                          isCategoryModification:Boolean = false
                        ): Future[Unit]

  def uploadProfile(globalCode: String): Future[Either[String, Unit]]

  def updateUploadStatus(
                          globalCode: String,
                          status: Long,
                          motive: Option[String],
                          userName: String,
                          isCategoryModification: Boolean = false
                        ): Future[Either[String, Unit]]

  def receiveDeleteProfile(globalCode: String, motive: DeletedMotive, labCodeInstanceOrigin: String, labCodeImmediateInstance: String, up: Boolean, userName:String): Future[Either[String, Unit]]

  def inferiorDeleteProfile(globalCode: SampleCode, motive: DeletedMotive, supUrl: String, userName:String): Unit

  def sendMatchToInferiorInstance(matchResult: MatchResult): Unit

  def receiveMatchFromSuperior(matchSuperiorInstance: MatchSuperiorInstance): Future[Either[Unit, Unit]]

  def convertStatus(matchId: String, firingCode: SampleCode, status: String, matchRes: MatchResult, onlyUpload: Boolean = false): Unit

  def receiveMatchStatus(matchId: String, firingCode: SampleCode, leftCode: SampleCode, rightCode: SampleCode, status: String, labOrigin: String, labImmediate: String): Future[Unit]

  def updateMatchSendStatus(id: String, targetLab: Option[String] = None, status: Option[Long], statusHitDiscard: Option[Long] = None, message: Option[String] = None): Future[Either[String, Unit]]

  def isInterconnectionMatch(matchResult: MatchResult): Boolean

  def isExternalMatch(matchResult: MatchResult): Boolean

  def isFromCurrentInstance(globalCode: SampleCode): Boolean

  def deleteMatch(matchId: String, firingCode: String): Unit

  def retry(): Unit

  def receiveFile(file: FileInterconnection): Future[Unit]

  def sendFiles(globalCode: String, targetLab: String): Unit

  def notify(notificationInfo: NotificationInfo, permission: Permission, usersToNotify: List[String] = Nil): Unit

  def wasMatchUploaded(matchResult: MatchResult, labImmediate: String): Boolean
}

@Singleton
class InterconnectionServiceImpl @Inject()(
                                            akkaSystem: ActorSystem = null,
                                            connectionRepository: ConnectionRepository,
                                            inferiorInstanceRepository: InferiorInstanceRepository,
                                            categoryRepository: CategoryRepository,
                                            superiorInstanceProfileApprovalRepository: SuperiorInstanceProfileApprovalRepository,
                                            client: WSClient,
                                            userService: UserService,
                                            roleService: RoleService,
                                            profileService: ProfileService,
                                            kitService: StrKitService,
                                            notificationService: NotificationService,
                                            @Named("protocol") val protocol: String,
                                            @Named("status") val status: String,
                                            @Named("categoryTreeCombo") val categoryTreeCombo: String,
                                            @Named("insertConnection") val insertConnection: String,
                                            @Named("localUrl") val localUrl: String,
                                            @Named("uploadProfile") val uploadProfile: String,
                                            @Named("labCode") val currentInstanceLabCode: String,
                                            profileDataService: ProfileDataService = null,
                                            categoryService: CategoryService = null,
                                            traceService: TraceService = null,
                                            matchingRepository: MatchingRepository = null,
                                            matchingService: MatchingService = null,

                                            @Named("defaultAssignee") val defaultNotificationReceiver: String = "tst-admin",
                                            @Named("timeOutOnDemand") val timeOutOnDemand: String = "1 seconds",
                                            @Named("timeOutQueue") val timeOutQueue: String = "1 seconds",
                                            @Named("timeActorSendRequestGet") val timeActorSendRequestGet: String = "1 seconds",
                                            @Named("timeActorSendRequestPutPostDelete") val timeActorSendRequestPutPostDelete: String = "1 seconds",
                                            @Named("timeOutHolder") val timeOutHolder: Int = 1000,
                                            cache: CacheService = null
                                          ) extends InterconnectionService {
  val defaultTimeoutQueue = akka.util.Timeout(Some(Duration(timeOutQueue)).collect { case d: FiniteDuration => d }.get)
  val defaultTimeoutOnDemand = akka.util.Timeout(Some(Duration(timeOutOnDemand)).collect { case d: FiniteDuration => d }.get)
  val sendRequestActorGlobal: ActorRef = akkaSystem.actorOf(SendRequestActor.props())
  val logger: Logger = Logger(this.getClass())
  val PENDIENTE_ENVIO = 1L
  val ENVIADA = 2L
  val RECHAZADA = 3L
  val APROBADA = 4L
  val PENDING_DELETE = 5L
  val DELETED_IN_SUP_INS = 6L
  val MATCH_SEND_PENDING = 7L
  val MATCH_SENT = 8L
  val HIT_SENT = 9L
  val DISCARD_SENT = 10L
  val HIT_PENDING = 11L
  val DISCARD_PENDING = 12L
  val DELETE_PENDING = 13L
  val DELETE_SENT = 14L
  val FILE_PENDING = 15L
  val FILE_SENT = 16L
  val REJECTED_THIS_INSTANCE_INF_INFORMED = 17L
  val APPROVED_THIS_INSTANCE_INF_INFORMED = 18L
  val DELETE_IN_SUP_INTSTANCE_PENDING_SEND_TO_INFERIOR = 19L
  val DELETE_IN_SUP_INTSTANCE_SENT_TO_INFERIOR = 20L
  val REJECTED_THIS_INSTANCE_PENDING_SEND_TO_INFERIOR = 21L
  val APPROVED_THIS_INSTANCE_PENDING_SEND_TO_INFERIOR = 22L
  val DELETED_IN_INF_INS = 23L
  val superiorLabCode = "SUPERIOR"

  override def getConnections(): Future[Either[String, Connection]] = {
    connectionRepository.getConnections()
  }

  override def updateConnections(connection: Connection): Future[Either[String, Connection]] = {

    connectionRepository.updateConnections(connection)

  }

  private def sendRequestOnDemand(holder: WSRequestHolder, body: String = "", timeoutParam: Timeout = defaultTimeoutOnDemand): Future[WSResponse] = {
    implicit val timeout = timeoutParam
    val sendRequestActor: ActorRef = akkaSystem.actorOf(SendRequestActor.props())
    (sendRequestActor ? (holder.withRequestTimeout(timeOutHolder), body, true, timeActorSendRequestGet)).mapTo[WSResponse]
  }

  private def sendRequestQueue(
                                holder: WSRequestHolder,
                                body: String = "",
                                timeoutParam: Timeout = defaultTimeoutQueue
                              ): Future[WSResponse] = {
    implicit val timeout: Timeout = timeoutParam
    (
      sendRequestActorGlobal ? (
        addHeadersURL(holder)
          .withRequestTimeout(timeOutHolder),
        body,
        false,
        timeActorSendRequestPutPostDelete
      )
      )
      .mapTo[WSResponse]
  }

  override def getConnectionsStatus(url: String): Future[Either[String, Unit]] = {

    try {
      val holder: WSRequestHolder = addHeadersURL(client.url(protocol + url + status))
      val futureResponse: Future[WSResponse] = this.sendRequestOnDemand(holder)
      futureResponse.flatMap(result => {
        if (result.status == 200) {
          Future.successful(Right(()))
        } else {
          Future.successful(Left(Messages("error.E0707")))
        }
      }).recoverWith {
        case e: akka.pattern.AskTimeoutException => {
          logger.debug(e.getMessage, e)
          Future.successful(Left(Messages("error.E0730")))
        }
        case e: Exception => {
          logger.debug(e.getMessage, e)
          Future.successful(Left(Messages("error.E0707")))
        }
      }
    } catch {
      case e: Exception => {
        logger.debug(e.getMessage, e)
        Future.successful(Left(Messages("error.E0707")))
      }
    }
  }

  private def addHeadersURL(holder: WSRequestHolder): WSRequestHolder = {
    holder.withHeaders(HeaderInsterconnections.url -> localUrl).withHeaders(HeaderInsterconnections.laboratoryImmediateInstance -> currentInstanceLabCode)
  }

  override def getCategoryConsumer: Future[Either[String, JsValue]] = {

    val connectionsUrl = connectionRepository.getSupInstanceUrl()
    connectionsUrl.flatMap {
      case None => Future.successful(Left("error.E0707"))
      case Some(connectionsUrl) => {

        val holder: WSRequestHolder = addHeadersURL(client.url(protocol + connectionsUrl + categoryTreeCombo))
        val futureResponse: Future[WSResponse] = holder.get()
        futureResponse.flatMap { result => {
          if (result.status == 200) {
            Future.successful(Right(result.json))
          } else {
            Future.successful(Left(Messages("error.E0707")))
          }
        }
        }.recoverWith {
          case e: Exception => {
            logger.error("", e)
            Future.successful(Left(Messages("error.E0707")))
          }
        }
      }
    }.recoverWith {
      case e: Exception => {
        logger.error("", e)
        Future.successful(Left(Messages("error.E0707")))
      }
    }

  }

  override def insertInferiorInstanceConnection(url: String, laboratory: String): Future[Either[String, Unit]] = {

    inferiorInstanceRepository.countByURL(url).flatMap {
      case 0 => {
        val existingRowF = inferiorInstanceRepository.findByLabCode(laboratory)
        existingRowF.flatMap { existingRow =>
          existingRow match {
            case None => {
              val result = inferiorInstanceRepository.insert(InferiorInstance(url = url, laboratory = laboratory))
              result.onSuccess {
                case _ => generarNotificacionInstancePending(url)
              }
              result.flatMap { x => {
                x.fold(errMsg => {
                  Future.successful(Left(errMsg))
                }, _ => Future.successful(Right(())))
              }
              }.recoverWith {
                case e: Exception => {
                  Future.successful(Left(e.getMessage))
                }
              }
            }
            case _ => {
              inferiorInstanceRepository.updateURL(InferiorInstance(url = url, laboratory = laboratory))
            }
          }
        }
      }
      case _ => Future.successful(Left(Messages("error.E0720")))
    }.recoverWith {
      case e: Exception => {
        logger.error("", e)
        Future.successful(Left(e.getMessage))
      }
    }

  }

  override def connect(): Future[Either[String, Unit]] = {
    connectionRepository.getSupInstanceUrl().flatMap {
      case None => Future.successful(Left(Messages("error.E0707")))
      case Some(urlSup) => {
        val holder: WSRequestHolder = addHeadersURL(client.url(protocol + urlSup + insertConnection))
        val futureResponse: Future[WSResponse] = this.sendRequestQueue(holder.withMethod("POST"))
        futureResponse.flatMap { result => {
          if (result.status == 200) {
            Future.successful(Right(()))
          } else {
            val node = play.libs.Json.parse(result.body).findValue("message")
            Future.successful(Left(node.asText(Messages("error.E0702"))))
          }
        }
        }.recoverWith {
          case e: Exception => {
            logger.error("", e)
            Future.successful(Left(Messages("error.E0707")))
          }
        }
      }
    }.recoverWith {
      case e: Exception => {
        logger.error("", e)
        Future.successful(Left(Messages("error.E0707")))
      }
    }
  }

  override def getAllInferiorInstances(): Future[Either[String, List[InferiorInstanceFull]]] = {
    inferiorInstanceRepository.findAll()
  }

  override def getAllInferiorInstanceStatus(): Future[Either[String, List[InferiorInstanceStatus]]] = {
    inferiorInstanceRepository.findAllInstanceStatus()
  }

  override def updateInferiorInstance(row: InferiorInstanceFull): Future[Either[String, Unit]] = {

    val update = inferiorInstanceRepository.update(row)
    update.onSuccess {
      case _ => solveNotification(row.url)
    }
    update
  }

  private def generarNotificacionInstancePending(urlInstancia: String) = {
    roleService.getRolePermissions().filter {
      case (_, permissions) =>
        permissions.contains(Permission.INF_INS_CRUD)
    }.foreach {
      case (role, _) =>
        userService.findUserAssignableByRole(role).foreach {
          admins =>
            admins.foreach { admin =>
              notificationService.push(admin.id, InferiorInstancePendingInfo(urlInstancia))
            }
            userService.sendNotifToAllSuperUsers(InferiorInstancePendingInfo(urlInstancia), admins.map(_.id))
        }
    }
  }

  override def notify(notificationInfo: NotificationInfo, permission: Permission, usersToNotify: List[String] = Nil): Unit = {

    userService.findUsersIdWithPermission(permission).map(usersIds => {
      val seqUsers =
        if (usersIds.isEmpty) {
          usersIds ++ Seq(defaultNotificationReceiver) ++ usersToNotify.toSeq
        } else {
          usersIds ++ usersToNotify.toSeq
        }
      val userSet = seqUsers.toSet
      userSet.foreach { userId: String => notificationService.push(userId, notificationInfo) }
      userService.sendNotifToAllSuperUsers(notificationInfo, userSet.toSeq)
    })

  }

  private def solveNotification(urlInstancia: String) = {
    roleService.getRolePermissions().filter {
      case (_, permissions) =>
        permissions.contains(Permission.INF_INS_CRUD)
    }.foreach {
      case (role, _) =>
        userService.findUserAssignableByRole(role).foreach {
          admins =>
            admins.foreach { admin =>
              notificationService.solve(admin.id, InferiorInstancePendingInfo(urlInstancia))
            }
        }
    }
  }

  private def solveNotificationProfileApproval(globalCode: String) = {
    roleService.getRolePermissions().filter {
      case (_, permissions) =>
        permissions.contains(Permission.INTERCON_NOTIF)
    }.foreach {
      case (role, _) =>
        userService.findUserAssignableByRole(role).foreach {
          admins =>
            admins.foreach { admin =>
              this.notificationService.solve(admin.id, ProfileUploadedInfo(SampleCode(globalCode)))
            }
        }
    }
  }

  private def doUploadProfileToSuperiorInstance(profile: Profile, pd: ProfileData, profileAssociated: Option[Profile] = None): Future[Either[String, Unit]] = {

    traceService.add(Trace(profile.globalCode, profile.assignee, new Date(), trace.ProfileInterconectionUploadInfo))
    val futureReturn = categoryService.getCategoriesMappingById(profile.categoryId).flatMap {
      case None => {
        logger.debug("No está mapeada la categoria de la instancia superior")
        this.profileDataService.updateUploadStatus(profile.globalCode.text, PENDIENTE_ENVIO, Option.empty[String], Some(s"No se puede enviar porque no esta mapeada la categoria ${profile.categoryId.text} en la instancia superior"), Option.empty[String])
        Future.successful(Left(Messages("error.E0721")))
      }
      case Some(idCategorySuperior) => {
        connectionRepository.getSupInstanceUrl().flatMap {
          case Some(supUrl) => {
            this.getConnectionsStatus(supUrl).flatMap {
              case Left(_) => {
                this.profileDataService.updateUploadStatus(profile.globalCode.text, PENDIENTE_ENVIO, Option.empty[String], Some(s"No se puede enviar porque no se pudo conectar con la instancia superior"), Option.empty[String])
                Future.successful(Left(Messages("error.E0723")))
              }
              case Right(_) => {
                var sampleEntryDateString = ""
                if (pd.sampleEntryDate.isDefined) {
                  sampleEntryDateString = String.valueOf(pd.sampleEntryDate.get.getTime)
                }
                val holder: WSRequestHolder = addHeadersURL(client.url(protocol + supUrl + uploadProfile))
                  .withHeaders("Content-Type" -> "application/json")
                  .withHeaders(HeaderInsterconnections.labCode -> pd.laboratory)
                  .withHeaders(HeaderInsterconnections.laboratoryOrigin -> currentInstanceLabCode)
                  .withHeaders(HeaderInsterconnections.sampleEntryDate -> sampleEntryDateString)

                val supProfile: Profile = profile.copy(categoryId = AlphanumericId(idCategorySuperior))
                val request = ProfileTransfer(supProfile, profileAssociated)
                val outputJson = Json.toJson(request)
                val outputJsonString = outputJson.toString
                val futureResponse: Future[WSResponse] = this.sendRequestQueue(holder.withMethod("POST"), outputJsonString)
                futureResponse.flatMap { result => {
                  if (result.status == 200) {
                    logger.debug("se envio correctamente el perfil a la instancia superior")
                    this.profileDataService.updateUploadStatus(profile.globalCode.text, ENVIADA, Option.empty[String],Option.empty[String], Option.empty[String])
                    sendFiles(profile.globalCode.text, superiorLabCode)
                    Future.successful(Right(()))
                  } else {
                    logger.debug("La instancia superior rechazo el perfil")
                    this.profileDataService.updateUploadStatus(profile.globalCode.text, PENDIENTE_ENVIO,Option.empty[String],Option.empty[String],Option.empty[String])
                    Future.successful(Left(Messages("error.E0724")))
                  }
                }
                }
              }
            }
          }
          case None => {
            this.profileDataService.updateUploadStatus(profile.globalCode.text, PENDIENTE_ENVIO, Option.empty[String], Some(s"No se puede enviar porque no se pudo conectar con la instancia superior"), Option.empty[String])
            Future.successful(Left(Messages("error.E0722")))
          }
        }
      }
    }.recoverWith {
      case e: Exception => {
        logger.error("Error de conexión con la instancia superior", e)
        this.profileDataService.updateUploadStatus(profile.globalCode.text, PENDIENTE_ENVIO,Option.empty[String],Some(s"No se puede enviar porque no se pudo conectar con la instancia superior"), Option.empty[String])
        Future.successful(Left(Messages("error.E0723")))
      }
    }
    futureReturn
  }

  def getProfileAssociatedCode(profile: Profile): Option[SampleCode] = {
    profile.labeledGenotypification.map(_.keySet.map(_.toString).map(x => Try(types.SampleCode(x))).filter(_.isSuccess).map(_.get)).flatMap(_.headOption)
  }

  def swap[T](o: Option[Future[T]]): Future[Option[T]] =
    o.map(_.map(Some(_))).getOrElse(Future.successful(None))

  def getProfileAssociated(profile: Profile): Future[Option[Profile]] = {
    FutureUtils.swap(getProfileAssociatedCode(profile).map(x => this.profileService.findByCode(x))).map(x => x.flatten).map(profileOpt => profileOpt.map(profile => profile.copy(internalSampleCode = profile.globalCode.text, matcheable = false, labeledGenotypification = None)))
  }

  override def uploadProfileToSuperiorInstance(profile: Profile, pd: ProfileData): Unit = {

    logger.debug("uploadProfileToSuperiorInstance" + profile._id + " " + profile.categoryId)

    val codeProfileAssociated = getProfileAssociatedCode(profile)
    codeProfileAssociated match {
      case None => {
        doUploadProfileToSuperiorInstance(profile.copy(internalSampleCode = profile.globalCode.text), pd.copy(internalSampleCode = profile.globalCode.text))
      }
      case Some(sampleCode) => {
        profileService.findByCode(sampleCode).flatMap(profileAssociated => {
          doUploadProfileToSuperiorInstance(profile.copy(internalSampleCode = profile.globalCode.text), pd.copy(internalSampleCode = profile.globalCode.text), profileAssociated.map(p => p.copy(internalSampleCode = p.globalCode.text, matcheable = false)))
        })
      }
    }

    ()
  }

  def kitToOptionKit(kit: String): Option[String] = {

    kit match {
      case "Manual" => None
      case _ => {
        val kitOrKitFromAlias = Await.result(kitService.listFull().map{
          list => {
            if(list.map(x => x.id).contains(kit)) {
              kit
            } else {
              list.filter(k => k.alias.contains(kit)).head.id
            }
          }
        },Duration(10, SECONDS))

        Some(kitOrKitFromAlias)
      }
    }

  }

  def validateAnalysis(profile: Profile): Future[scala.List[scala.Either[scala.List[String], Unit]]] = {
    val validations: List[Future[scala.Either[scala.List[String], Unit]]] = profile.analyses.get.map(analysis => {
      profileService.isExistingKit(analysis.kit).flatMap {
        case true => {
          var kitOpt = kitToOptionKit(analysis.kit)
          profileService.getAnalysisType(kitOpt, analysis.`type`).flatMap {
            at => {
              async {
                val locusValidation = await(profileService.validateExistingLocusForKit(analysis.genotypification, kitOpt))
                if (locusValidation.isLeft) {
                  locusValidation
                } else {
                  await(
                    profileService.validateAnalysis(analysis.genotypification, profile.categoryId,
                      kitOpt, profile.contributors.getOrElse(0), analysis.`type`, at).flatMap {
                      case Left(e) => Future.successful(Left("Kit: " + analysis.kit :: e))
                      case Right(_) => Future.successful(Right(()))
                    })
                }
              }

            }
          }
        }
        case false => Future.successful(Left(List(Messages("error.E0692", analysis.kit))))
      }
    })
    Future.sequence(validations)
  }

  private def importProfileValidatorOpt(profileOpt: Option[Profile]): Future[Either[String, Unit]] = {
    profileOpt match {
      case None => {
        Future.successful(Right(()))
      }
      case Some(profile) => {
        importProfileValidatorSingle(profile)
      }
    }

  }

  private def importProfileValidator(profile: Profile): Future[Either[String, Unit]] = {
    importProfileValidatorSingle(profile).flatMap {
      case Right(_) => {
        getProfileAssociated(profile).flatMap(p => importProfileValidatorOpt(p.map(pa => pa)))
      }
      case Left(m) => Future.successful(Left(m))
    }
  }

  private def importProfileValidatorSingle(profile: Profile): Future[Either[String, Unit]] = {

    //Validate Category
    if (!profileService.isExistingCategory(profile.categoryId)) {
      val msg = Messages("error.E0600", profile.categoryId.text)
      return Future.successful(Left(msg));
    }

    validateAnalysis(profile).map {
      lista => {
        if (lista.forall(x => x.isRight)) {
          Right(())
        } else {
          Left(lista.filter(x => x.isLeft).map(x => x.left.get.mkString(", ")).mkString("; "))
        }
      }
    }.recoverWith {
      case e: Exception => {
        logger.error(Messages("error.E0122", profile._id), e)
        Future.successful(Left(Messages("error.E0122", profile._id)))
      }
    }
  }

  override def importProfile(
                              profile: Profile,
                              labo: String,
                              sampleEntryDate: String,
                              labCodeInstanceOrigin: String,
                              labCodeInmediateInstance: String,
                              profileAssociated: Option[Profile] = None
                            ): Unit = {

    var sampleEntryDateOption: Option[java.sql.Date] = None
    if (sampleEntryDate != null && !sampleEntryDate.isEmpty) {
      sampleEntryDateOption = Some(new java.sql.Date(java.lang.Long.valueOf(sampleEntryDate)))
    }
    val newGenotipificationWithLocusKeys = profile
      .genotypification
      .map(
        x => {
          x.copy(_2=locusAliasToLocusId(x._2))
        }
      )
    val newAnalisisWithLocusKeys = profile
      .analyses
      .get
      .map(
        analysis => {
          analysis.copy(genotypification = locusAliasToLocusId(analysis.genotypification))
        }
      )
    val profileChanged1 = profile
      .copy(
        genotypification = newGenotipificationWithLocusKeys,
        analyses = Some(newAnalisisWithLocusKeys)
      )
    importProfileValidator(profileChanged1)
      .map {
        case Right(()) =>
          val newAnalisis = profileChanged1
            .analyses
            .get
            .map(
              analysis => {
                val kitOpt = kitToOptionKit(analysis.kit)
                if (kitOpt.isDefined) {
                  analysis.copy(kit = kitOpt.get)
                } else {
                  analysis
                }
              }
            )
          val profileChanged2 = profileChanged1.copy(analyses = Some(newAnalisis))
          superiorInstanceProfileApprovalRepository
            .upsert(
              SuperiorInstanceProfileApproval(
                id = 0L,
                globalCode = profile._id.text,
                profile = Json.toJson(profileChanged2).toString(),
                laboratory = labo,
                laboratoryInstanceOrigin = labCodeInstanceOrigin,
                laboratoryImmediateInstance = labCodeInmediateInstance,
                sampleEntryDate = sampleEntryDateOption,
                profileAssociated = profileAssociated.map(profile => Json.toJson(profile).toString())
              )
            )
          this.notify(
            ProfileUploadedInfo(profile.globalCode),
            Permission.INTERCON_NOTIF
          )
        case Left(error) => {
          superiorInstanceProfileApprovalRepository
            .upsert(
              SuperiorInstanceProfileApproval(
                id = 0L,
                globalCode = profile._id.text,
                profile = Json.toJson(profile).toString(),
                laboratory = labo,
                laboratoryInstanceOrigin = labCodeInstanceOrigin,
                laboratoryImmediateInstance = labCodeInmediateInstance,
                sampleEntryDate = sampleEntryDateOption,
                errors = Some(error),
                profileAssociated = profileAssociated.map(profile => Json.toJson(profile).toString())
              )
            )
          this.notify(
            ProfileUploadedInfo(profile.globalCode),
            Permission.INTERCON_NOTIF
          )
        }
      }
    ()
  }

  private def locusAliasToLocusId(genotipification: Profile.Genotypification): Profile.Genotypification = {
    val locusAlias = Await.result(kitService.getLocusAlias,Duration(10,SECONDS))
    // locusAlias keys are the alias
    val newGenotipification = genotipification.map(marker => {
      if(locusAlias.keys.toList.contains(marker._1)) {
        marker.copy(_1=locusAlias.get(marker._1).get.toString())
      } else{
        marker
      }
    })
    newGenotipification
  }

  private def existProfileData(globalCode: SampleCode): Future[Boolean] = {
    profileDataService.findByCodeWithoutDetails(globalCode).map {
      case None => false
      case Some(_) => true
    }
  }

  def insertOrUpdateProfile(
                             profile: Profile,
                             laboratoryInstanceOrigin: String,
                             laboratoryImmediateInstance: String,
                             laboratory: String,
                             profileAssociated: Option[Profile],
                             allowFromOtherInstances: Boolean = false
                           ): Future[SampleCode] = {
    (getProfileAssociatedCode(profile), profileAssociated) match {
      case (None, None) => {
        this.insertOrUpdateProfileSingle(
          profile,
          laboratoryInstanceOrigin,
          laboratoryImmediateInstance,
          laboratory,
          allowFromOtherInstances = allowFromOtherInstances
        )
      }
      case (Some(associatedProfileCode), Some(associatedProfile)) => {
        this.insertOrUpdateProfileSingle(
          associatedProfile,
          laboratoryInstanceOrigin,
          laboratoryImmediateInstance,
          laboratory,
          allowFromOtherInstances = allowFromOtherInstances
        ).flatMap(result => {
          this.insertOrUpdateProfileSingle(
            profile,
            laboratoryInstanceOrigin,
            laboratoryImmediateInstance,
            laboratory,
            allowFromOtherInstances = allowFromOtherInstances
          )
        })
      }
      case _ => {
        this.insertOrUpdateProfileSingle(
          profile.copy(
            labeledGenotypification = None
          ),
          laboratoryInstanceOrigin,
          laboratoryImmediateInstance,
          laboratory,
          allowFromOtherInstances = allowFromOtherInstances
        )
      }
    }
  }

  def insertOrUpdateProfileSingle(
                                   profile: Profile,
                                   laboratoryInstanceOrigin: String,
                                   laboratoryImmediateInstance: String,
                                   laboratory: String,
                                   allowFromOtherInstances: Boolean = false
                                 ): Future[SampleCode] = {

    this.existProfileData(profile.globalCode).flatMap {
      case true =>
        if (isFromCurrentInstance(profile.globalCode)) {
          Future.successful(profile.globalCode)
        } else {
          val pda = ProfileDataAttempt(
            category = profile.categoryId,
            attorney = None,
            bioMaterialType = None,
            court = None,
            crimeInvolved = None,
            crimeType = None,
            criminalCase = None,
            internalSampleCode = profile.internalSampleCode,
            assignee = profile.assignee,
            laboratory = Some(laboratory),
            responsibleGeneticist = None,
            profileExpirationDate = None,
            sampleDate = None,
            sampleEntryDate = None,
            dataFiliation = None
          )
          profileDataService
            .updateProfileData(
              profile.globalCode,
              profileData = pda,
              allowFromOtherInstances = allowFromOtherInstances
            )
          profileService.updateProfile(profile)
        }
      case false =>
        profileDataService.importFromAnotherInstance(
          ProfileData(
            category = profile.categoryId,
            globalCode = profile.globalCode,
            internalSampleCode = profile.internalSampleCode,
            assignee = profile.assignee,
            laboratory = laboratory,
            deleted = false,
            attorney = None,
            bioMaterialType = None,
            court = None,
            crimeInvolved = None,
            crimeType = None,
            criminalCase = None,
            deletedMotive = None,
            responsibleGeneticist = None,
            profileExpirationDate = None,
            sampleDate = None,
            sampleEntryDate = None,
            dataFiliation = None,
            isExternal = true
          ), laboratoryInstanceOrigin, laboratoryImmediateInstance
        ).flatMap(_ => profileService.addProfile(profile)).recoverWith {
          case ex: Exception => {
            logger.error("" + ex.getMessage, ex)
            if (ex.getMessage != null && ex.getMessage.contains("duplicate")) {
              Future.successful(profile.globalCode)
            } else {
              Future.failed(ex)
            }
          }
        }
    }
  }
// TODO: agregar el usuario
  def approveProfile(
                      profileApproval: ProfileApproval,
                      isCategoryModification: Boolean = false
                    ): Future[Either[String, SampleCode]] = {
    superiorInstanceProfileApprovalRepository
      .findByGlobalCode(profileApproval.globalCode)
      .flatMap {
        case Left(e) => Future.successful(Left(e))
        case Right(row) => {
          val profile = Json.fromJson[Profile](Json.parse(row.profile)).get
          val profileAssociated = row
            .profileAssociated
            .map(profile => Json.fromJson[Profile](Json.parse(profile)).get)
          importProfileValidator(profile)
            .flatMap {
              case Right(()) => {
                insertOrUpdateProfile(
                  profile,
                  row.laboratoryInstanceOrigin,
                  row.laboratoryImmediateInstance,
                  row.laboratory,
                  profileAssociated.map(
                    p => p.copy(
                      labeledGenotypification = None,
                      matcheable = false
                    )
                  ),
                  allowFromOtherInstances = true
                )
                  .flatMap {
                    sampleCode => {
                      // disparar el proceso de match
                      //                Right(profileService.fireMatching(sampleCode))
                      // notificar a la instancia inferior que la superior le aprobó el perfil
                      this.notifyChangeStatus(
                        row.globalCode,
                        row.laboratoryImmediateInstance,
                        APROBADA,
                        isCategoryModification = isCategoryModification
                      ).flatMap {
                        _ => superiorInstanceProfileApprovalRepository
                          .delete(row.globalCode)
                      }
                    }
                  }.recoverWith {
                    case e: Exception => {
                      logger.error("Error al importar el perfil ", e)
                      superiorInstanceProfileApprovalRepository.upsert(
                        SuperiorInstanceProfileApproval
                        (id = 0L,
                          globalCode = row.globalCode,
                          profile = row.profile,
                          laboratory = row.laboratory,
                          laboratoryInstanceOrigin = row.laboratoryInstanceOrigin,
                          laboratoryImmediateInstance = row.laboratoryImmediateInstance,
                          sampleEntryDate = row.sampleEntryDate,
                          errors = Some(e.getMessage),
                          receptionDate = row.receptionDate,
                          profileAssociated = row.profileAssociated)).map { _ => Left(e.getMessage) }
                    }
                  }
              }
              case Left(error) => {
                logger.error("Error al importar el perfil " + error)

                superiorInstanceProfileApprovalRepository.upsert(
                  SuperiorInstanceProfileApproval
                  (id = 0L,
                    globalCode = row.globalCode,
                    profile = row.profile,
                    laboratory = row.laboratory,
                    laboratoryInstanceOrigin = row.laboratoryInstanceOrigin,
                    laboratoryImmediateInstance = row.laboratoryImmediateInstance,
                    sampleEntryDate = row.sampleEntryDate,
                    errors = Some(error),
                    profileAssociated = row.profileAssociated)).map { _ => Left(error) }
              }
            }
        }
      }.map {
        case Left(msg) => Left(profileApproval.globalCode + ": " + msg)
        case Right(_) => Right(SampleCode(profileApproval.globalCode))
      }
  }

  def rejectProfile(
                     profileApproval: ProfileApproval,
                     motive: String,
                     idMotive: Long,
                     user: String
                   ): Future[Either[String, Unit]] = {
    superiorInstanceProfileApprovalRepository
      .findByGlobalCode(profileApproval.globalCode)
      .flatMap {
        case Right(p) =>
          val modSetup = getModificationSetup(profileApproval)
          modSetup
            .flatMap(
              setup =>
                this
                  .notifyChangeStatus(
                    p.globalCode,
                    p.laboratoryImmediateInstance,
                    RECHAZADA,
                    Some(motive),
                    Some(user),
                    setup.isCategoryUpdated()
                  )
                  .map( _ => (p, setup) )
            )
            .flatMap {
              case (p, setup) =>
                if (setup.isCategoryUpdated()) {
                  traceService
                    .add(
                      Trace(
                        setup.globalCode,
                        setup.assignee,
                        new Date(),
                        SuperiorCategoryChangeRejectedInfo
                      )
                    )
                } else {
                  Future.successful(())
                }
            }
            .flatMap {
              _ => {
                val futureResponse = superiorInstanceProfileApprovalRepository
                  .deleteLogical(
                    p.globalCode,
                    Some(user),
                    Some(
                      new java.sql.Timestamp(
                        Calendar
                          .getInstance()
                          .getTime
                          .getTime
                      )
                    ),
                    Some(idMotive),
                    Some(motive)
                  )
                futureResponse
                  .onSuccess(
                    { case _ => this.solveNotificationProfileApproval(p.globalCode) }
                  )
                futureResponse
              }
            }
        case Left(msg) => Future.successful(Left(msg))
      }
  }

  override def getPendingProfiles(profileApprovalSearch: ProfileApprovalSearch): Future[List[PendingProfileApproval]] = {
    superiorInstanceProfileApprovalRepository
      .findAll(profileApprovalSearch)
      .flatMap(listPendingProfiles => {
        Future.sequence(
          listPendingProfiles.map(
            pendingProfile => {
              this
                .profileService
                .getElectropherogramsByCode(SampleCode(pendingProfile.globalCode))
                .map(electros => { pendingProfile.copy(hasElectropherogram = !electros.isEmpty) })
                .flatMap(
                  pendingProfile => {
                    this
                      .profileService
                      .getFilesByCode(SampleCode(pendingProfile.globalCode))
                      .map(files => { pendingProfile.copy(hasFiles = !files.isEmpty) })
                  }
                )
            }
          )
        )
      })
  }

  override def getTotalPendingProfiles(): Future[Long] = {
    superiorInstanceProfileApprovalRepository.getTotal()
  }

  private def traceImportedProfile(
                                    setup: ProfileCategoryModificationSetup
                                  ): ProfileCategoryModificationSetup = {
    setup match {
      case ProfileCategoryModificationSetup(code, _, _, assignee, _, Some(Right(_))) =>
        if (!setup.isCategoryUpdated()) {
          val traceInfo = ProfileImportedFromInferiorInfo
          traceService.add(
            Trace(
              code,
              assignee,
              new Date(),
              traceInfo
            )
          )
        }
      case _ => ()
    }
    setup
  }

  private def traceApprovedProfileWithDifferentCategory(
                                                         setup: ProfileCategoryModificationSetup
                                                       ) = {
    setup match {
      case ProfileCategoryModificationSetup(
      globalCode, Some(cCat), nCat, assignee, _, Some(Right(_))
      ) =>
        if (nCat.text != cCat.text) {
          val traceInfo = SuperiorInstanceCategoryModificationInfo(
            cCat.text,
            nCat.text
          )
          val _ = traceService.add(
            Trace(
              globalCode,
              assignee,
              new Date(),
              traceInfo
            )
          )
        }
      case _ => ()
    }
    setup
  }

  private def uploadToSuperiorInstanceIfCategoryIsModified(
                                                            setup: ProfileCategoryModificationSetup
                                                          ): ProfileCategoryModificationSetup = {
    def getProfiles(prOpt: Option[Profile], globalCode: SampleCode):
    Future[Either[String, (Profile, Option[ProfileData], Option[Profile])]] = {
      prOpt match {
        case Some(profile) =>
          for {
            pd <- profileDataService.get(globalCode)
            associatedProfile <- getProfileAssociated(profile)
              .map(
                pOpt => pOpt.map(
                  p => p.copy(
                    internalSampleCode = p.globalCode.text,
                    labeledGenotypification = None,
                    matcheable = false
                  )
                )
              )
          } yield {
            Right((profile, pd, associatedProfile))
          }
        case None =>
          Future.successful(Left(Messages("error.E0109")))
      }
    }
    def uploadProfile(
                       profiles: Either[String, (Profile, Option[ProfileData], Option[Profile])]
                     ): Future[Either[String, Unit]] = {
      profiles match {
        case Right((profile, Some(pd), associatedProfile)) =>
          doUploadProfileToSuperiorInstance(
            profile.copy(
              internalSampleCode = profile.globalCode.text
            ),
            pd.copy(
              internalSampleCode = profile.globalCode.text
            ),
            associatedProfile
          )
        case Left(msg) => Future.successful(Left(msg))
        case _ => Future.successful(Left(Messages("error.E0109")))
      }
    }
    setup match {
      case ProfileCategoryModificationSetup(
      globalCode, Some(cCat), nCat, _, _, Some(Right(_))
      ) =>
        if (cCat.text != nCat.text) {
          val forbiddenStatusToUpload = Seq(PENDING_DELETE, DELETED_IN_SUP_INS)
          profileDataService
            .getProfileUploadStatusByGlobalCode(globalCode)
            .flatMap {
              case Some(status) if !forbiddenStatusToUpload.contains(status) => {
                profileService
                  .get(globalCode)
                  .flatMap { getProfiles (_, globalCode)}
                  .flatMap { uploadProfile }
              }
              case _ => Future.successful(Right(()))
            }
        } else {
          Future.successful(Right(()))
        }
      case _ => Future.successful(Right(()))
    }
    // Todo: Error messages are generated by discarded. Should be returned.
    //       Mayber ProfileCategoryModificationSetup should be changed to
    //       include the error messages.
    setup
  }

  private def getModificationSetup(
                                    profileApproval: ProfileApproval
                                  ): Future[ProfileCategoryModificationSetup] = {
    val globalCode = profileApproval.globalCode
    for {
      oldProfile <- profileDataService
        .get(SampleCode(globalCode))
      newProfile <- superiorInstanceProfileApprovalRepository
        .buildUploadedProfile(globalCode)
    } yield {
      ProfileCategoryModificationSetup(
        newProfile.get.globalCode,
        oldProfile.map(_.category),
        newProfile.get.categoryId,
        newProfile.get.assignee,
        profileApproval,
        None
      )
    }
  }

  private def approveModificationSetup(
                                        setup: ProfileCategoryModificationSetup
                                      ): Future[ProfileCategoryModificationSetup] = {
    for {
      profileResult <- approveProfile(
        setup.profileApproval,
        setup.isCategoryUpdated()
      )
    } yield {
      setup.copy(approvalResult = Some(profileResult))
    }
  }

  private def solveApprovalNotification(
                                         setup: ProfileCategoryModificationSetup
                                       ): ProfileCategoryModificationSetup = {
    setup match {
      case ProfileCategoryModificationSetup(
      globalCode, _, _, _, _, Some(Right(_))
      ) => solveNotificationProfileApproval(globalCode.text)
      case _ => ()
    }
    setup
  }

  private def launchFindMatches(
                                 setup: ProfileCategoryModificationSetup
                               ): ProfileCategoryModificationSetup = {
    setup match {
      case ProfileCategoryModificationSetup(
      globalCode, _, _, _, _, Some(Right(_))
      ) => profileService.fireMatching(globalCode)
      case _ => ()
    }
    setup
  }

  //Se fija si es cambio de categoría
  override def approveProfiles(
                                profileApprovals: List[ProfileApproval]
                              ): Future[Either[String, Unit]] = {
    val extractErrors:
      ProfileCategoryModificationSetup => Either[String, Unit] = {
      setup =>
        setup.approvalResult match {
          case Some(Left(msg)) => Left(msg)
          case _               => Right(())
        }
    }
    val compileErrors:
      List[Either[String, Unit]] => Either[String, Unit] = {
      errors =>
        if (errors.forall(_.isRight)) {
          Right(())
        } else {
          Left(
            errors.filter(_.isLeft)
              .map(erroneo => erroneo.left.get)
              .mkString(start = "[", sep = ",", end = "]")
          )
        }
    }
    Future
      .sequence(profileApprovals.map(getModificationSetup))
      .map(_.map(approveModificationSetup))
      .flatMap( Future.sequence(_) )
      .map(_.map(traceImportedProfile))
      .map(_.map(traceApprovedProfileWithDifferentCategory))
      .map(_.map(solveApprovalNotification))
      .map(_.map(uploadToSuperiorInstanceIfCategoryIsModified))
      .map(_.map(launchFindMatches))
      .map(_.map(extractErrors))
      .map(compileErrors)
  }

  private def getLabUrl(lab: String): Future[Option[String]] = {
    if (lab == superiorLabCode) {
      this.connectionRepository.getSupInstanceUrl().map {
        case None => None
        case Some("") => None
        case Some(url) => Some(url)
      }
    } else {
      this.inferiorInstanceRepository.findByLabCode(lab).map(ii => ii.map(_.url))
    }
  }

  def notifyChangeStatus(
                          globalCode: String,
                          labCode: String,
                          status: Long,
                          motive: Option[String] = None,
                          userName: Option[String] = None,
                          isCategoryModification:Boolean = false
                        ): Future[Unit] = {
    Future {
      //Se ejecuta cada vez que se aprueba o rechaza un perfil. Modificar de modo tal que si el status es
      inferiorInstanceRepository
        .findByLabCode(labCode)
        .flatMap {
          case None => Future.successful(Right(()))
          case Some(inferiorInstance) => {
            val holder: WSRequestHolder =
              if (motive.isEmpty) {
                client.url(protocol + inferiorInstance.url + "/inferior/profile/status")
                  .withQueryString("globalCode" -> globalCode)
                  .withQueryString("status" -> String.valueOf(status))
                  .withQueryString("userName" -> userName.get)
                  .withQueryString("isCategoryModification" -> isCategoryModification.toString)
              }
              else {
                client.url(protocol + inferiorInstance.url + "/inferior/profile/status")
                  .withQueryString("globalCode" -> globalCode)
                  .withQueryString("status" -> String.valueOf(status))
                  .withQueryString("motive" -> motive.get)
                  .withQueryString("userName" -> userName.get)
                  .withQueryString("isCategoryModification" -> isCategoryModification.toString)
              }
            val futureResponse: Future[WSResponse] = this.sendRequestQueue(holder.withMethod("PUT"))
            futureResponse.flatMap { result => {
              if (result.status == 200) {
                logger.debug("se actualizó correctamente el status del perfil en la instancia inferior")
                Future.successful(Right(()))
              } else {
                logger.debug(Messages("error.E0710. El result.status es " + result.status))
                Future.successful(Left(Messages("error.E0710")))
              }
            }
            }.recoverWith {
              case _: Exception => Future.successful(Left(Messages("error.E0710")))
            }
          }
        }
    }
    Future.successful(())
  }

  override def uploadProfile(globalCode: String): Future[Either[String, Unit]] = {
    val sampleCode = SampleCode(globalCode)
    profileService
      .get(sampleCode)
      .flatMap {
        prof => {
          prof match {
            case Some(profile) => {
              profileDataService
                .get(sampleCode)
                .flatMap {
                  pd => {
                    pd match {
                      case Some(pd) => {
                        categoryService.getCategory(pd.category) match {
                          case Some(category) => {
                            if (category.replicate) {
                              getProfileAssociatedCode(profile) match {
                                case None => {
                                  this.doUploadProfileToSuperiorInstance(
                                    profile.copy(
                                      internalSampleCode = profile.globalCode.text
                                    ),
                                    pd.copy(
                                      internalSampleCode = profile.globalCode.text
                                    )
                                  )
                                }
                                case Some(sampleCode) => {
                                  profileService
                                    .findByCode(sampleCode)
                                    .flatMap(
                                      profileAssociated => {
                                        this.doUploadProfileToSuperiorInstance(
                                          profile.copy(internalSampleCode = profile.globalCode.text),
                                          pd.copy(internalSampleCode = profile.globalCode.text),
                                          profileAssociated
                                            .map(
                                              p => p.copy(
                                                internalSampleCode = p.globalCode.text,
                                                labeledGenotypification = None,
                                                matcheable = false
                                              )
                                            )
                                        )
                                      })
                                }
                              }
                            } else {
                              Future.successful(Left(Messages("error.E0725")))
                            }
                          }
                          case None => {
                            Future.successful(Left(Messages("error.E0666")))
                          }
                        }
                      }
                      case None => {
                        Future.successful(Left(Messages("error.E0109")))
                      }
                    }
                  }
                }
            }
            case None => {
              Future.successful(Left(Messages("error.E0109")))
            }
          }
        }
      }
  }

  def updateUploadStatus(
                          globalCode: String,
                          status: Long,
                          motive: Option[String] = None,
                          userName: String,
                          isCategoryModification:Boolean = false
                        ): Future[Either[String, Unit]] = {
    val profileData = Await
      .result(
        profileDataService.findByCode(SampleCode(globalCode)),
        Duration.Inf
      )
    profileData match {
      case Some(p) =>
        logger.info("1. Encontro el perfil")
        //enviar notificacion al assignee y grabar en la auditoria
        status match {
          case RECHAZADA =>
            logger.info("2. es rechazada, envio notif de rechazo")
            this.notify(
              RejectedProfileInfo(
                SampleCode(globalCode),
                userName,
                Some(isCategoryModification)
              ),
              Permission.INTERCON_NOTIF,
              List(p.assignee)
            )
            logger.info("3. Notif enviada, voy a guardar trazabilidad de rechazo")
            val c_trace: TraceInfo =  // Explicit type annotation here!
              if (isCategoryModification) {
                trace.CategoryChangeRejectedInSupInfo
              } else {
                // create an instance passing motive
                ProfileRejectedInSuperiorInfo(motive.getOrElse("Motivo no especificado"))
              }
            traceService.add(
              Trace(
                SampleCode(globalCode),
                userName,
                new Date(),
                c_trace
              )
            )
            logger.info("4. Trazabilidad guardada")
          case PENDING_DELETE =>  // Add this case
            logger.info("2. Status is pending delete")
          //Agregar trazabilidad de pendiente de baja
          case APROBADA =>
            logger.info("2. es aprobada, envio notif de aprobada")
            this.notify(
              AprovedProfileInfo(SampleCode(globalCode), userName, Some(isCategoryModification)),
              Permission.INTERCON_NOTIF,
              List(p.assignee)
            )
            logger.info("3. Notif enviada, voy a guardar trazabilidad de aprobada")
            val c_trace: TraceInfo = if (isCategoryModification) {
              trace.ProfileCategoryChangeAprovedInSuperiorInfo
            } else {
              trace.ProfileAprovedInSuperiorInfo
            }
            traceService.add(
              Trace(
                SampleCode(globalCode),
                userName,
                new Date(),
                c_trace
              )
            )
            logger.info("4. Trazabilidad guardada")
        }
      case _ => // Add default case to handle unexpected values
        logger.warn(s"Unexpected status value: $status")
      case None => ()
    }
    profileDataService.updateUploadStatus(globalCode, status, motive, Option.empty[String], Some(userName))
  }

  // Se ejecuta cada vez que se recibe la notificación del borrado de un perfil en una instancia inferior (up:true) o en una superior(up:false)
  override def receiveDeleteProfile(globalCode: String, motive: DeletedMotive, labCodeInstanceOrigin: String, labCodeImmediateInstance: String, up: Boolean, userName: String): Future[Either[String, Unit]] = {
    // Si se va notificar el borrado de un perfil desde una instancia inferior (up: quiere decir que el pedido viene de una instanica infeiror)
    // Hay que hacer un update en PROFILE_RECEIVED cambiando el estado a 6
    if (up) {
      logger.info(s"ReceiveDeleteProfile from inferior instance called for globalCode: $globalCode, labCodeInstanceOrigin: $labCodeInstanceOrigin, labCodeImmediateInstance: $labCodeImmediateInstance")
      // Hago el update de la tabla PROFILE_RECEIVED con status 6 (Notificación de eliminación recibida en instancia superior)
      this.profileDataService.updateProfileReceivedStatus(labCodeInstanceOrigin, globalCode, DELETED_IN_INF_INS, s"Usuario: $userName. Motivo: ${motive.motive}.", interconnection_error = "", Some(userName))
      // Agregar al trace del perfil que fue eliminado en la instancia inferior
      val c_trace: TraceInfo = trace.InterconnectionDeletedInInferiorInfo(Option(motive.motive).getOrElse("Motivo no especificado"))
      traceService.add(Trace(SampleCode(globalCode), userName, new Date(), c_trace)).map { _ => Right(()) }
    }
    else {
      // Recibo un delete de un perfil de la instancia superior
      // Debo actualizar el estado en PROFILE_UPLOADED a 20: Instancia inferior notificada de la eliminación del perfil en la instancia superior
      logger.info(s"ReceiveDeleteProfile from superior instance called for globalCode: $globalCode, labCodeInstanceOrigin: $labCodeInstanceOrigin, labCodeImmediateInstance: $labCodeImmediateInstance")
      this.profileDataService.updateUploadStatus(globalCode, DELETE_IN_SUP_INTSTANCE_SENT_TO_INFERIOR, Some(s"Usuario: $userName. Motivo: ${motive.motive}."),Some("") , Some(userName))
    }
  }




  def deleteApproval(globalCode: String,
                     rejectionUser: Option[String] = None,
                     rejectionDate: Option[java.sql.Timestamp] = None,
                     idRejectMotive: Option[Long] = None,
                     rejectMotive: Option[String] = None): Future[Either[String, Unit]] = {
    async {
      await(superiorInstanceProfileApprovalRepository.findByGlobalCode(globalCode)) match {
        case Left(_) => Right(())
        case Right(_) => await(superiorInstanceProfileApprovalRepository.deleteLogical(globalCode
          , rejectionUser
          , rejectionDate
          , idRejectMotive
          , rejectMotive))
      }
    }
  }

  def inferiorDeleteProfile(globalCode: SampleCode, motive: DeletedMotive, supUrl: String,userName: String): Unit = {
    doInferiorDeleteProfile(globalCode, motive, supUrl,userName)
    ()
  }

  def sendDeletionToSuperiorInstance(globalCode: SampleCode, motive: DeletedMotive, supUrl: String, userName: String): Future[Either[String, Unit]] = {

    sendDeletionToInstance(globalCode, motive, protocol + supUrl, currentInstanceLabCode, currentInstanceLabCode, superiorLabCode, true, userName)

  }

  // Si el borrado es en una instancia superior, envío el delete a la instancia inferior? NO LO ESTOY USANDO
  /*def sendDeletionToAllInferiorInstancesExceptPrevious(globalCode: SampleCode, motive: DeletedMotive, laboratoryOrigin: String): Future[Either[String, Unit]] = {
    inferiorInstanceRepository.findAll().flatMap {
      case Left(l) => Future.successful(Left(l))
      case Right(inferiorInstances) => {
        val inferiorInstancesToSend = inferiorInstances.filter(_.laboratory != laboratoryOrigin)
        Future.sequence(inferiorInstancesToSend.map(inferiorInstanceFull => {
          sendDeletionToInstance(globalCode, motive, protocol + inferiorInstanceFull.url, laboratoryOrigin, superiorLabCode, inferiorInstanceFull.laboratory, false)
            .recoverWith {
              case _: Exception => Future.successful(Left("No se pudo notificar el status a la instancia inferior"))
            }
        })).map(list => {
          if (list.forall(_.isRight)) {
            Right(())
          } else {
            Left("No se pudo notificar el status a la instancia inferior")
          }
        })
      }
    }
  }*/

  def sendDeletionToInstance(globalCode: SampleCode, motive: DeletedMotive, url: String, laboratoryOrigin: String, laboratoryImmediateInstance: String, labCode: String, up: Boolean, userName:String): Future[Either[String, Unit]] = {
    profileDataService.updateProfileSentStatus(globalCode.text, PENDING_DELETE, Some(s"Usuario: ${motive.solicitor}. Motivo: ${motive.motive}."), labCode, Option.empty[String], Some(userName)).flatMap {
      case Left(l) => Future.successful(Left(l))
      case Right(_) => this.doSendDeletionToInstance(globalCode, motive, url, laboratoryOrigin, laboratoryImmediateInstance, labCode, up, userName).recoverWith {
        case ex: Exception =>
          logger.error(s"Error during doSendDeletionToInstance for $globalCode at $url", ex)
          // Return a Left with an error message but DO NOT modify the motive in profile_uploaded table
          Future.successful(Left("Failed to notify deletion due to: " + ex.getMessage))
      }
    }
  }

  def doSendDeletionToInstance(globalCode: SampleCode, motive: DeletedMotive, url: String, laboratoryOrigin: String, laboratoryImmediateInstance: String, labCode: String, up: Boolean, userName: String): Future[Either[String, Unit]] = {
    logger.debug(s"Attempting to delete profile ${globalCode.text} at URL: $url") // Added logging
    val deleteUrl = if (up) {
      url + "/superior/profile/" + globalCode.text + "/" + userName
    } else {
      url + "/inferior/profile/" + globalCode.text + "/" + userName
    }
    async {
      val holder: WSRequestHolder = addHeadersURL(client.url(deleteUrl))
        .withHeaders("Content-Type" -> "application/json")
        .withHeaders(HeaderInsterconnections.labCode -> currentInstanceLabCode)
        .withHeaders(HeaderInsterconnections.laboratoryOrigin -> laboratoryOrigin)
        .withHeaders(HeaderInsterconnections.laboratoryImmediateInstance -> laboratoryImmediateInstance)
        .withBody(InMemoryBody(Json.toJson(motive).toString().getBytes))

      logger.debug(s"Sending DELETE request to: $deleteUrl")
      val result = await(this.sendRequestQueue(holder.withMethod("DELETE")))

      logger.debug(s"DELETE request to $deleteUrl returned status: ${result.status}")

      if (result.status == play.api.http.Status.OK) {
        logger.debug(s"DELETE request to $deleteUrl was successful")
        if (labCode == superiorLabCode) {
          await(profileDataService.updateUploadStatus(globalCode.text, DELETED_IN_SUP_INS, Some(motive.motive), Some(s"Instancia $deleteUrl notificada de la baja"), Some(userName)))
        } else {
          await(profileDataService.updateProfileSentStatus(globalCode.text, DELETED_IN_SUP_INS, Some(motive.motive), labCode, Some(s"Instancia $deleteUrl notificada de la baja"), Some(userName)))
        }
        Right(())
      } else if (result.status == play.api.http.Status.BAD_REQUEST) {
        logger.warn(s"DELETE request to $deleteUrl returned BAD_REQUEST")
        val node = play.libs.Json.parse(result.body).findValue("message")
        val errorMessage = node.asText(s"No se pudo notificar el borrado a la instancia $deleteUrl")
        logger.warn(s"BAD_REQUEST error message: $errorMessage")
        Left(errorMessage)
      } else {
        logger.error(s"DELETE request to $deleteUrl failed with status: ${result.status} and body: ${result.body}")
        Left(s"No se pudo notificar el borrado a la instancia: $url")
      }
    }
  }

  def doInferiorDeleteProfile(globalCode: SampleCode, motive: DeletedMotive, supUrl: String, userName: String): Future[Unit] = {
    async {
      val statusProfileUploaded = await(this.profileDataService.getProfileUploadStatusByGlobalCode(globalCode))
      val shouldDeleteProfileOnSupInst = statusProfileUploaded match {
        case Some(PENDIENTE_ENVIO) => false
        case Some(ENVIADA) => true
        case Some(RECHAZADA) => false
        case Some(APROBADA) => true
        case Some(PENDING_DELETE) => true
        case Some(_) => false
        case None => false
      }
      if (shouldDeleteProfileOnSupInst) {
        await(this.profileDataService.updateUploadStatus(globalCode.text, PENDING_DELETE, Some(motive.motive), Option.empty[String], Some(userName)))
        sendDeletionToSuperiorInstance(globalCode, motive, supUrl,userName).map {
          case Left(m) => {
            logger.info(s"Error sending deletion to superior instance: $m")
            this.profileDataService.updateUploadStatus(globalCode.text, PENDING_DELETE, Some(motive.motive), Some(s"Instancia $currentInstanceLabCode no pudo notificar la baja a la instancia superior: $m"), Some(userName))
          }
          case _ => {
            this.profileDataService.updateUploadStatus(globalCode.text, DELETED_IN_SUP_INS, Some(motive.motive) ,Option.empty[String], Some(userName))
          }
        }
      }
    }
  }

  override def sendMatchToInferiorInstance(matchResult: MatchResult): Unit = {
    //    if(!matchResult.leftProfile.globalCode.text.contains(currentInstanceLabCode)||
    //      !matchResult.rightProfile.globalCode.text.contains(currentInstanceLabCode)){
    doSendMatchToInferiorInstance(matchResult)
    //    }
    ()
  }

  def sendRequestMatch(outputJsonString: String, url: String, laboratory: String, matchId: String, listProfiles: Seq[String]): Future[scala.Either[String, Unit]] = {
    this.getConnectionsStatus(url).flatMap {
      case Left(_) => Future.successful(Left("No se pudo enviar el match a la instancia inferior"))
      case Right(_) => {
        val holder: WSRequestHolder = client.url(protocol + url + "/inferior/match/")
          .withHeaders("Content-Type" -> "application/json")
        val futureResponse: Future[WSResponse] = this.sendRequestQueue(holder.withMethod("POST"), outputJsonString)
        futureResponse.flatMap { result => {
          if (result.status == 200) {
            logger.debug("Se envió correctamente el match a la instancia inferior")
            // Actualizar el estado a enviado
            val fut = this.updateMatchSendStatus(matchId, Some(laboratory), Some(MATCH_SENT), None, Some(outputJsonString))
            listProfiles.foreach(globalCode => {
              sendFiles(globalCode, laboratory)
            })
            fut
          } else {
            logger.error(result.body)
            logger.debug("No se pudo enviar el match a la instancia inferior")
            Future.successful(Left("No se pudo enviar el match a la instancia inferior"))
          }
        }
        }.recoverWith {
          case _: Exception => Future.successful(Left("No se pudo enviar el match a la instancia inferior"))
        }
      }
    }
  }

  def convertToJson(matchSuperiorInstance: MatchSuperiorInstance): String = {
    if (matchSuperiorInstance.superiorProfileAssociated.isDefined) {
      Json.obj("matchResult" -> matchingRepository.convertToJson(matchSuperiorInstance.matchResult),
        "superiorProfile" -> Json.toJson(matchSuperiorInstance.superiorProfile),
        "superiorProfileData" -> Json.toJson(matchSuperiorInstance.superiorProfileData),
        "superiorProfileAssociated" -> Json.toJson(matchSuperiorInstance.superiorProfileAssociated)).toString()
    } else {
      Json.obj("matchResult" -> matchingRepository.convertToJson(matchSuperiorInstance.matchResult),
        "superiorProfile" -> Json.toJson(matchSuperiorInstance.superiorProfile),
        "superiorProfileData" -> Json.toJson(matchSuperiorInstance.superiorProfileData)).toString()
    }
  }

  def sendMatchToLaboratory(laboratory: String, matchResult: MatchResult, matchingProfile: MatchingProfile, externalProfileData: ExternalProfileDataRow): Future[Unit] = {

    logger.info(s"send match ${matchResult._id.id} to laboratory ${laboratory}")
    // Convertir el MatchResult
    // Enviar el request http
    //////////////////////////
    (for {
      profileOpt <- profileService.findByCode(matchingProfile.globalCode)
      profileDataOpt <- profileDataService.findByCodeWithoutDetails(matchingProfile.globalCode)
    } yield (profileOpt, profileDataOpt)).flatMap {
      case (Some(profile), Some(profileData)) => {
        this.getProfileAssociated(profile).flatMap(pAssociated => {
          val categoryDesc = this.categoryService.listCategories.find(c => c._1 == profileData.category).flatMap(_._2.description)
          val matchSuperiorInstance = MatchSuperiorInstance(matchResult, profile.copy(matcheable = false, internalSampleCode = profile.globalCode.text),
            SuperiorProfileData(categoryDesc.getOrElse(profileData.category.text), profileData.bioMaterialType,
              profileData.assignee, Some(profileData.laboratory), profileData.laboratory, currentInstanceLabCode, currentInstanceLabCode
              , profileData.responsibleGeneticist, profileData.globalCode.text, profileData.profileExpirationDate,
              profileData.sampleDate, profileData.sampleEntryDate), pAssociated)
          val outputJsonString = convertToJson(matchSuperiorInstance)
          inferiorInstanceRepository.findByLabCode(laboratory).flatMap {
            case None => Future.successful(Right(()))
            case Some(inferiorInstance) => {
              this.updateMatchSendStatus(matchResult._id.id, Some(laboratory), Some(MATCH_SEND_PENDING), None, Some(outputJsonString)).flatMap(_ => {
                this.sendRequestMatch(outputJsonString, inferiorInstance.url, laboratory, matchResult._id.id, Seq(Some(profile.globalCode.text), pAssociated.map(_.globalCode.text)).flatten)
              })
            }
          }.map(x => ())
        })
      }
      case (_, _) => {
        Future.successful(())
      }
    }


  }

  def doSendMatchToInferiorInstance(matchResult: MatchResult): Future[Unit] = {

    (for {
      leftProfile <- profileDataService.getExternalProfileDataByGlobalCode(matchResult.leftProfile.globalCode.text)
      rightProfile <- profileDataService.getExternalProfileDataByGlobalCode(matchResult.rightProfile.globalCode.text)
    } yield (leftProfile, rightProfile)).flatMap(x => {
      x match {
        case (Some(left), Some(right)) if !left.laboratoryOrigin.equals(right.laboratoryOrigin) => {
          Future.sequence(List(sendMatchToLaboratory(left.laboratoryImmediate, matchResult, matchResult.rightProfile, left),
            sendMatchToLaboratory(right.laboratoryImmediate, matchResult, matchResult.leftProfile, right))).flatMap(list => Future.successful(()))
        }
        case (None, Some(right)) => sendMatchToLaboratory(right.laboratoryImmediate, matchResult, matchResult.leftProfile, right)
        case (Some(left), None) => sendMatchToLaboratory(left.laboratoryImmediate, matchResult, matchResult.rightProfile, left)
        case (None, None) => {
          Future.successful(())
        }
        case (_, _) => {
          // caso de que eran del mismo laboratorio origen, ignoro el match
          Future.successful(())
        }
      }
    })

    //    ()
  }

  override def receiveMatchFromSuperior(matchSuperiorInstance: MatchSuperiorInstance): Future[Either[Unit, Unit]] = {
    this.isFromCurrentInstance(matchSuperiorInstance.superiorProfile.globalCode) match {
      case true => {
        logger.error("No deberia bajar un perfil del propio laboratorio " + matchSuperiorInstance.superiorProfile.globalCode.text)
        Future.successful(Right(()))
      }
      case false => {
        // el superiorProfile es el del otro laboratorio, si el left es el superiorProfile entonces el propio es el right
        if (matchSuperiorInstance.matchResult.leftProfile.globalCode == matchSuperiorInstance.superiorProfile.globalCode) {
          this.notificationService.push(matchSuperiorInstance.matchResult.rightProfile.assignee,
            MatchingInfo(
              matchSuperiorInstance.matchResult.rightProfile.globalCode,
              matchSuperiorInstance.matchResult.leftProfile.globalCode,
              matchSuperiorInstance.matchResult._id.id
            )
          )
          userService.sendNotifToAllSuperUsers(MatchingInfo(
            matchSuperiorInstance.matchResult.rightProfile.globalCode,
            matchSuperiorInstance.matchResult.leftProfile.globalCode,
            matchSuperiorInstance.matchResult._id.id
          ), Seq(matchSuperiorInstance.matchResult.rightProfile.assignee))
        } else {
          this.notificationService.push(matchSuperiorInstance.matchResult.leftProfile.assignee,
            MatchingInfo(
              matchSuperiorInstance.matchResult.leftProfile.globalCode,
              matchSuperiorInstance.matchResult.rightProfile.globalCode,
              matchSuperiorInstance.matchResult._id.id
            )
          )
          userService.sendNotifToAllSuperUsers(MatchingInfo(
            matchSuperiorInstance.matchResult.leftProfile.globalCode,
            matchSuperiorInstance.matchResult.rightProfile.globalCode,
            matchSuperiorInstance.matchResult._id.id
          ), Seq(matchSuperiorInstance.matchResult.leftProfile.assignee))
        }

        this.traceMatch(matchSuperiorInstance.matchResult.leftProfile.globalCode,
          matchSuperiorInstance.matchResult.rightProfile.globalCode,
          matchSuperiorInstance.matchResult._id.id,
          matchSuperiorInstance.matchResult.`type`, matchSuperiorInstance.matchResult.leftProfile.assignee)

        this.traceMatch(matchSuperiorInstance.matchResult.rightProfile.globalCode,
          matchSuperiorInstance.matchResult.leftProfile.globalCode,
          matchSuperiorInstance.matchResult._id.id,
          matchSuperiorInstance.matchResult.`type`, matchSuperiorInstance.matchResult.rightProfile.assignee)


        var fut = this.getCategoryReverseOptional(matchSuperiorInstance.superiorProfileAssociated).flatMap(categoryAssociated => {
          categoryService.getCategoriesMappingReverseById(matchSuperiorInstance.superiorProfile.categoryId).flatMap(categoryInferiorProfile => {
            this.insertOrUpdateProfile(matchSuperiorInstance.superiorProfile.copy(internalSampleCode = matchSuperiorInstance.superiorProfile.globalCode.text, matcheable = false, categoryId = categoryInferiorProfile.getOrElse(matchSuperiorInstance.superiorProfile.categoryId)),
                matchSuperiorInstance.superiorProfileData.laboratoryOrigin,
                matchSuperiorInstance.superiorProfileData.laboratoryImmediate,
                matchSuperiorInstance.superiorProfileData.laboratory
                , matchSuperiorInstance.superiorProfileAssociated.map(p => p.copy(internalSampleCode = p.globalCode.text, labeledGenotypification = None, matcheable = false, categoryId = categoryAssociated.getOrElse(p.categoryId))))
              .flatMap { sampleCode => {

                matchingRepository.insertMatchingResult(
                  matchSuperiorInstance.matchResult.copy(superiorProfileInfo = Some(SuperiorProfileInfo(matchSuperiorInstance.superiorProfileData, matchSuperiorInstance.superiorProfile)))
                ).recover { case error => logger.error(error.getMessage, error) }
              }
              }
          })
        })
        fut.onSuccess{ case _ =>
          Await.result(this.forwardMatchToInferiorInstances(matchSuperiorInstance), Duration.Inf)
        }
        Future.successful(Right(()))
      }
    }

  }
  private def forwardMatchToInferiorInstances(matchSuperiorInstance: MatchSuperiorInstance): Future[Unit] = {
    val profiles = List(matchSuperiorInstance.matchResult.leftProfile.globalCode,
      matchSuperiorInstance.matchResult.rightProfile.globalCode)
    val globalCode = profiles.filter(profile => profile != matchSuperiorInstance.superiorProfile.globalCode).head
    if(!this.isFromCurrentInstance(globalCode)){
      profileDataService.getExternalProfileDataByGlobalCode(globalCode.text).flatMap(externalProfile => {
        this.forwardMatchToInferiorInstance(matchSuperiorInstance,externalProfile.get.laboratoryImmediate)
      })
    }else{
      Future.successful(())
    }
  }
  def forwardMatchToInferiorInstance(matchSuperiorInstance: MatchSuperiorInstance, laboratory: String): Future[Unit] = {
    logger.info(s"send match ${matchSuperiorInstance.matchResult._id.id} to laboratory ${laboratory}")
    val outputJsonString = convertToJson(matchSuperiorInstance)

    inferiorInstanceRepository.findByLabCode(laboratory).flatMap {
      case None => Future.successful(Right(()))
      case Some(inferiorInstance) => {
        this.updateMatchSendStatus(matchSuperiorInstance.matchResult._id.id, Some(laboratory), Some(MATCH_SEND_PENDING), None, Some(outputJsonString)).flatMap(_ => {
          this.sendRequestMatch(outputJsonString, inferiorInstance.url, laboratory, matchSuperiorInstance.matchResult._id.id,
            List(matchSuperiorInstance.superiorProfile.globalCode.text) ::: matchSuperiorInstance.superiorProfile.associatedTo.map(list => list.map(_.text)).getOrElse(Nil))
        })
      }
    }.map(x => ())

  }
  private def getCategoryReverseOptional(profile: Option[Profile]): Future[Option[AlphanumericId]] = {
    if (profile.isDefined) {
      categoryService.getCategoriesMappingReverseById(profile.get.categoryId)
    } else {
      Future.successful(None)
    }
  }

  def traceMatch(left: SampleCode, right: SampleCode, matchingId: String, analysisType: Int, assignee: String): Unit = {
    if (left.text.contains(currentInstanceLabCode)) {
      traceService.add(Trace(left, assignee, new Date(),
        MatchInfo(matchingId, right, analysisType, MatchTypeInfo.Insert)))
    }
  }

  override def convertStatus(matchId: String, firingCode: SampleCode, status: String, matchRes: MatchResult, onlyUpload: Boolean = false): Unit = {
    doConvertStatus(matchId, firingCode, status, matchRes, currentInstanceLabCode, currentInstanceLabCode,onlyUpload)
    ()
  }

  override def deleteMatch(matchId: String, firingCode: String): Unit = {
    Future {
      this.matchingRepository.getByMatchingProfileId(matchId).map {
        case None => ()
        case Some(matchResult) => {
          this.convertStatus(matchId, SampleCode(firingCode), MatchStatus.deleted.toString, matchResult)
        }
      }
    }
    ()
  }

  def isFromLab(globalCode: SampleCode, lab: String): Boolean = {
    var externalProfileData: Option[ExternalProfileDataRow] =
      Await.result(this.profileDataService.getExternalProfileDataByGlobalCode(globalCode.text), Duration.Inf)
    var profileData: Option[ProfileData] = Await.result(this.profileDataService.get(globalCode), Duration.Inf)
    val profileLab = externalProfileData.map(_.laboratoryOrigin).getOrElse(currentInstanceLabCode)

    return profileData.isDefined && profileLab.equals(lab)
  }

  override def isFromCurrentInstance(globalCode: SampleCode): Boolean = {
    isFromLab(globalCode, currentInstanceLabCode)
  }

  def isSuperiorLab(labImmediate: String) : Boolean = {
    var result = false
    var inferiorInstances = Await.result(this.inferiorInstanceRepository.findAll(), Duration.Inf)
    inferiorInstances match {
      case Left(a) => {
        //si no tiene inferiores es el superior
        result = !labImmediate.equals(currentInstanceLabCode)
      }
      case Right(inferiorInstancesList) => {
        //si tiene inferiores se fija si alguno de las inferiores es el immediateLab, sino es el superior
        if (inferiorInstancesList.size == 0) {
          result = !labImmediate.equals(currentInstanceLabCode)
        } else {
          val inferiorLabs = inferiorInstancesList.map(_.laboratory)
          result = inferiorLabs.filter(labImmediate.equals(_)).size == 0 && !labImmediate.equals(currentInstanceLabCode)
        }
      }
    }
    result
  }

  override def wasMatchUploaded(matchResult: MatchResult, labImmediate: String): Boolean = {
    var result = false;
    val isFromSuperior = isSuperiorLab(labImmediate)
    if (!isFromSuperior) {
      val profiles = List(matchResult.leftProfile.globalCode,matchResult.rightProfile.globalCode)

      val localProfiles = profiles.filter(isFromCurrentInstance(_))
      val uploadedProfiles = profiles.filter(isUploaded(_))
      // me tengo que fijar de los perfiles del match cuales son de mis instancias inferiores
      // si son de mis instancias inferiores tengo que chequear que fueron subidos
      if(localProfiles.size == 2){
        result = uploadedProfiles.size == 2;
      }else if(localProfiles.size == 1) {
        result = uploadedProfiles.contains(localProfiles.head)
      }else if (localProfiles.size == 0) {
        // me tengo que fijar cuantos son de instancias inferiores mias
        // y esa cantidad tiene que ser igual a la cantidad de uploaded profiles
        var inferiorInstances = Await.result(this.inferiorInstanceRepository.findAll(), Duration.Inf)
        inferiorInstances match {
          case Left(a) => {
            result = false
          }
          case Right(inferiorInstancesList) => {
            val inferiorLabs = inferiorInstancesList.map(_.laboratory)
            val profileLeftOpt = Await.result(profileDataService.getExternalProfileDataByGlobalCode(matchResult.leftProfile.globalCode.text), Duration.Inf)
            val profileRightOpt = Await.result(profileDataService.getExternalProfileDataByGlobalCode(matchResult.rightProfile.globalCode.text), Duration.Inf)

            (profileLeftOpt,profileRightOpt) match {
              case (Some(profileLeft),Some(profileRight)) => {
                result = List(profileLeft.laboratoryOrigin,profileRight.laboratoryOrigin)
                  .filter(inferiorLabs.contains(_)).size == uploadedProfiles.size
              }
            }

          }
        }
      }
      result

    } else {
      result
    }
  }

  override def isInterconnectionMatch(matchResult: MatchResult): Boolean = {
    //chequea tambien si alguno de los perfiles fue subido, eso es por si son los dos de la misma instancia
    return (!isFromCurrentInstance(matchResult.leftProfile.globalCode) ||
      !isFromCurrentInstance(matchResult.rightProfile.globalCode) ||
      (isUploaded(matchResult.leftProfile.globalCode) && isUploaded(matchResult.rightProfile.globalCode))
      )
  }

  override def isExternalMatch(matchResult: MatchResult): Boolean = {
    return (!isFromCurrentInstance(matchResult.leftProfile.globalCode) &&
      !isFromCurrentInstance(matchResult.rightProfile.globalCode))
  }

  def shouldResendInterconnectionMatchStatus(matchResult: MatchResult): Boolean = {
    return (!isFromCurrentInstance(matchResult.leftProfile.globalCode) &&
      !isFromCurrentInstance(matchResult.rightProfile.globalCode))
  }

  def isUploaded(globalCode: SampleCode): Boolean = {
    val statusProfileUploaded = Await.result(this.profileDataService.getProfileUploadStatusByGlobalCode(globalCode), Duration.Inf)
    val isUploaded = statusProfileUploaded match {
      case Some(PENDIENTE_ENVIO) => true
      case Some(ENVIADA) => true
      case Some(RECHAZADA) => false
      case Some(APROBADA) => true
      case Some(PENDING_DELETE) => false
      case Some(_) => false
      case None => false
    }
    isUploaded
  }

  def handleDeliveryConvertStatus(globalCode: SampleCode, convertStatus: ConvertStatusInterconnection, labImmediate: String, sendToSuperior: Boolean,
                                  onlyUpload: Boolean = false): Unit = {
    getUrl(globalCode).map {
      case Nil => {
        logger.error("No deberia pasar por aca porque se supone que tiene configurada la url")
        ()
      }
      case (urls) => {
        urls
          .filter(_._2 != labImmediate)
          .filter(x => (x._2 != superiorLabCode && !onlyUpload) || (x._2 == superiorLabCode && sendToSuperior) )
          .foreach(url => {
            sendConvertStatus(convertStatus, url._1, url._2)
          })
      }
    }
  }

  def markHitOrDiscardSent(convertStatus: ConvertStatusInterconnection, outputJsonString: Option[String], labcode: String): Future[Either[String, Unit]] = {
    convertStatus.status match {
      case "hit" => {
        this.updateMatchSendStatus(convertStatus.matchId, Some(labcode), None, Some(HIT_SENT), outputJsonString)
      }
      case "discarded" => {
        this.updateMatchSendStatus(convertStatus.matchId, Some(labcode), None, Some(DISCARD_SENT), outputJsonString)
      }
      case _ => {
        this.updateMatchSendStatus(convertStatus.matchId, Some(labcode), None, Some(DELETE_SENT), outputJsonString)
      }
    }
  }

  def markHitOrDiscardPending(convertStatus: ConvertStatusInterconnection, outputJsonString: Option[String], labcode: String): Future[Either[String, Unit]] = {
    convertStatus.status match {
      case "hit" => {
        this.updateMatchSendStatus(convertStatus.matchId, Some(labcode), None, Some(HIT_PENDING), outputJsonString)
      }
      case "discarded" => {
        this.updateMatchSendStatus(convertStatus.matchId, Some(labcode), None, Some(DISCARD_PENDING), outputJsonString)
      }
      case _ => {
        this.updateMatchSendStatus(convertStatus.matchId, Some(labcode), None, Some(DELETE_PENDING), outputJsonString)
      }
    }
  }

  def sendConvertStatus(convertStatus: ConvertStatusInterconnection, url: String, labcode: String): Unit = {

    val holder: WSRequestHolder = client.url(protocol + url + "/interconection/match/status")
      .withHeaders("Content-Type" -> "application/json")
    val outputJson = Json.toJson(convertStatus)
    val outputJsonString = outputJson.toString
    this.markHitOrDiscardPending(convertStatus, Some(outputJsonString), labcode).map {
        case Left(_) => {
          Future.successful(())
        }
        case Right(()) => {
          this.getConnectionsStatus(url).flatMap {
            case Left(_) => Future.successful(Left("No se pudo enviar el status del match"))
            case Right(_) => {
              this.sendRequestQueue(holder.withMethod("POST"), outputJsonString).flatMap { result => {
                if (result.status == 200) {
                  logger.debug("Se envió correctamente el status del match")
                  // Actualizar el estado a enviado
                  this.markHitOrDiscardSent(convertStatus, Some(outputJsonString), labcode)
                  Future.successful(Right(()))
                } else {
                  logger.error(result.body)
                  // Actualizar el estado a error
                  logger.debug("No se pudo enviar el status del match")
                  Future.successful(Left("No se pudo enviar el status del match"))
                }
              }
              }
            }
          }
        }
      }
      .recoverWith {
        case _: Exception => Future.successful(Left("No se pudo enviar el status del match"))
      }

  }

  def getUrl(globalCode: SampleCode): Future[List[(String, String)]] = {
    (for {
      pd <- profileDataService.getExternalProfileDataByGlobalCode(globalCode.text)
      url <- connectionRepository.getSupInstanceUrl()
    } yield (pd, url)).flatMap {
      case (Some(pd), None) => {
        this.inferiorInstanceRepository.findByLabCode(pd.laboratoryImmediate).flatMap {
          case Some(inferiorInstance) => {
            // Send to inferior instance
            Future.successful(List((inferiorInstance.url, inferiorInstance.laboratory)))
          }
          case None => {
            // no deberia pasar por aca
            Future.successful(Nil)
          }
        }
      }
      case (None, Some(url)) => {
        Future.successful(List((url,superiorLabCode)))
      }
      case (Some(pd), Some(url)) => {
        this.inferiorInstanceRepository.findByLabCode(pd.laboratoryImmediate).flatMap {
          case Some(inferiorInstance) => {
            // Send to inferior instance
            Future.successful(List((inferiorInstance.url, inferiorInstance.laboratory),(url,superiorLabCode)))
          }
          case None => {
            // no deberia pasar por aca
            Future.successful(List((url,superiorLabCode)))
          }
        }
      }
    }
  }

  def shouldBeForwarded(globalCode: SampleCode, labOrigin: String, labImmediate: String): Boolean = {
    //chequea tambien si es de la propia instancia pero fue subido
    return ((!isFromCurrentInstance(globalCode) || (isFromCurrentInstance(globalCode) && isUploaded(globalCode))))
    /*

    return (!isFromCurrentInstance(globalCode) && (isFromCurrentInstance(globalCode) && isUploaded(globalCode))) &&
        !isFromLab(globalCode,labOrigin) &&
        !isFromLab(globalCode,labImmediate))
       */
  }

  def doConvertStatus(matchId: String, firingCode: SampleCode, status: String, matchResult: MatchResult, labOrigin: String, labImmediate: String
                      , onlyUpload: Boolean = false): Future[Unit] = Future {
    var sendToSuperior = wasMatchUploaded(matchResult, labImmediate)
    if (isInterconnectionMatch(matchResult)) {
      //      val convertStatus = ConvertStatusInterconnection(matchId, firingCode, matchResult.leftProfile.globalCode, matchResult.rightProfile.globalCode, status, labOrigin, labImmediate)
      val convertStatus = ConvertStatusInterconnection(matchId, firingCode, matchResult.leftProfile.globalCode, matchResult.rightProfile.globalCode, status, labOrigin, currentInstanceLabCode)
      if (shouldBeForwarded(matchResult.leftProfile.globalCode, labOrigin, labImmediate) && matchResult.leftProfile.globalCode.text != firingCode.text) {
        logger.debug("Sube estado match perfile izquierdo al superior")
        handleDeliveryConvertStatus(matchResult.leftProfile.globalCode, convertStatus,labImmediate, sendToSuperior,onlyUpload)
      }
      if (shouldBeForwarded(matchResult.rightProfile.globalCode, labOrigin, labImmediate) && matchResult.rightProfile.globalCode.text != firingCode.text) {
        logger.debug("Sube estado match perfile derecho al superior")
        handleDeliveryConvertStatus(matchResult.rightProfile.globalCode, convertStatus,labImmediate,sendToSuperior,onlyUpload)
      }
    }
    ()
  }

  def receiveMatchStatus(matchId: String, firingCode: SampleCode, leftCode: SampleCode, rightCode: SampleCode, status: String, labOrigin: String, labImmediate: String): Future[Unit] = Future {

    matchingRepository.getByMatchingProfileId(matchId).flatMap {
      case Some(matchingResult) => {
        if (shouldBeForwarded(matchingResult.leftProfile.globalCode, labOrigin, labImmediate) ||
          shouldBeForwarded(matchingResult.rightProfile.globalCode, labOrigin, labImmediate)) {
          //          doConvertStatus(matchId, firingCode, status, matchingResult, labOrigin, currentInstanceLabCode)
          doConvertStatus(matchId, firingCode, status, matchingResult, labOrigin, labImmediate)
        }
        val isAdmin = true
        matchingService.convertHitOrDiscard(matchId, firingCode, isAdmin, status)
        notifyHitOrDiscard(matchId, firingCode, status, matchingResult.leftProfile.globalCode, matchingResult.rightProfile.globalCode, getAssignee(matchingResult))
        // matching service hit or discard
        Future.successful(())
      }
      case None => {
        //Pueden ser los dos de la misma instancia y por eso no subio el match, pero igual se debe actualizar
        matchingRepository.getByFiringAndMatchingProfile(leftCode, rightCode).flatMap {
          case Some(matchingResult) => {
            if (shouldBeForwarded(matchingResult.leftProfile.globalCode, labOrigin, labImmediate) ||
              shouldBeForwarded(matchingResult.rightProfile.globalCode, labOrigin, labImmediate)) {
              doConvertStatus(matchingResult._id.id, firingCode, status, matchingResult, labOrigin, labImmediate)
            }
            val isAdmin = true
            matchingService.convertHitOrDiscard(matchingResult._id.id, firingCode, isAdmin, status)
            notifyHitOrDiscard(matchingResult._id.id, firingCode, status, matchingResult.leftProfile.globalCode, matchingResult.rightProfile.globalCode, getAssignee(matchingResult))
            // matching service hit or discard
            Future.successful(())
          }
          case None => {
            Future.successful(())
          }
        }
        //Future.successful(())
      }
    }
  }

  private def getAssignee(matchingResult: MatchResult): List[String] = {

    if (this.isFromCurrentInstance(matchingResult.leftProfile.globalCode)) {
      List(matchingResult.leftProfile.assignee)
    } else if (this.isFromCurrentInstance(matchingResult.rightProfile.globalCode)) {
      List(matchingResult.rightProfile.assignee)
    } else {
      Nil
    }

  }

  private def notifyHitOrDiscard(matchId: String, firingCode: SampleCode, action: String, left: SampleCode, right: SampleCode, users: List[String]): Unit = {

    val globalCode = if (firingCode == left) right else left
    val matchedProfile = if (globalCode == left) right else left

    action match {
      case "hit" => {
        this.notify(HitInfoInbox(globalCode, matchedProfile, matchId), Permission.INTERCON_NOTIF, users)
      }
      case "discarded" => {
        this.notify(DiscardInfoInbox(globalCode, matchedProfile, matchId), Permission.INTERCON_NOTIF, users)
      }
      case "deleted" => {
        ()
      }
      case _ => {
        ()
      }
    }
  }

  override def updateMatchSendStatus(id: String, targetLab: Option[String] = None, status: Option[Long], statusHitDiscard: Option[Long] = None, message: Option[String] = None): Future[Either[String, Unit]] = {
    status match {
      case Some(s) => {
        connectionRepository.updateMatchSendStatus(id, targetLab, status, message)
      }
      case None => {
        connectionRepository.getMatchUpdateSendStatusById(id, targetLab).flatMap {
          case Some(status) => {
            connectionRepository.updateMatchSendStatusHitOrDiscard(id, targetLab, statusHitDiscard, message)
          }
          case None => {
            connectionRepository.insertMatchUpdateSendStatus(id, targetLab, statusHitDiscard, message)
          }
        }
      }
    }
  }

  def retry(): Unit = {
    Future {
      this.doRetry()
    }
    ()
  }

  def retryDeleteProfilesToSuperior(lab: String, url: String): Future[Unit] = {
    if (lab == superiorLabCode) {
      logger.info("Reintento Baja de perfiles laboratorio:" + lab)
      this.profileDataService.gefFailedProfilesUploadedDeleted().flatMap(listFailed => {
        logger.info("Cantidad de perfiles para volver a borrar:" + lab + " " + listFailed.size)
        Future.successful(listFailed.foreach(profileFailedRow => {
          logger.info("Intentando borrar el perfil: " + profileFailedRow.globalCode + " en la instancia: " + currentInstanceLabCode)
          //TODO : obtener el usuario conectado
          doInferiorDeleteProfile(SampleCode(profileFailedRow.globalCode), DeletedMotive(profileFailedRow.userName.getOrElse(""), profileFailedRow.motive.getOrElse("")), url, profileFailedRow.userName.getOrElse(""))
        }))
      })
    } else {
      Future.successful(())
    }
  }

  def retryDeleteProfilesToInferior(lab: String, url: String): Future[Unit] = {
    if (lab != superiorLabCode) {
      logger.info("Reintento Baja de perfiles laboratorio:" + lab)
      this.profileDataService.gefFailedProfilesSentDeleted(lab).flatMap(listFailed => {
        logger.info("Cantidad de perfiles para volver a borrar:" + lab + " " + listFailed.size)
        Future.successful(listFailed.foreach(profileFailedRow => {
          sendDeletionToInstance(SampleCode(profileFailedRow.globalCode), DeletedMotive(profileFailedRow.userName.getOrElse(""), profileFailedRow.motive.getOrElse(""), 0), protocol + url, superiorLabCode, superiorLabCode, lab, false, profileFailedRow.userName.getOrElse("")) }))
      })
    } else {
      Future.successful(())
    }
  }

  def retryUploadProfiles(lab: String): Future[Unit] = {
    if (lab == superiorLabCode) {
      logger.info("Reintento Subida de perfiles laboratorio:" + lab)
      this.profileDataService.gefFailedProfilesUploaded().flatMap(listFailed => {
        logger.info("Cantidad de perfiles para volver a subir:" + lab + " " + listFailed.size)

        listFailed.foreach(profileFailedRow => {
          (for {
            profileFut <- profileService.findByCode(SampleCode(profileFailedRow.globalCode))
            profileDataFut <- profileDataService.findByCodeWithoutDetails(SampleCode(profileFailedRow.globalCode))
          } yield (profileFut, profileDataFut)).flatMap {
            case (Some(profile), Some(profileData)) => {
              Future.successful(this.uploadProfileToSuperiorInstance(profile, profileData))
            }
            case (_, _) => {
              Future.successful(())
            }
          }
          ()
        })
        Future.successful(())
      })
    }
    Future.successful(())
  }

  def retryHitOrDiscard(labcode: String, url: String): Future[Unit] = {
    logger.info("Reintento Hit y discard laboratorio:" + labcode)
    this.connectionRepository.getFailedMatchUpdateSent(labcode).flatMap(listFailed => {
      Future.successful(listFailed.foreach(matchUpdateFailed => {
        if (matchUpdateFailed.message.isDefined) {
          val convertStatus = Json.fromJson[ConvertStatusInterconnection](Json.parse(matchUpdateFailed.message.get)).get
          Future.successful(sendConvertStatus(convertStatus, url, labcode))
        }
      }))
    })
    Future.successful(())
  }

  def retrySendFiles(labcode: String, url: String): Future[Unit] = {
    logger.info("Reintento envio de arhivos laboratorio:" + labcode)
    this.connectionRepository.getFailedFileSent(labcode).flatMap(listFailed => {
      logger.info("Reintento cantidad de arhivos:" + labcode + " " + listFailed.size)
      Future.successful(listFailed.foreach(sendFileFailed => {
        doRetrySendFiles(sendFileFailed.id, sendFileFailed.targetLab)
      }))
    })
    Future.successful(())
  }

  def retryMatchSend(lab: String, url: String): Future[Unit] = {
    if (lab != superiorLabCode) {
      logger.info("Reintento Matchs laboratorio:" + lab)
      this.connectionRepository.getFailedMatchSent(lab).flatMap(listFailed => {
        logger.info("Reintento cantidad de matchs:" + lab + " " + listFailed.size)

        Future.successful(listFailed.foreach(matchFailed => {
          if (matchFailed.message.isDefined) {
            val matchSuperiorInstance = Json.fromJson[MatchSuperiorInstance](Json.parse(matchFailed.message.get)).get
            this.sendRequestMatch(matchFailed.message.get, url, lab, matchFailed.id, Seq(Some(matchSuperiorInstance.superiorProfile.globalCode.text),
              matchSuperiorInstance.superiorProfileAssociated.map(_.globalCode.text)).flatten)
          }
        }))
      })
    }
    Future.successful(())
  }

  def retryAll(lab: String, url: String): Future[Unit] = {
    logger.info("Proceso General de reintento laboratorio:" + lab)
    Future.sequence(List(
      // Reintento subida de perfiles
      this.retryUploadProfiles(lab)
      ,
      // Reintento Envio de Matchs
      this.retryMatchSend(lab, url)
      ,
      // Reintento Envio de hit y descarte
      this.retryHitOrDiscard(lab, url)
      ,
      // Reintento envio de electroferogramas y archivos
      this.retrySendFiles(lab, url)
      ,
      // Reintento envio Baja de Perfil desde instancia inferior hacia instancia superior
      //logger.info("Reintento envio Baja de Perfil desde instancia inferior hacia instancia superior"),
      this.retryDeleteProfilesToSuperior(lab, url)
      ,
      // Reintento envio Baja de Perfil desde instancia superior hacia instancia inferior
      this.retryDeleteProfilesToInferior(lab,url)
    )).map(_ => ())
  }

  private def doRetry(): Future[Unit] = {
    logger.info("Proceso de reintento")
    connectionRepository.getSupInstanceUrl().flatMap {
      case None => {
        logger.info("Reintentos: No esta configurada instancia superior")
        Future.successful(())
      }
      case Some("") => {
        logger.info("Reintentos: No esta configurada instancia superior")
        Future.successful(())
      }
      case Some(superiorUrl) => {
        getConnectionsStatus(superiorUrl).flatMap {
          case Left(_) => {
            logger.error("Reintentos: " + superiorUrl + " superior no operativo")
            Future.successful(())
          }
          case Right(_) => {
            this.retryAll(superiorLabCode, superiorUrl)
          }
        }
      }
    }
    inferiorInstanceRepository.findAll().flatMap {
      case Left(_) => {
        logger.info("Reintentos: No hay instancias inferiores")
        Future.successful(())
      }
      case Right(instancesFull) => {
        instancesFull.filter(_.idStatus == 2).foreach(instancesFull => {
          getConnectionsStatus(instancesFull.url).flatMap {
            case Left(_) => {
              logger.error("Reintentos: laboratorio:" + instancesFull.laboratory + " no operativo")
              Future.successful(())
            }
            case Right(_) => {
              this.retryAll(instancesFull.laboratory, instancesFull.url)
            }
          }
        })
        Future.successful(())
      }
    }

    Future.successful(())
  }

  def receiveFile(file: FileInterconnection): Future[Unit] = {
    Future {
      if (file.typeFile == "FILE") {
        this.profileService.addFileWithId(SampleCode(file.profileId), file.analysisId, Base64.decodeBase64(file.content), file.name.getOrElse("file"), file.id)
      } else {
        this.profileService.addElectropherogramWithId(SampleCode(file.profileId), file.analysisId, Base64.decodeBase64(file.content), file.name.orNull, file.id)
      }
    }
    Future.successful(())
  }

  private def sendRequestFile(url: String, targetLab: String, fileToSend: FileInterconnection): Unit = {
    val holder: WSRequestHolder = client.url(protocol + url + "/interconnection/file").withHeaders("Content-Type" -> "application/json")
    val outputJson = Json.toJson(fileToSend)
    val outputJsonString = outputJson.toString
    val futureResponse: Future[WSResponse] = this.sendRequestQueue(holder.withMethod("POST"), outputJsonString)
    futureResponse.flatMap { result => {
      if (result.status == 200) {
        this.connectionRepository.updateFileSent(fileToSend.id, Some(targetLab), Some(FILE_SENT), fileToSend.typeFile)
        logger.debug("se envio el archivo correctamente")
        Future.successful(Right(()))
      } else {
        logger.debug(Messages("error.E0710"))
        Future.successful(Left(Messages("error.E0710")))
      }
    }
    }.recoverWith {
      case _: Exception => Future.successful(Left(Messages("error.E0710")))
    }
    ()
  }

  def sendFiles(globalCode: String, targetLab: String): Unit = {
    Future.sequence(List(this.profileService.getFullElectropherogramsByCode(SampleCode(globalCode)),
        this.profileService.getFullFilesByCode(SampleCode(globalCode))))
      .map(listListFiles => {
        val listFiles = listListFiles.flatten.toSeq
        if (!listFiles.isEmpty) {
          listFiles.foreach(fileToSend =>
            this.connectionRepository.updateFileSent(fileToSend.id, Some(targetLab), Some(FILE_PENDING), fileToSend.typeFile).flatMap(result => {
              this.getLabUrl(targetLab).map {
                case None => ()
                case Some(url) => {
                  sendRequestFile(url, targetLab, fileToSend)
                }
              }
            })
          )
        }
      }
      )
    ()
  }

  def doRetrySendFiles(id: String, targetLab: String): Unit = {
    Future.sequence(List(this.profileService.getFullElectropherogramsById(id),
        this.profileService.getFullFilesById(id)))
      .map(listListFiles => {
        val listFiles = listListFiles.flatten.toSeq
        if (!listFiles.isEmpty) {
          listFiles.foreach(fileToSend => {
            logger.info("sending file to lab:" + targetLab + " id: " + fileToSend.id)

            this.connectionRepository.updateFileSent(fileToSend.id, Some(targetLab), Some(FILE_PENDING), fileToSend.typeFile).flatMap(result => {
              this.getLabUrl(targetLab).map {
                case None => ()
                case Some(url) => {
                  sendRequestFile(url, targetLab, fileToSend)
                }
              }
            })
          }
          )
        }
      }
      )
    ()
  }


}