package matching

import play.api.libs.json.Reads
import play.api.libs.json.Writes
import util.EnumJsonUtils

object Algorithm extends Enumeration {
  type Algorithm = Value
  val ENFSI, GENIS_MM = Value

  implicit val algorithmReads: Reads[Algorithm] = EnumJsonUtils.enumReads(Algorithm)

  implicit def algorithmWrites: Writes[Algorithm] = EnumJsonUtils.enumWrites

}

