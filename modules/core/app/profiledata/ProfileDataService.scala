package profiledata

import profile.MtRCRS
import types.SampleCode

import scala.concurrent.Future

trait ProfileDataRepository {
  def findByCode(globalCode: SampleCode): Future[Option[ProfileData]]
  def isDeleted(globalCode: SampleCode): Future[Option[Boolean]]
  def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]]
  def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]]
}

trait ProfileDataService {
  def getMtRcrs(): Future[MtRCRS]
}

@javax.inject.Singleton
class ProfileDataRepositoryStub extends ProfileDataRepository {
  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def isDeleted(globalCode: SampleCode): Future[Option[Boolean]] = Future.successful(None)
  override def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]] = Future.successful(None)
  override def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]] = Future.successful(List.empty)
}

@javax.inject.Singleton
class ProfileDataServiceStub extends ProfileDataService {
  override def getMtRcrs(): Future[MtRCRS] = Future.successful(MtRCRS(Map.empty))
}
