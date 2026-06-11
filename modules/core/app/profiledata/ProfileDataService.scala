package profiledata

import jakarta.inject.{Inject, Named, Provider, Singleton}
import models.Tables.{ProfileReceivedRow, ProfileSentRow, ProfileUploadedRow, ExternalProfileDataRow}
import play.api.Logging
import play.api.i18n.{Lang, MessagesApi}
import profile.{MtRCRS, ProfileRepository, ProfileService}
import services.{CacheService, LaboratoryService, TemporaryAssetKey}
import types.{AlphanumericId, SampleCode}
import configdata.{BioMaterialTypeService, CategoryService, CrimeTypeService}
import connections.InterconnectionService
import inbox.{NotificationService, ProfileDataAssociationInfo, ProfileDataInfo}
import matching.MatchingService
import pedigree.PedigreeService
import scenarios.ScenarioRepository
import security.ConnectionRepository
import trace.{DeleteInfo, ProfileCategoryModificationInfo, ProfileDataInfo as TraceProfileDataInfo, Trace, TraceService}
import services.UserService

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

// ============================================================================
// Trait
// ============================================================================

trait ProfileDataRepository:
  def findByCode(globalCode: SampleCode): Future[Option[ProfileData]]
  def isDeleted(globalCode: SampleCode): Future[Option[Boolean]]
  def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]]
  def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]]
  def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]]
  def get(id: Long): Future[ProfileData]
  def get(globalCode: SampleCode): Future[Option[ProfileData]]
  def add(profileData: ProfileData, completeLabCode: String, imageList: Option[List[java.io.File]], picturesList: Option[List[java.io.File]], signaturesList: Option[List[java.io.File]]): Future[SampleCode]
  def updateProfileData(globalCode: SampleCode, newProfile: ProfileData, imageList: Option[List[java.io.File]], picturesList: Option[List[java.io.File]], signaturesList: Option[List[java.io.File]]): Future[Boolean]
  def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]]
  def delete(globalCode: SampleCode, motive: DeletedMotive): Future[Int]
  def removeProfile(globalCode: SampleCode): Future[Either[String, SampleCode]]
  def getTotalProfilesByUser(search: ProfileDataSearch): Future[Int]
  def getTotalProfilesByUser(userId: String, isSuperUser: Boolean, category: String): Future[Int]
  def getProfilesByUser(search: ProfileDataSearch): Future[Seq[ProfileDataFull]]
  def giveGlobalCode(labCode: String): Future[String]
  def isDesktopProfile(globalCode: SampleCode): Future[Option[Boolean]]
  def getDeletedMotive(globalCode: SampleCode): Future[Option[DeletedMotive]]
  def findByCodes(globalCodes: List[SampleCode]): Future[Seq[ProfileData]]
  def getDesktopProfiles(): Future[Seq[SampleCode]]
  def addExternalProfile(profileData: ProfileData, labOrigin: String, labImmediate: String): Future[SampleCode]
  def updateUploadStatus(globalCode: String, status: Long, motive: Option[String], interconnectionError: Option[String], userName: Option[String], operationOriginatedInInstance: String): Future[Either[String, Unit]]
  def findUploadedProfilesByCodes(globalCodes: Seq[SampleCode]): Future[Seq[SampleCode]]
  def getExternalProfileDataByGlobalCode(globalCode: String): Future[Option[ExternalProfileDataRow]]
  def gefFailedProfilesUploaded(): Future[List[ProfileUploadedRow]]
  def gefFailedProfilesUploadedDeleted(): Future[List[ProfileUploadedRow]]
  def gefFailedProfilesSentDeleted(labCode: String): Future[List[ProfileSentRow]]
  def updateProfileSentStatus(globalCode: String, status: Long, motive: Option[String], labCode: String, interconnectionError: Option[String]): Future[Either[String, Unit]]
  def getMtRcrs(): Future[MtRCRS]
  def updateInterconnectionError(globalCode: String, status: Long, interconnectionError: String): Future[Either[String, Unit]]
  def addProfileReceivedApproved(labCode: String, globalCode: String, status: Long, userName: String, isCategoryModification: Boolean): Future[Either[String, Unit]]
  def addProfileReceivedRejected(labCode: String, globalCode: String, status: Long, motive: String, userName: String, isCategoryModification: Boolean): Future[Either[String, Unit]]
  def updateProfileReceivedStatus(labCode: String, globalCode: String, status: Long, motive: Option[String], isCategoryModification: Boolean, interconnectionError: Option[String], userName: Option[String], operationOriginatedInInstance: String): Future[Either[String, Unit]]
  def getPendingApprovalNotification(labCode: String): Future[Seq[ProfileReceivedRow]]
  def getPendingRejectionNotification(labCode: String): Future[Seq[ProfileReceivedRow]]
  def getFailedProfilesReceivedDeleted(labCode: String): Future[Seq[ProfileReceivedRow]]
  def getProfileReceivedStatusByGlobalCode(gc: SampleCode): Future[Option[Long]]
  def getIsProfileReplicated(globalCode: SampleCode): Future[Boolean]
  def getIsProfileReplicatedInternalCode(internalCode: String): Future[Boolean]
  def getImmediateInferiorInstanceLabCode(globalCode: String): Future[String]
  def getProfileReceivedLabInferior(globalCode: String): Future[String]
  def getProfileReceiveOperationOriginatedInInstance(globalCode: SampleCode): Future[Option[String]]
  def countProfiles(): Future[Int]

