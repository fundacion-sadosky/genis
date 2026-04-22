package pedigree

import _root_.util.PlayEnumUtils

object PedigreeStatus extends Enumeration:
  type PedigreeStatus = Value
  val UnderConstruction, Active, Deleted, Validated, Closed, Open = Value

  implicit val enumTypeFormat: play.api.libs.json.Format[PedigreeStatus.Value] = PlayEnumUtils.enumFormat(PedigreeStatus)
