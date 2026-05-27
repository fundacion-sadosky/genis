package pedigree

import _root_.util.PlayEnumUtils

object PedigreeMatchKind extends Enumeration:
  type PedigreeMatchKind = Value
  val DirectLink, Compatibility, MissingInfo = Value

  implicit val enumTypeFormat: play.api.libs.json.Format[PedigreeMatchKind.Value] = PlayEnumUtils.enumFormat(PedigreeMatchKind)
