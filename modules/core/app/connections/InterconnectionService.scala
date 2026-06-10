package connections

import jakarta.inject.Singleton
import scala.concurrent.Future
import types.SampleCode

trait InterconnectionService {
  def isFromCurrentInstance(globalCode: SampleCode): Boolean
  def uploadProfileToSuperiorInstance(profileData: profile.Profile, pData: profiledata.ProfileData, userName: String): Unit
  // TODO: migrate interconnection — used by BulkUploadService
  def isUplpoadableInternalCode(sampleName: String): Future[Boolean]
  def uploadProfile(globalCode: String, userId: String): Future[Either[String, Unit]]
  // TODO: migrate interconnection — used by ProfileDataService.deleteProfile
  def inferiorDeleteProfile(globalCode: SampleCode, motive: profiledata.DeletedMotive, supUrl: String, userId: String, labCode: String): Unit
  def sendDeletionToInferior(globalCode: String, motive: profiledata.DeletedMotive, labCode: String, infUrl: String, userId: String, originLabCode: String): Unit
}

@Singleton
class InterconnectionServiceStub extends InterconnectionService {
  override def isFromCurrentInstance(globalCode: SampleCode): Boolean = true
  override def uploadProfileToSuperiorInstance(profileData: profile.Profile, pData: profiledata.ProfileData, userName: String): Unit = ()
  override def isUplpoadableInternalCode(sampleName: String): Future[Boolean] = Future.successful(true)
  override def uploadProfile(globalCode: String, userId: String): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def inferiorDeleteProfile(globalCode: SampleCode, motive: profiledata.DeletedMotive, supUrl: String, userId: String, labCode: String): Unit = ()
  override def sendDeletionToInferior(globalCode: String, motive: profiledata.DeletedMotive, labCode: String, infUrl: String, userId: String, originLabCode: String): Unit = ()
}
