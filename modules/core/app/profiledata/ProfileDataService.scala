package profiledata

import java.io.File
import scala.concurrent.Future
import types.SampleCode
import models.{
  ExternalProfileDataRow,
  ProfileReceivedRow,
  ProfileSentRow,
  ProfileUploadedRow
}

trait ProfileDataService {

  // --- core profile data ---

  def get(id: Long): Future[Option[ProfileData]]
  def get(sampleCode: SampleCode): Future[Option[ProfileData]]
  def findByCode(globalCode: SampleCode): Future[Option[ProfileData]]
  def findByCodeWithoutDetails(globalCode: SampleCode): Future[Option[ProfileData]]
  def findByCodes(globalCodes: List[SampleCode]): Future[Seq[ProfileData]]
  def create(profileData: ProfileDataAttempt): Future[Either[String, SampleCode]]
  def updateProfileData(
    globalCode: SampleCode,
    profileData: ProfileDataAttempt,
    allowFromOtherInstances: Boolean = false
  ): Future[Boolean]
  def updateProfileCategoryData(
    globalCode: SampleCode,
    profileData: ProfileDataAttempt,
    userName: String
  ): Future[Option[String]]

  // --- delete ---

  def deleteProfile(
    globalCode: SampleCode,
    motive: DeletedMotive,
    userId: String,
    validateMPI: Boolean = true
  ): Future[Either[String, SampleCode]]
  def delete(globalCode: SampleCode): Future[Either[String, SampleCode]]
  def removeProfile(globalCode: SampleCode): Future[Either[String, SampleCode]]
  def getDeleteMotive(sampleCode: SampleCode): Future[Option[DeletedMotive]]

  // --- queries ---

  def isEditable(
    sampleCode: SampleCode,
    allowFromOtherInstances: Boolean = false
  ): Future[Option[Boolean]]
  def getDesktopProfiles(): Future[Seq[SampleCode]]
  def isDesktopProfile(globalCode: SampleCode): Future[Option[Boolean]]

  // --- resources ---

  def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]]

  // --- external profiles (interconnection) ---

  def importFromAnotherInstance(
    profileData: ProfileData,
    labOrigin: String,
    labImmediate: String
  ): Future[Unit]
  def getExternalProfileDataByGlobalCode(globalCode: String): Future[Option[ExternalProfileDataRow]]

  // TODO: findProfileDataLocalOrSuperior depends on ProfileService (profile module, not yet migrated).
  // def findProfileDataLocalOrSuperior(globalCode: SampleCode): Future[Option[ProfileData]]

  // --- upload status ---

  def updateUploadStatus(
    globalCode: String,
    status: Long,
    motive: Option[String],
    interconnectionError: Option[String],
    userName: Option[String]
  ): Future[Either[String, Unit]]
  def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]]
  def gefFailedProfilesUploaded(): Future[Seq[ProfileUploadedRow]]
  def gefFailedProfilesUploadedDeleted(): Future[Seq[ProfileUploadedRow]]

  // --- sent status ---

  def updateProfileSentStatus(
    globalCode: String,
    status: Long,
    motive: Option[String],
    labCode: String,
    interconnectionError: Option[String],
    userName: Option[String]
  ): Future[Either[String, Unit]]
  def gefFailedProfilesSentDeleted(labCode: String): Future[Seq[ProfileSentRow]]

  // --- received status ---

  def addProfileReceivedApproved(
    labCode: String,
    globalCode: String,
    status: Long,
    userName: String,
    isCategoryModification: Boolean
  ): Future[Either[String, Unit]]
  def addProfileReceivedRejected(
    labCode: String,
    globalCode: String,
    status: Long,
    motive: String,
    userName: String,
    isCategoryModification: Boolean
  ): Future[Either[String, Unit]]
  def updateProfileReceivedStatus(
    labCode: String,
    globalCode: String,
    status: Long,
    motive: String,
    isCategoryModification: Boolean,
    interconnectionError: String,
    userName: Option[String]
  ): Future[Either[String, Unit]]
  def updateInterconnectionError(
    globalCode: String,
    status: Long,
    interconnectionError: String
  ): Future[Either[String, Unit]]
  def getPendingApprovalNotification(labCode: String): Future[Seq[ProfileReceivedRow]]
  def getPendingRejectionNotification(labCode: String): Future[Seq[ProfileReceivedRow]]
  def gefFailedProfilesReceivedDeleted(labCode: String): Future[Seq[ProfileReceivedRow]]

  // --- interconnection helpers ---

  def shouldSendDeleteToSuperiorInstance(globalCode: SampleCode): Boolean
  def shouldSendDeleteToInferiorInstance(globalCode: SampleCode): Boolean
  def getLabFromGlobalCode(globalCode: SampleCode): Option[String]
  def getIsProfileReplicatedInternalCode(internalCode: String): Boolean

  // TODO: removeAll was already unimplemented in legacy (??? body). Omitted.
  // TODO: getMtRcrs depends on MtRCRS type in profile module, not yet migrated.
}