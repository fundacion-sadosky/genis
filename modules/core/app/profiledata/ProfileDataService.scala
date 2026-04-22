package profiledata

import profile.MtRCRS
import types.{AlphanumericId, SampleCode}
import types.AlphanumericId
import play.api.libs.json.*

import scala.concurrent.Future

// View types used by PedigreeService
case class ProfileDataView(
  globalCode: SampleCode,
  category: AlphanumericId,
  internalSampleCode: String,
  assignee: String,
  associated: Boolean
)

object ProfileDataView:
  implicit val format: OFormat[ProfileDataView] = Json.format[ProfileDataView]

case class ProfileDataWithBatch(
  globalCode: SampleCode,
  category: AlphanumericId,
  internalSampleCode: String,
  assignee: String,
  idBatch: Option[Long]
)

case class DeletedMotive(userId: String, motive: String, motiveId: Int)

object DeletedMotive:
  implicit val format: OFormat[DeletedMotive] = Json.format[DeletedMotive]

case class ProfileDataViewCoincidencia(
  globalCode: SampleCode,
  category: String,
  internalSampleCode: String,
  assignee: String,
  lastMatch: java.util.Date,
  hit: Int,
  pending: Int,
  descarte: Int
)

object ProfileDataViewCoincidencia:
  implicit val format: OFormat[ProfileDataViewCoincidencia] = Json.format[ProfileDataViewCoincidencia]

trait ProfileDataRepository {
  def findByCode(globalCode: SampleCode): Future[Option[ProfileData]]
  def isDeleted(globalCode: SampleCode): Future[Option[Boolean]]
  def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]]
  def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]]
  def get(globalCode: SampleCode): Future[Option[ProfileData]]
  def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]]
  def findByCodes(codes: List[SampleCode]): Future[List[ProfileData]]
}

trait ProfileDataService {
  def getMtRcrs(): Future[MtRCRS]
  def deleteProfile(globalCode: SampleCode, motive: DeletedMotive, userId: String): Future[Either[String, Unit]]
  def findByCodes(globalCodes: List[SampleCode]): Future[List[ProfileData]]
}

@javax.inject.Singleton
class ProfileDataRepositoryStub extends ProfileDataRepository {
  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def isDeleted(globalCode: SampleCode): Future[Option[Boolean]] = Future.successful(None)
  override def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]] = Future.successful(None)
  override def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]] = Future.successful(List.empty)
  override def get(globalCode: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]] = Future.successful(None)
  override def findByCodes(codes: List[SampleCode]): Future[List[ProfileData]] = Future.successful(List.empty)
}

@javax.inject.Singleton
class ProfileDataServiceStub extends ProfileDataService {
  override def getMtRcrs(): Future[MtRCRS] = Future.successful(MtRCRS(Map.empty))
  override def deleteProfile(globalCode: SampleCode, motive: DeletedMotive, userId: String): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def findByCodes(globalCodes: List[SampleCode]): Future[List[ProfileData]] = Future.successful(List.empty)
}
