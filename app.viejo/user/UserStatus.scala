package user

import util.PlayEnumUtils

object UserStatus extends Enumeration {
  type UserStatus = Value
  val active, pending, blocked, inactive, pending_reset = Value
  
  implicit val enumUserStatusFormat = PlayEnumUtils.enumFormat(UserStatus)
}