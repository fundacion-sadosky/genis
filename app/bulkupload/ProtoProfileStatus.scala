package bulkupload

import util.PlayEnumUtils

object ProtoProfileStatus extends Enumeration {
  type ProtoProfileStatus = Value
  val Invalid, Incomplete, ReadyForApproval, Approved, Disapproved, Imported, Uploaded, Rejected, DesktopSearch, ReplicatedMatchingProfile = Value

  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(ProtoProfileStatus)
}