trait ProfileDataService:
  def getMtRcrs(): Future[MtRCRS]
  def create(profileData: ProfileDataAttempt): Future[Either[String, SampleCode]]
  def updateProfileData(globalCode: SampleCode, profileData: ProfileDataAttempt, allowFromOtherInstances: Boolean = false): Future[Boolean]
  def updateProfileCategoryData(globalCode: SampleCode, profileData: ProfileDataAttempt, userName: String): Future[Option[String]]
  def get(sampleCode: SampleCode): Future[Option[ProfileData]]
  def get(id: Long): Future[(ProfileData, configdata.Group, configdata.Category)]
  def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]]
  def findByCode(globalCode: SampleCode): Future[Option[ProfileData]]
  def findByCodeWithoutDetails(globalCode: SampleCode): Future[Option[ProfileData]]
  def findByCodes(globalCodes: List[SampleCode]): Future[Seq[ProfileData]]
  def findByCodeWithAssociations(globalCode: SampleCode): Future[Option[(ProfileData, configdata.Group, configdata.FullCategory)]]
  def findProfileDataLocalOrSuperior(globalCode: SampleCode): Future[Option[ProfileData]]
  def isEditable(sampleCode: SampleCode, allowFromOtherInstances: Boolean = false): Future[Option[Boolean]]
  def isDesktopProfile(globalCode: SampleCode): Future[Option[Boolean]]
  def getDeleteMotive(sampleCode: SampleCode): Future[Option[DeletedMotive]]
  def deleteProfile(globalCode: SampleCode, motive: DeletedMotive, userId: String, validateMPI: Boolean = true): Future[Either[String, SampleCode]]
  def delete(globalCode: SampleCode): Future[Either[String, SampleCode]]
  def removeAll(): Future[Int]
  def removeProfile(globalCode: SampleCode): Future[Either[String, SampleCode]]
  def getDesktopProfiles(): Future[Seq[SampleCode]]
  def importFromAnotherInstance(profileData: ProfileData, labOrigin: String, labImmediate: String): Future[Unit]
  def updateUploadStatus(globalCode: String, status: Long, motive: Option[String], interconnectionError: Option[String], userName: Option[String], operationOriginatedInInstance: String): Future[Either[String, Unit]]
  def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]]
  def getExternalProfileDataByGlobalCode(globalCode: String): Future[Option[ExternalProfileDataRow]]
  def gefFailedProfilesUploaded(): Future[Seq[ProfileUploadedRow]]
  def gefFailedProfilesUploadedDeleted(): Future[Seq[ProfileUploadedRow]]
  def gefFailedProfilesSentDeleted(labCode: String): Future[Seq[ProfileSentRow]]
  def updateProfileSentStatus(globalCode: String, status: Long, motive: Option[String], labCode: String, interconnectionError: Option[String], userName: Option[String]): Future[Either[String, Unit]]
  def updateInterconnectionError(globalCode: String, status: Long, interconnectionError: String): Future[Either[String, Unit]]
  def addProfileReceivedApproved(labCode: String, globalCode: String, status: Long, userName: String, isCategoryModification: Boolean): Future[Either[String, Unit]]
  def addProfileReceivedRejected(labCode: String, globalCode: String, status: Long, motive: String, userName: String, isCategoryModification: Boolean): Future[Either[String, Unit]]
  def updateProfileReceivedStatus(labCode: String, globalCode: String, status: Long, motive: String, isCategoryModification: Boolean, interconnectionError: String, userName: Option[String], operationOriginatedInInstance: String): Future[Either[String, Unit]]
  def getPendingApprovalNotification(labCode: String): Future[Seq[ProfileReceivedRow]]
  def getPendingRejectionNotification(labCode: String): Future[Seq[ProfileReceivedRow]]
  def gefFailedProfilesReceivedDeleted(labCode: String): Future[Seq[ProfileReceivedRow]]
  def shouldSendDeleteToSuperiorInstance(globalCode: SampleCode): Future[Boolean]
  def shouldSendDeleteToInferiorInstance(globalCode: SampleCode): Future[Boolean]
  def getLabFromGlobalCode(globalCode: SampleCode): Option[String]
  def getIsProfileReplicatedInternalCode(internalCode: String): Future[Boolean]
  def getProfileReceivedLabCode(globalCode: SampleCode): Future[Option[String]]
  def countProfiles(): Future[Int]

// ============================================================================
// Stubs (used by BulkUploadService while full implementation isn't active)
// ============================================================================

