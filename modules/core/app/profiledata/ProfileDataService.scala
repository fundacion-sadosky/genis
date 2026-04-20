package profiledata

import profile.MtRCRS
import types.SampleCode

import scala.concurrent.Future

trait ProfileDataRepository {
  def findByCode(globalCode: SampleCode): Future[Option[ProfileData]]
  def isDeleted(globalCode: SampleCode): Future[Option[Boolean]]
  def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]]
  def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]]
  // TODO: migrate profiledata repo — used by BulkUploadService
  def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]]
}

trait ProfileDataService {
  def getMtRcrs(): Future[MtRCRS]
  // TODO: migrate stash profiledata service — used by BulkUploadService and ProtoProfileData controller
  def create(profileData: ProfileDataAttempt): Future[Either[String, SampleCode]]
  def updateProfileData(globalCode: SampleCode, profileData: ProfileDataAttempt): Future[Boolean]
  def get(sampleCode: SampleCode): Future[Option[ProfileData]]
  def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]]
}

@javax.inject.Singleton
class ProfileDataRepositoryStub extends ProfileDataRepository {
  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def isDeleted(globalCode: SampleCode): Future[Option[Boolean]] = Future.successful(None)
  override def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]] = Future.successful(None)
  override def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]] = Future.successful(List.empty)
  override def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]] = Future.successful(None)
}

@javax.inject.Singleton
class ProfileDataServiceStub extends ProfileDataService {
  override def getMtRcrs(): Future[MtRCRS] = Future.successful(MtRCRS(Map.empty))
  override def create(profileData: ProfileDataAttempt): Future[Either[String, SampleCode]] = Future.successful(Left("not implemented"))
  override def updateProfileData(globalCode: SampleCode, profileData: ProfileDataAttempt): Future[Boolean] = Future.successful(false)
  override def get(sampleCode: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]] = Future.successful(None)
}
