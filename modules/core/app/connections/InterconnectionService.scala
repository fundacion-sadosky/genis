package connections

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import types.SampleCode

trait InterconnectionService {
  def isFromCurrentInstance(globalCode: SampleCode): Boolean
  def uploadProfileToSuperiorInstance(profileData: profile.Profile, pData: profiledata.ProfileData, userName: String): Unit
  def uploadProfile(globalCode: String, userName: String)(using ec: ExecutionContext): Future[Either[String, Unit]]
  def isUplpoadableInternalCode(internalCode: String): Future[Boolean]
}

@Singleton
class InterconnectionServiceStub extends InterconnectionService {
  override def isFromCurrentInstance(globalCode: SampleCode): Boolean = true
  override def uploadProfileToSuperiorInstance(profileData: profile.Profile, pData: profiledata.ProfileData, userName: String): Unit = ()
  override def uploadProfile(globalCode: String, userName: String)(using ec: ExecutionContext): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def isUplpoadableInternalCode(internalCode: String): Future[Boolean] = Future.successful(true)
}