@Singleton
class ProfileDataRepositoryStub extends ProfileDataRepository:
  private def nyi = Future.failed(new UnsupportedOperationException("ProfileDataRepositoryStub"))
  override def findByCode(gc: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def isDeleted(gc: SampleCode): Future[Option[Boolean]] = Future.successful(None)
  override def getProfileUploadStatusByGlobalCode(gc: SampleCode): Future[Option[Long]] = Future.successful(None)
  override def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]] = Future.successful(Nil)
  override def getGlobalCode(c: String): Future[Option[SampleCode]] = Future.successful(None)
  override def get(id: Long): Future[ProfileData] = nyi
  override def get(gc: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def add(pd: ProfileData, lc: String, i: Option[List[java.io.File]], p: Option[List[java.io.File]], s: Option[List[java.io.File]]): Future[SampleCode] = nyi
  override def updateProfileData(gc: SampleCode, pd: ProfileData, i: Option[List[java.io.File]], p: Option[List[java.io.File]], s: Option[List[java.io.File]]): Future[Boolean] = Future.successful(false)
  override def getResource(rt: String, id: Long): Future[Option[Array[Byte]]] = Future.successful(None)
  override def delete(gc: SampleCode, m: DeletedMotive): Future[Int] = Future.successful(0)
  override def removeProfile(gc: SampleCode): Future[Either[String, SampleCode]] = Future.successful(Left("stub"))
  override def getTotalProfilesByUser(s: ProfileDataSearch): Future[Int] = Future.successful(0)
  override def getTotalProfilesByUser(u: String, su: Boolean, c: String): Future[Int] = Future.successful(0)
  override def getProfilesByUser(s: ProfileDataSearch): Future[Seq[ProfileDataFull]] = Future.successful(Seq.empty)
  override def giveGlobalCode(lc: String): Future[String] = Future.successful("")
  override def isDesktopProfile(gc: SampleCode): Future[Option[Boolean]] = Future.successful(None)
  override def getDeletedMotive(gc: SampleCode): Future[Option[DeletedMotive]] = Future.successful(None)
  override def findByCodes(codes: List[SampleCode]): Future[Seq[ProfileData]] = Future.successful(Seq.empty)
  override def getDesktopProfiles(): Future[Seq[SampleCode]] = Future.successful(Seq.empty)
  override def addExternalProfile(pd: ProfileData, lo: String, li: String): Future[SampleCode] = nyi
  override def updateUploadStatus(gc: String, s: Long, m: Option[String], ie: Option[String], u: Option[String], op: String): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def findUploadedProfilesByCodes(codes: Seq[SampleCode]): Future[Seq[SampleCode]] = Future.successful(Seq.empty)
  override def getExternalProfileDataByGlobalCode(gc: String): Future[Option[ExternalProfileDataRow]] = Future.successful(None)
  override def gefFailedProfilesUploaded(): Future[List[ProfileUploadedRow]] = Future.successful(Nil)
  override def gefFailedProfilesUploadedDeleted(): Future[List[ProfileUploadedRow]] = Future.successful(Nil)
  override def gefFailedProfilesSentDeleted(lc: String): Future[List[ProfileSentRow]] = Future.successful(Nil)
  override def updateProfileSentStatus(gc: String, s: Long, m: Option[String], lc: String, ie: Option[String]): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def getMtRcrs(): Future[MtRCRS] = Future.successful(MtRCRS(Map.empty))
  override def updateInterconnectionError(gc: String, s: Long, ie: String): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def addProfileReceivedApproved(lc: String, gc: String, s: Long, u: String, cm: Boolean): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def addProfileReceivedRejected(lc: String, gc: String, s: Long, m: String, u: String, cm: Boolean): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def updateProfileReceivedStatus(lc: String, gc: String, s: Long, m: Option[String], cm: Boolean, ie: Option[String], u: Option[String], op: String): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def getPendingApprovalNotification(lc: String): Future[Seq[ProfileReceivedRow]] = Future.successful(Seq.empty)
  override def getPendingRejectionNotification(lc: String): Future[Seq[ProfileReceivedRow]] = Future.successful(Seq.empty)
  override def getFailedProfilesReceivedDeleted(lc: String): Future[Seq[ProfileReceivedRow]] = Future.successful(Seq.empty)
  override def getProfileReceivedStatusByGlobalCode(gc: SampleCode): Future[Option[Long]] = Future.successful(None)
  override def getIsProfileReplicated(gc: SampleCode): Future[Boolean] = Future.successful(false)
  override def getIsProfileReplicatedInternalCode(ic: String): Future[Boolean] = Future.successful(false)
  override def getImmediateInferiorInstanceLabCode(gc: String): Future[String] = Future.successful("")
  override def getProfileReceivedLabInferior(gc: String): Future[String] = Future.successful("")
  override def getProfileReceiveOperationOriginatedInInstance(gc: SampleCode): Future[Option[String]] = Future.successful(None)
  override def countProfiles(): Future[Int] = Future.successful(0)

@Singleton
class ProfileDataServiceStub extends ProfileDataService:
  override def getMtRcrs(): Future[MtRCRS] = Future.successful(MtRCRS(Map.empty))
  override def create(pd: ProfileDataAttempt): Future[Either[String, SampleCode]] = Future.successful(Left("not implemented"))
  override def updateProfileData(gc: SampleCode, pd: ProfileDataAttempt, allow: Boolean): Future[Boolean] = Future.successful(false)
  override def updateProfileCategoryData(gc: SampleCode, pd: ProfileDataAttempt, u: String): Future[Option[String]] = Future.successful(None)
  override def get(sc: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def get(id: Long): Future[(ProfileData, configdata.Group, configdata.Category)] = Future.failed(new UnsupportedOperationException("stub"))
  override def getResource(rt: String, id: Long): Future[Option[Array[Byte]]] = Future.successful(None)
  override def findByCode(gc: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def findByCodeWithoutDetails(gc: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def findByCodes(codes: List[SampleCode]): Future[Seq[ProfileData]] = Future.successful(Seq.empty)
  override def findByCodeWithAssociations(gc: SampleCode): Future[Option[(ProfileData, configdata.Group, configdata.FullCategory)]] = Future.successful(None)
  override def findProfileDataLocalOrSuperior(gc: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def isEditable(sc: SampleCode, allow: Boolean): Future[Option[Boolean]] = Future.successful(None)
  override def isDesktopProfile(gc: SampleCode): Future[Option[Boolean]] = Future.successful(None)
  override def getDeleteMotive(sc: SampleCode): Future[Option[DeletedMotive]] = Future.successful(None)
  override def deleteProfile(gc: SampleCode, m: DeletedMotive, u: String, v: Boolean): Future[Either[String, SampleCode]] = Future.successful(Left("stub"))
  override def delete(gc: SampleCode): Future[Either[String, SampleCode]] = Future.successful(Left("stub"))
  override def removeAll(): Future[Int] = Future.successful(0)
  override def removeProfile(gc: SampleCode): Future[Either[String, SampleCode]] = Future.successful(Left("stub"))
  override def getDesktopProfiles(): Future[Seq[SampleCode]] = Future.successful(Seq.empty)
  override def importFromAnotherInstance(pd: ProfileData, lo: String, li: String): Future[Unit] = Future.successful(())
  override def updateUploadStatus(gc: String, s: Long, m: Option[String], ie: Option[String], u: Option[String], op: String): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def getProfileUploadStatusByGlobalCode(gc: SampleCode): Future[Option[Long]] = Future.successful(None)
  override def getExternalProfileDataByGlobalCode(gc: String): Future[Option[ExternalProfileDataRow]] = Future.successful(None)
  override def gefFailedProfilesUploaded(): Future[Seq[ProfileUploadedRow]] = Future.successful(Seq.empty)
  override def gefFailedProfilesUploadedDeleted(): Future[Seq[ProfileUploadedRow]] = Future.successful(Seq.empty)
  override def gefFailedProfilesSentDeleted(lc: String): Future[Seq[ProfileSentRow]] = Future.successful(Seq.empty)
  override def updateProfileSentStatus(gc: String, s: Long, m: Option[String], lc: String, ie: Option[String], u: Option[String]): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def updateInterconnectionError(gc: String, s: Long, ie: String): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def addProfileReceivedApproved(lc: String, gc: String, s: Long, u: String, cm: Boolean): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def addProfileReceivedRejected(lc: String, gc: String, s: Long, m: String, u: String, cm: Boolean): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def updateProfileReceivedStatus(lc: String, gc: String, s: Long, m: String, cm: Boolean, ie: String, u: Option[String], op: String): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def getPendingApprovalNotification(lc: String): Future[Seq[ProfileReceivedRow]] = Future.successful(Seq.empty)
  override def getPendingRejectionNotification(lc: String): Future[Seq[ProfileReceivedRow]] = Future.successful(Seq.empty)
  override def gefFailedProfilesReceivedDeleted(lc: String): Future[Seq[ProfileReceivedRow]] = Future.successful(Seq.empty)
  override def shouldSendDeleteToSuperiorInstance(gc: SampleCode): Future[Boolean] = Future.successful(false)
  override def shouldSendDeleteToInferiorInstance(gc: SampleCode): Future[Boolean] = Future.successful(false)
  override def getLabFromGlobalCode(gc: SampleCode): Option[String] = None
  override def getIsProfileReplicatedInternalCode(ic: String): Future[Boolean] = Future.successful(false)
  override def getProfileReceivedLabCode(gc: SampleCode): Future[Option[String]] = Future.successful(None)
  override def countProfiles(): Future[Int] = Future.successful(0)

// ============================================================================
// Full implementation
// ============================================================================

@Singleton
class ProfileDataServiceImpl @Inject()(
  cache: CacheService,
  @Named("special") profileDataRepository: ProfileDataRepository,
  categoryService: CategoryService,
  connectionRepository: ConnectionRepository,
  notificationService: NotificationService,
  bioMatService: BioMaterialTypeService,
  crimeTypeService: CrimeTypeService,
  laboratoryService: LaboratoryService,
  matchingServiceProvider: Provider[MatchingService],
  scenarioRepository: ScenarioRepository,
  profileRepository: ProfileRepository,
  traceService: TraceService,
  @Named("labCode") val labCode: String,
  @Named("country") val country: String,
  @Named("province") val province: String,
  interconnectionService: InterconnectionService,
  profileServiceProvider: Provider[ProfileService],
  pedigreeServiceProvider: Provider[PedigreeService],
  userService: UserService,
  messagesApi: play.api.i18n.MessagesApi
)(implicit ec: ExecutionContext) extends ProfileDataService with Logging:

  private def matchingService: MatchingService = matchingServiceProvider.get()
  private def profileService: ProfileService   = profileServiceProvider.get()
  private def pedigreeService: PedigreeService = pedigreeServiceProvider.get()

  private implicit val lang: Lang = Lang("es")
  private def msg(key: String, args: Any*): String = messagesApi(key, args*)(lang)

  override def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]] =
    profileDataRepository.getResource(resourceType, id)

  override def get(sampleCode: SampleCode): Future[Option[ProfileData]] =
    profileDataRepository.get(sampleCode)

  override def get(id: Long): Future[(ProfileData, configdata.Group, configdata.Category)] =
    for
      profile <- profileDataRepository.get(id)
      cats    <- categoryService.listCategories
      tree    <- categoryService.categoryTree
    yield
      val catId = profile.category
      val grpId = cats.get(catId).map(_.group).getOrElse(types.AlphanumericId(""))
      val groupEntry = tree.find { case (g, _) => g.id == grpId }.getOrElse(
        throw new NoSuchElementException(s"Group $grpId not found for profile $id")
      )
      val group    = groupEntry._1
      val category = groupEntry._2.find(_.id == catId).getOrElse(
        throw new NoSuchElementException(s"Category $catId not found in group $grpId for profile $id")
      )
      (profile, group, category)

  private def canDeleteProfile(sampleCode: SampleCode, validateMPI: Boolean): Future[(Boolean, Option[String])] =
    val checks =
      if validateMPI then
        Seq(
          doesntHaveScenarios(sampleCode),
          isNotAssociatedToCourtCase(sampleCode),
          isNotAssociatedToActivePedigree(sampleCode),
          doesNotHavePedigreeMatches(sampleCode),
          doesNotHavePedigreeMatchesBeingUnknown(sampleCode)
        )
      else
        Seq(
          doesntHaveScenarios(sampleCode),
          isNotAssociatedToActivePedigree(sampleCode),
          doesNotHavePedigreeMatches(sampleCode),
          doesNotHavePedigreeMatchesBeingUnknown(sampleCode)
        )
    Future.sequence(checks).map { list =>
      (list.forall(_._1), list.find(!_._1).getOrElse((true, None))._2)
    }

  private def doesNotHavePedigreeMatches(sc: SampleCode) =
    pedigreeService.getTotalProfilesPedigreeMatches(sc).map(n => (n == 0, Some("error.E0130")))

  private def doesNotHavePedigreeMatchesBeingUnknown(sc: SampleCode) =
    pedigreeService.getTotalProfileNumberOfMatches(sc).map(n => (n == 0, Some("error.E0129")))

  private def isNotAssociatedToCourtCase(sc: SampleCode) =
    pedigreeService.getTotalProfilesOccurenceInCase(sc).map(n => (n == 0, Some("error.E0125")))

  private def isNotAssociatedToActivePedigree(sc: SampleCode) =
    pedigreeService.countActivePedigreesByProfile(sc.text).map(n => (n == 0, Some("error.E0126")))

  private def doesntHaveScenarios(sc: SampleCode) =
    scenarioRepository.getByProfile(sc).map(s => (s.isEmpty, Some("error.E0118")))

  override def getLabFromGlobalCode(globalCode: SampleCode): Option[String] =
    val parts = globalCode.text.split("-")
    if parts.length == 4 then Some(parts(2)) else None

  override def shouldSendDeleteToSuperiorInstance(globalCode: SampleCode): Future[Boolean] =
    profileDataRepository.getProfileUploadStatusByGlobalCode(globalCode).map(_.isDefined)

  override def shouldSendDeleteToInferiorInstance(globalCode: SampleCode): Future[Boolean] =
    profileDataRepository.getProfileReceivedStatusByGlobalCode(globalCode).map(_.isDefined)

  override def getProfileReceivedLabCode(globalCode: SampleCode): Future[Option[String]] =
    // Fidelidad legacy (#218 review S4Q1): el legacy devuelve Some(inferiorLab) siempre, aun vacio.
    profileDataRepository.getProfileReceivedLabInferior(globalCode.text).map(s => Some(s))

  override def deleteProfile(
    globalCode: SampleCode, motive: DeletedMotive, userId: String, validateMPI: Boolean = true
  ): Future[Either[String, SampleCode]] =
    canDeleteProfile(globalCode, validateMPI).flatMap { case (allowed, errMsg) =>
      if !allowed then
        Future.successful(Left(msg(errMsg.getOrElse("error.E0127"), globalCode.text)))
      else
        delete(globalCode).flatMap {
          case Left(err) => Future.successful(Left(err))
          case Right(_) =>
            profileDataRepository.delete(globalCode, motive).flatMap { resp =>
              if resp == 1 then
                traceService.add(Trace(globalCode, userId, new Date(), DeleteInfo(userId, "Solicitado por: " + motive.solicitor + " " + motive.motive)))
                // Fire-and-forget (#218 review S2Q1): la baja local ya esta confirmada; la propagacion a
                // instancias superior/inferior NO se encadena al resultado, de modo que un fallo de
                // interconexion no convierte una baja exitosa en error. Se loguea server-side.
                val propagation =
                  for
                    sendSup  <- shouldSendDeleteToSuperiorInstance(globalCode)
                    sendInf  <- shouldSendDeleteToInferiorInstance(globalCode)
                    supUrl   <- if sendSup then connectionRepository.getSupInstanceUrl().map(_.getOrElse("")) else Future.successful("")
                    _        <- (if sendSup then
                                  Future.successful(interconnectionService.inferiorDeleteProfile(globalCode, motive, supUrl, userId, labCode))
                                else Future.successful(()))
                    infLabOpt <- if sendInf then getProfileReceivedLabCode(globalCode) else Future.successful(None)
                    _        <- infLabOpt match
                      case Some(infLab) =>
                        for
                          _ <- profileDataRepository.updateProfileReceivedStatus(infLab, globalCode.text, 19L, Some(motive.motive), false, None, Some(userId), labCode)
                          infUrlOpt <- connectionRepository.getInfInstanceUrl(infLab)
                          _ = infUrlOpt.foreach(infUrl => interconnectionService.sendDeletionToInferior(globalCode.text, motive, labCode, infUrl, userId, labCode))
                        yield ()
                      case None => Future.successful(())
                  yield ()
                propagation.recover { case e =>
                  logger.error(s"deleteProfile interconnection propagation failed for ${globalCode.text}", e)
                }
                Future.successful(Right(globalCode))
              else
                Future.successful(Left(msg("error.E0117")))
            }
        }
    }

  override def delete(globalCode: SampleCode): Future[Either[String, SampleCode]] =
    profileRepository.delete(globalCode).map(_.map(_ => globalCode))

  override def removeAll(): Future[Int] = Future.successful(0)

  override def removeProfile(globalCode: SampleCode): Future[Either[String, SampleCode]] =
    profileDataRepository.removeProfile(globalCode)

  private def getDetails(pd: Option[ProfileData]): Future[Option[ProfileData]] =
    pd.fold(Future.successful(None)) { pd =>
      for
        bmt <- bioMatService.list().map(seq => pd.bioMaterialType.flatMap(f => seq.find(_.id.text == f).map(_.name)))
        crt <- crimeTypeService.list().map(map => pd.crimeType.flatMap(map.get))
        lab <- laboratoryService.list().map(_.find(_.code == pd.laboratory))
      yield
        val labName = lab.map(_.name).getOrElse(labCode)
        val crimetype = crt.map(_.name)
        val crimeInv = pd.crimeInvolved.flatMap(f => crt.flatMap(_.crimes.find(_.id == f).map(_.name)))
        Some(pd.copy(bioMaterialType = bmt, crimeType = crimetype, crimeInvolved = crimeInv, laboratory = labName))
    }

  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] =
    profileDataRepository.findByCode(globalCode).flatMap(getDetails)

  override def findByCodeWithoutDetails(globalCode: SampleCode): Future[Option[ProfileData]] =
    profileDataRepository.findByCode(globalCode)

  override def findByCodes(globalCodes: List[SampleCode]): Future[Seq[ProfileData]] =
    profileDataRepository.findByCodes(globalCodes).flatMap { list =>
      Future.sequence(list.map(p => getDetails(Some(p)).map(_.get)))
    }

  private def searchImagesInCache(uuid: String): Either[String, Option[List[java.io.File]]] =
    cache.get(TemporaryAssetKey(uuid))
      .map(tempFiles => Right(Some(tempFiles)))
      .getOrElse(Left(msg("error.E0951", uuid)))

  override def updateProfileData(
    globalCode: SampleCode, profileData: ProfileDataAttempt, allowFromOtherInstances: Boolean = false
  ): Future[Boolean] =
    isEditable(globalCode, allowFromOtherInstances).flatMap { editableOpt =>
      if !editableOpt.getOrElse(false) then Future.successful(false)
      else
        val images = profileData.dataFiliation.map { fd =>
          for
            i <- searchImagesInCache(fd.inprint)
            p <- searchImagesInCache(fd.picture)
            s <- searchImagesInCache(fd.signature)
          yield (i, p, s)
        }.getOrElse(Right((None, None, None)))
        images match
          case Left(_) => Future.successful(false)
          case Right((inprints, pictures, signatures)) =>
            val pd = profileData.pdAttempToPd(labCode).copy(
              laboratory = profileData.laboratory.getOrElse(""),
              responsibleGeneticist = profileData.responsibleGeneticist.map(_.toString)
            )
            val promise = profileDataRepository.updateProfileData(globalCode, pd, inprints, pictures, signatures)
            promise.onComplete {
              case Success(_) =>
                traceService.add(Trace(globalCode, profileData.assignee, new Date(), TraceProfileDataInfo))
                profileData.dataFiliation.foreach { fd =>
                  cache.pop(TemporaryAssetKey(fd.inprint))
                  cache.pop(TemporaryAssetKey(fd.picture))
                  cache.pop(TemporaryAssetKey(fd.signature))
                }
              case Failure(ex) =>
                logger.error(s"updateProfileData error for $globalCode: ${ex.getMessage}")
            }
            promise
    }

  override def updateProfileCategoryData(
    globalCode: SampleCode, profileData: ProfileDataAttempt, userName: String
  ): Future[Option[String]] =
    profileService.isReadOnlySampleCode(globalCode, uploadedIsAllowed = true).flatMap {
      case (true, errorMsg) => Future.successful(Some(errorMsg))
      case (false, _) =>
        val images = profileData.dataFiliation.map { fd =>
          for
            i <- searchImagesInCache(fd.inprint)
            p <- searchImagesInCache(fd.picture)
            s <- searchImagesInCache(fd.signature)
          yield (i, p, s)
        }.getOrElse(Right((None, None, None)))
        images match
          case Left(error) => Future.successful(Some(error))
          case Right((inprints, pictures, signatures)) =>
            val pd = profileData.pdAttempToPd(labCode)
            val oldProfileData = profileDataRepository.findByCode(globalCode)
            val updatePromise = profileDataRepository.updateProfileData(globalCode, pd, inprints, pictures, signatures)
            for
              old     <- oldProfileData
              updated <- updatePromise
            yield
              if updated then
                traceService.add(Trace(globalCode, userName, new Date(),
                  ProfileCategoryModificationInfo(old.map(_.category.text).getOrElse(""), profileData.category.text, userName)))
                profileData.dataFiliation.foreach { fd =>
                  cache.pop(TemporaryAssetKey(fd.inprint))
                  cache.pop(TemporaryAssetKey(fd.picture))
                  cache.pop(TemporaryAssetKey(fd.signature))
                }
                None
              else Some(msg("error.E0132"))
            }.recover { case _ => Some(msg("error.E0132")) }

  override def create(profileData: ProfileDataAttempt): Future[Either[String, SampleCode]] =
    val images = profileData.dataFiliation.map { fd =>
      for
        i <- searchImagesInCache(fd.inprint)
        p <- searchImagesInCache(fd.picture)
        s <- searchImagesInCache(fd.signature)
      yield (i, p, s)
    }.getOrElse(Right((None, None, None)))
    images match
      // #218 review S5Q1: propagar el error real de imagenes (E0951), consistente con updateProfileCategoryData.
      case Left(error) => Future.successful(Left(error))
      case Right((inprints, pictures, signatures)) =>
        val pd = profileData.pdAttempToPd(labCode)
        val gcFuture = profileData.laboratory
          .fold(Future.successful(s"$country-$province-$labCode"))(l => profileDataRepository.giveGlobalCode(l))
        gcFuture.flatMap { g =>
          profileDataRepository.add(pd, g, inprints, pictures, signatures)
            .recover { case ex =>
              logger.error(s"create profiledata failed for internalCode=${profileData.internalSampleCode}: ${ex.getMessage}", ex)
              throw ex
            }
            .map { sc =>
              profileData.dataFiliation.foreach { fd =>
                cache.pop(TemporaryAssetKey(fd.inprint))
                cache.pop(TemporaryAssetKey(fd.picture))
                cache.pop(TemporaryAssetKey(fd.signature))
              }
              traceService.add(Trace(sc, profileData.assignee, new Date(), TraceProfileDataInfo))
              notificationService.push(profileData.assignee, ProfileDataInfo(profileData.internalSampleCode, sc))
              userService.sendNotifToAllSuperUsers(ProfileDataInfo(profileData.internalSampleCode, sc), Seq(profileData.assignee))
              // Fire-and-forget: category association check must not fail the create response.
              // The profile already exists at this point; a listCategories failure must not
              // return Left(error) to the caller (which would cause them to retry and duplicate).
              categoryService.listCategories.foreach { cats =>
                if cats.get(profileData.category).exists(_.associations.nonEmpty) then
                  notificationService.push(profileData.assignee, ProfileDataAssociationInfo(profileData.internalSampleCode, sc))
                  userService.sendNotifToAllSuperUsers(ProfileDataAssociationInfo(profileData.internalSampleCode, sc), Seq(profileData.assignee))
              }
              Right(sc)
            }
            .recover { case _ => Left(msg("error.E0119")) }
        }

  override def findByCodeWithAssociations(globalCode: SampleCode): Future[Option[(ProfileData, configdata.Group, configdata.FullCategory)]] =
    for
      profileOpt <- profileService.findProfileDataLocalOrSuperior(globalCode)
      cats       <- categoryService.listCategories
      tree       <- categoryService.categoryTree
    yield
      profileOpt.map { profile =>
        val catId    = profile.category
        val grpId    = cats.get(catId).map(_.group).getOrElse(types.AlphanumericId(""))
        val group    = tree.find { case (g, _) => g.id == grpId }.map(_._1).getOrElse(
          throw new NoSuchElementException(s"Group $grpId not found for profile ${globalCode.text}")
        )
        val category = cats.getOrElse(catId,
          throw new NoSuchElementException(s"Category $catId not found for profile ${globalCode.text}")
        )
        (profile, group, category)
      }

  override def getDeleteMotive(sampleCode: SampleCode): Future[Option[DeletedMotive]] =
    profileDataRepository.getDeletedMotive(sampleCode)

  override def getDesktopProfiles(): Future[Seq[SampleCode]] =
    profileDataRepository.getDesktopProfiles()

  override def isDesktopProfile(globalCode: SampleCode): Future[Option[Boolean]] =
    profileDataRepository.isDesktopProfile(globalCode)

  override def importFromAnotherInstance(profileData: ProfileData, labOrigin: String, labImmediate: String): Future[Unit] =
    profileDataRepository.addExternalProfile(profileData, labOrigin, labImmediate).map(_ => ())

  override def updateUploadStatus(
    globalCode: String, status: Long, motive: Option[String],
    interconnectionError: Option[String], userName: Option[String], operationOriginatedInInstance: String
  ): Future[Either[String, Unit]] =
    profileDataRepository.updateUploadStatus(globalCode, status, motive, interconnectionError, userName, operationOriginatedInInstance)

  override def getProfileUploadStatusByGlobalCode(gc: SampleCode): Future[Option[Long]] =
    profileDataRepository.getProfileUploadStatusByGlobalCode(gc)

  override def getExternalProfileDataByGlobalCode(globalCode: String): Future[Option[ExternalProfileDataRow]] =
    profileDataRepository.getExternalProfileDataByGlobalCode(globalCode)

  override def findProfileDataLocalOrSuperior(globalCode: SampleCode): Future[Option[ProfileData]] =
    profileService.findProfileDataLocalOrSuperior(globalCode)

  override def gefFailedProfilesUploaded(): Future[Seq[ProfileUploadedRow]] =
    profileDataRepository.gefFailedProfilesUploaded()

  override def gefFailedProfilesUploadedDeleted(): Future[Seq[ProfileUploadedRow]] =
    profileDataRepository.gefFailedProfilesUploadedDeleted()

  override def gefFailedProfilesSentDeleted(labCode: String): Future[Seq[ProfileSentRow]] =
    profileDataRepository.gefFailedProfilesSentDeleted(labCode)

  // userName accepted for API symmetry but not stored — PROFILE_SENT has no userName column.
  // Legacy ProfileDataServiceImpl did the same (app/profiledata/ProfileDataService.scala:666).
  override def updateProfileSentStatus(
    globalCode: String, status: Long, motive: Option[String],
    labCode: String, interconnectionError: Option[String], userName: Option[String]
  ): Future[Either[String, Unit]] =
    profileDataRepository.updateProfileSentStatus(globalCode, status, motive, labCode, interconnectionError)

  override def getMtRcrs(): Future[MtRCRS] =
    profileDataRepository.getMtRcrs()

  override def updateInterconnectionError(globalCode: String, status: Long, interconnectionError: String): Future[Either[String, Unit]] =
    profileDataRepository.updateInterconnectionError(globalCode, status, interconnectionError)

  override def addProfileReceivedApproved(labCode: String, globalCode: String, status: Long, userName: String, isCategoryModification: Boolean): Future[Either[String, Unit]] =
    profileDataRepository.addProfileReceivedApproved(labCode, globalCode, status, userName, isCategoryModification)

  override def addProfileReceivedRejected(labCode: String, globalCode: String, status: Long, motive: String, userName: String, isCategoryModification: Boolean): Future[Either[String, Unit]] =
    profileDataRepository.addProfileReceivedRejected(labCode, globalCode, status, motive, userName, isCategoryModification)

  override def updateProfileReceivedStatus(
    labCode: String, globalCode: String, status: Long, motive: String,
    isCategoryModification: Boolean, interconnectionError: String,
    userName: Option[String], operationOriginatedInInstance: String
  ): Future[Either[String, Unit]] =
    profileDataRepository.updateProfileReceivedStatus(labCode, globalCode, status, Some(motive), isCategoryModification, Some(interconnectionError), userName, operationOriginatedInInstance)

  override def getPendingApprovalNotification(labCode: String): Future[Seq[ProfileReceivedRow]] =
    profileDataRepository.getPendingApprovalNotification(labCode)

  override def getPendingRejectionNotification(labCode: String): Future[Seq[ProfileReceivedRow]] =
    profileDataRepository.getPendingRejectionNotification(labCode)

  override def gefFailedProfilesReceivedDeleted(labCode: String): Future[Seq[ProfileReceivedRow]] =
    profileDataRepository.getFailedProfilesReceivedDeleted(labCode)

  override def getIsProfileReplicatedInternalCode(internalCode: String): Future[Boolean] =
    profileDataRepository.getIsProfileReplicatedInternalCode(internalCode)

  override def isEditable(sampleCode: SampleCode, allowFromOtherInstances: Boolean = false): Future[Option[Boolean]] =
    for
      matchesOpt <- matchingService.findMatchingResults(sampleCode)
      readOnly   <- profileService.isReadOnlySampleCode(sampleCode, uploadedIsAllowed = true, allowFromOtherInstances = allowFromOtherInstances)
    yield Some(!(matchesOpt.isDefined || readOnly._1))

  override def countProfiles(): Future[Int] =
    profileDataRepository.countProfiles()
