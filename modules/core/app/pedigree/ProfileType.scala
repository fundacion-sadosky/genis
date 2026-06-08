package pedigree

import _root_.util.PlayEnumUtils

object ProfileType extends Enumeration:
  type ProfileType = Value
  val Referencia, Resto = Value

  implicit val enumTypeFormat: play.api.libs.json.Format[ProfileType.Value] =
    PlayEnumUtils.enumFormat(ProfileType)
