package inbox

import javax.inject.Singleton
import types.SampleCode

trait NotificationService {
  def solve(userId: String, info: NotificationInfo): Unit
}

trait NotificationInfo

case class ProfileDataInfo(internalSampleCode: String, globalCode: SampleCode) extends NotificationInfo
case class ProfileDataAssociationInfo(internalSampleCode: String, globalCode: SampleCode) extends NotificationInfo

@Singleton
class NotificationServiceStub extends NotificationService {
  override def solve(userId: String, info: NotificationInfo): Unit = ()
}
