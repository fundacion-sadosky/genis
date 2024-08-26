package pedigree

import util.PlayEnumUtils

object PedigreeStatus extends Enumeration{
  type PedigreeStatus = Value
  val UnderConstruction, Active, Deleted, Validated, Closed, Open = Value

  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(PedigreeStatus)
}
