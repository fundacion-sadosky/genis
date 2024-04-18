package types

import util.PlayEnumUtils

object Sex extends Enumeration {
  
  type Sex = Value
  
  val Female, Male, Unknown = Value
  
  implicit val sexFormat = PlayEnumUtils.enumFormat(Sex)
}