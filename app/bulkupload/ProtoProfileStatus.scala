package bulkupload

import util.PlayEnumUtils

object ProtoProfileStatus extends Enumeration {
  type ProtoProfileStatus = Value
  val Invalid, Incomplete, ReadyForApproval, Approved, Disapproved, Imported, Rejected = Value

  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(ProtoProfileStatus)
}