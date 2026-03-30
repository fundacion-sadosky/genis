package connections

import javax.inject.Singleton
import types.SampleCode

trait InterconnectionService {
  def isFromCurrentInstance(globalCode: SampleCode): Boolean
  def uploadProfileToSuperiorInstance(profileData: profile.Profile, pData: profiledata.ProfileData, userName: String): Unit
}

@Singleton
class InterconnectionServiceStub extends InterconnectionService {
  override def isFromCurrentInstance(globalCode: SampleCode): Boolean = true
  override def uploadProfileToSuperiorInstance(profileData: profile.Profile, pData: profiledata.ProfileData, userName: String): Unit = ()
}
