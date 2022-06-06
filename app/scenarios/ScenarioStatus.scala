package scenarios

import util.PlayEnumUtils

object ScenarioStatus extends Enumeration{
  type ScenarioStatus = Value
  val Pending, Validated, Deleted = Value

  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(ScenarioStatus)
}
