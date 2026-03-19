package profiledata

import javax.inject._
import java.io.File
import scala.concurrent.{ExecutionContext, Future}
import types.SampleCode
import models.{
  ExternalProfileDataRow,
  ProfileReceivedRow,
  ProfileSentRow,
  ProfileUploadedRow
}

class ProfileDataServiceImpl @Inject()(
  repository: ProfileDataRepository
)(implicit ec: ExecutionContext) extends ProfileDataService {

  // --- core profile data ---

  override def get(id: Long): Future[Option[ProfileData]] =
    repository.get(id)

  override def get(sampleCode: SampleCode): Future[Option[ProfileData]] =
    repository.get(sampleCode)

  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] =
    repository.findByCode(globalCode)

  override def findByCodeWithoutDetails(globalCode: SampleCode): Future[Option[ProfileData]] =
    repository.findByCode(globalCode)

  override def findByCodes(globalCodes: List[SampleCode]): Future[Seq[ProfileData]] =
    repository.findByCodes(globalCodes)

  // TODO: create requires CacheService (image lookup), TraceService, NotificationService,
  // UserService, and CategoryService. Not implemented until those modules are migrated.
  override def create(profileData: ProfileDataAttempt): Future[Either[String, SampleCode]] =
    Future.failed(new NotImplementedError("create: depends on CacheService, TraceService, NotificationService"))

  // TODO: updateProfileData requires CacheService and TraceService.
  override def updateProfileData(
    globalCode: SampleCode,
    profileData: ProfileDataAttempt,
    allowFromOtherInstances: Boolean
  ): Future[Boolean] =
    Future.failed(new NotImplementedError("updateProfileData: depends on CacheService, TraceService"))

  // TODO: updateProfileCategoryData requires CacheService, TraceService, ProfileService.
  override def updateProfileCategoryData(
    globalCode: SampleCode,
    profileData: ProfileDataAttempt,
    userName: String
  ): Future[Option[String]] =
    Future.failed(new NotImplementedError("updateProfileCategoryData: depends on CacheService, TraceService, ProfileService"))

  // --- delete ---

  // TODO: deleteProfile requires PedigreeService, ScenarioRepository, MatchingService,
  // TraceService, InterconnectionService, ConnectionRepository.
  override def deleteProfile(
    globalCode: SampleCode,
    motive: DeletedMotive,
    userId: String,
    validateMPI: Boolean
  ): Future[Either[String, SampleCode]] =
    Future.failed(new NotImplementedError("deleteProfile: depends on PedigreeService, ScenarioRepository, MatchingService"))

  // TODO: delete requires ProfileRepository (profile module, not yet migrated).
  override def delete(globalCode: SampleCode): Future[Either[String, SampleCode]] =
    Future.failed(new NotImplementedError("delete: depends on ProfileRepository (profile module)"))

  override def removeProfile(globalCode: SampleCode): Future[Either[String, SampleCode]] =
    repository.removeProfile(globalCode)

  override def getDeleteMotive(sampleCode: SampleCode): Future[Option[DeletedMotive]] =
    repository.getDeletedMotive(sampleCode)

  // --- queries ---

  override def isDeleted(globalCode: SampleCode): Future[Option[Boolean]] =
    repository.isDeleted(globalCode)

  // TODO: isEditable requires MatchingService and ProfileService.
  override def isEditable(
    sampleCode: SampleCode,
    allowFromOtherInstances: Boolean
  ): Future[Option[Boolean]] =
    Future.failed(new NotImplementedError("isEditable: depends on MatchingService, ProfileService"))

  override def getDesktopProfiles(): Future[Seq[SampleCode]] =
    repository.getDesktopProfiles()

  override def isDesktopProfile(globalCode: SampleCode): Future[Option[Boolean]] =
    repository.isDesktopProfile(globalCode)

  // --- resources ---

  override def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]] =
    repository.getResource(resourceType, id)

  // --- external profiles ---

  override def importFromAnotherInstance(
    profileData: ProfileData,
    labOrigin: String,
    labImmediate: String
  ): Future[Unit] =
    repository.addExternalProfile(profileData, labOrigin, labImmediate).map(_ => ())

  override def getExternalProfileDataByGlobalCode(globalCode: String): Future[Option[ExternalProfileDataRow]] =
    repository.getExternalProfileDataByGlobalCode(globalCode)

  // --- upload status ---

  override def updateUploadStatus(
    globalCode: String,
    status: Long,
    motive: Option[String],
    interconnectionError: Option[String],
    userName: Option[String]
  ): Future[Either[String, Unit]] =
    repository.updateUploadStatus(globalCode, status, motive, interconnectionError, userName)

  override def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]] =
    repository.getProfileUploadStatusByGlobalCode(globalCode)

  override def gefFailedProfilesUploaded(): Future[Seq[ProfileUploadedRow]] =
    repository.gefFailedProfilesUploaded().map(_.toSeq)

  override def gefFailedProfilesUploadedDeleted(): Future[Seq[ProfileUploadedRow]] =
    repository.gefFailedProfilesUploadedDeleted().map(_.toSeq)

  // --- sent status ---

  override def updateProfileSentStatus(
    globalCode: String,
    status: Long,
    motive: Option[String],
    labCode: String,
    interconnectionError: Option[String],
    userName: Option[String]
  ): Future[Either[String, Unit]] =
    repository.updateProfileSentStatus(globalCode, status, motive, labCode, interconnectionError)

  override def gefFailedProfilesSentDeleted(labCode: String): Future[Seq[ProfileSentRow]] =
    repository.gefFailedProfilesSentDeleted(labCode).map(_.toSeq)

  // --- received status ---

  override def addProfileReceivedApproved(
    labCode: String,
    globalCode: String,
    status: Long,
    userName: String,
    isCategoryModification: Boolean
  ): Future[Either[String, Unit]] =
    repository.addProfileReceivedApproved(labCode, globalCode, status, userName, isCategoryModification)

  override def addProfileReceivedRejected(
    labCode: String,
    globalCode: String,
    status: Long,
    motive: String,
    userName: String,
    isCategoryModification: Boolean
  ): Future[Either[String, Unit]] =
    repository.addProfileReceivedRejected(labCode, globalCode, status, motive, userName, isCategoryModification)

  override def updateProfileReceivedStatus(
    labCode: String,
    globalCode: String,
    status: Long,
    motive: String,
    isCategoryModification: Boolean,
    interconnectionError: String,
    userName: Option[String]
  ): Future[Either[String, Unit]] =
    repository.updateProfileReceivedStatus(
      labCode, globalCode, status, Some(motive),
      isCategoryModification, Some(interconnectionError), userName
    )

  override def updateInterconnectionError(
    globalCode: String,
    status: Long,
    interconnectionError: String
  ): Future[Either[String, Unit]] =
    repository.updateInterconnectionError(globalCode, status, interconnectionError)

  override def getPendingApprovalNotification(labCode: String): Future[Seq[ProfileReceivedRow]] =
    repository.getPendingApprovalNotification(labCode)

  override def getPendingRejectionNotification(labCode: String): Future[Seq[ProfileReceivedRow]] =
    repository.getPendingRejectionNotification(labCode)

  override def gefFailedProfilesReceivedDeleted(labCode: String): Future[Seq[ProfileReceivedRow]] =
    repository.getFailedProfilesReceivedDeleted(labCode)

  // --- interconnection helpers ---

  override def shouldSendDeleteToSuperiorInstance(globalCode: SampleCode): Boolean =
    repository.getIsProfileReplicated(globalCode)

  override def shouldSendDeleteToInferiorInstance(globalCode: SampleCode): Boolean = {
    // TODO: same blocking concern as repository.getIsProfileReplicated.
    // Refactor to Future[Boolean] once service layer is fully async.
    import scala.concurrent.Await
    import scala.concurrent.duration._
    Await.result(
      repository.getProfileReceivedStatusByGlobalCode(globalCode).map(_.isDefined),
      3.seconds
    )
  }

  override def getLabFromGlobalCode(globalCode: SampleCode): Option[String] = {
    val parts = globalCode.text.split("-")
    if (parts.length == 4) Some(parts(2)) else None
  }

  override def getIsProfileReplicatedInternalCode(internalCode: String): Boolean =
    repository.getIsProfileReplicatedInternalCode(internalCode)
}