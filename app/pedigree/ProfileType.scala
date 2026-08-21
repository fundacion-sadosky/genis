package pedigree

import util.PlayEnumUtils

object ProfileType extends Enumeration{
  type ProfileType = Value
  val Referencia, Resto = Value

  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(ProfileType)
}
