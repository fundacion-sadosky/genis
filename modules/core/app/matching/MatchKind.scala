package matching

import _root_.util.PlayEnumUtils

object MatchKind extends Enumeration:
  type MatchKind = Value
  val Normal, MixMix, Restricted, Other, Mitocondrial = Value

  implicit val enumTypeFormat: play.api.libs.json.Format[MatchKind.Value] = PlayEnumUtils.enumFormat(MatchKind)
