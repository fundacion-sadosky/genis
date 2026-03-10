package profiledata

import java.io.File
import scala.concurrent.Future
import types.SampleCode
import models.ProfileDataTables.{
  ExternalProfileDataRow,
  ProfileDataMotiveRow,
  ProfileReceivedRow,
  ProfileSentRow,
  ProfileUploadedRow
}

trait ProfileDataRepository {

  // --- core profile data ---

  // CD. Changed from legacy: returned Future[ProfileData] (throws if not found).
  // Future[Option[ProfileData]] is the correct type for a nullable lookup.
  def get(id: Long): Future[Option[ProfileData]]
  def get(globalCode: SampleCode): Future[Option[ProfileData]]
  def findByCode(globalCode: SampleCode): Future[Option[ProfileData]]
  def findByCodes(globalCodes: List[SampleCode]): Future[Seq[ProfileData]]
  def add(
    profileData: ProfileData,
    completeLabCode: String,
    imageList: Option[List[File]] = None,
    picturesList: Option[List[File]] = None,
    signaturesList: Option[List[File]] = None
  ): Future[SampleCode]
  def updateProfileData(
    globalCode: SampleCode,
    newProfile: ProfileData,
    imageList: Option[List[File]] = None,
    picturesList: Option[List[File]] = None,
    signaturesList: Option[List[File]] = None
  ): Future[Boolean]
  def delete(globalCode: SampleCode, motive: DeletedMotive): Future[Int]
  def removeProfile(globalCode: SampleCode): Future[Either[String, SampleCode]]

  // --- queries ---

  def getProfilesByUser(search: ProfileDataSearch): Future[Seq[ProfileDataFull]]
  def getTotalProfilesByUser(search: ProfileDataSearch): Future[Int]
  def getTotalProfilesByUser(
    userId: String,
    isSuperUser: Boolean,
    category: String = ""
  ): Future[Int]
  def isDeleted(globalCode: SampleCode): Future[Option[Boolean]]
  def isDesktopProfile(globalCode: SampleCode): Future[Option[Boolean]]
  def getDeletedMotive(globalCode: SampleCode): Future[Option[DeletedMotive]]
  def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]]
  def giveGlobalCode(labCode: String): Future[String]
  def getDesktopProfiles(): Future[Seq[SampleCode]]

  // --- resources (filiation binary data) ---

  def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]]

  // --- external profiles (interconnection) ---

  def addExternalProfile(
    profileData: ProfileData,
    labOrigin: String,
    labImmediate: String
  ): Future[SampleCode]
  def getExternalProfileDataByGlobalCode(globalCode: String): Future[Option[ExternalProfileDataRow]]

  // --- upload status (superior instance) ---

  def updateUploadStatus(
    globalCode: String,
    status: Long,
    motive: Option[String],
    interconnectionError: Option[String],
    userName: Option[String]
  ): Future[Either[String, Unit]]
  def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]]
  def findUploadedProfilesByCodes(globalCodes: Seq[SampleCode]): Future[Seq[SampleCode]]
  def gefFailedProfilesUploaded(): Future[List[ProfileUploadedRow]]
  def gefFailedProfilesUploadedDeleted(): Future[List[ProfileUploadedRow]]

  // --- sent status (inferior instance) ---

  def updateProfileSentStatus(
    globalCode: String,
    status: Long,
    motive: Option[String],
    labCode: String,
    interconnectionError: Option[String]
  ): Future[Either[String, Unit]]
  def gefFailedProfilesSentDeleted(labCode: String): Future[List[ProfileSentRow]]

  // --- received status (superior instance receiving from inferior) ---

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
    motive: Option[String],
    isCategoryModification: Boolean,
    interconnectionError: Option[String],
    userName: Option[String]
  ): Future[Either[String, Unit]]
  def updateInterconnectionError(
    globalCode: String,
    status: Long,
    interconnectionError: String
  ): Future[Either[String, Unit]]
  def getProfileReceivedStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]]
  def getPendingApprovalNotification(labCode: String): Future[Seq[ProfileReceivedRow]]
  def getPendingRejectionNotification(labCode: String): Future[Seq[ProfileReceivedRow]]
  def getFailedProfilesReceivedDeleted(labCode: String): Future[Seq[ProfileReceivedRow]]

  // --- replication checks ---

  def getIsProfileReplicated(globalCode: SampleCode): Boolean
  def getIsProfileReplicatedInternalCode(internalCode: String): Boolean

  // --- mitochondrial reference ---

  // TODO: MtRCRS type lives in profile module, not yet migrated.
  // Signature kept for completeness; implement once profile module is available.
  // def getMtRcrs(): Future[MtRCRS]
}