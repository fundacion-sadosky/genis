package probability

import util.EnumJsonUtils

import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.Format
import play.api.libs.json._

object ProbabilityModel extends Enumeration {
  type ProbabilityModel = Value
  val HardyWeinberg, NRCII41, NRCII410 = Value

  implicit val stringencyReads: Reads[ProbabilityModel] = EnumJsonUtils.enumReads(ProbabilityModel)
  implicit def stringencyWrites: Writes[ProbabilityModel] = EnumJsonUtils.enumWrites

}
