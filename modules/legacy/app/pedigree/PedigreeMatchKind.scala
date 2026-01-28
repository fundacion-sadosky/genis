package pedigree

import util.PlayEnumUtils

object PedigreeMatchKind extends Enumeration{
  type PedigreeMatchKind = Value
  val DirectLink, Compatibility, MissingInfo = Value

  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(PedigreeMatchKind)
}
