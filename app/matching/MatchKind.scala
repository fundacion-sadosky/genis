package matching

import util.PlayEnumUtils

object MatchKind extends Enumeration{
  type MatchKind = Value
  val Normal, MixMix, Restricted, Other, Mitocondrial = Value

  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(MatchKind)
}
