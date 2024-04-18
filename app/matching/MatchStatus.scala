package matching

import util.PlayEnumUtils

object MatchStatus extends Enumeration {
  type MatchStatus = Value
  val hit, discarded, pending, deleted = Value

  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(MatchStatus)
}

object MatchGlobalStatus extends Enumeration {
  type MatchGlobalStatus = Value
  val hit, discarded, pending, conflict, deleted = Value

  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(MatchGlobalStatus)
}
