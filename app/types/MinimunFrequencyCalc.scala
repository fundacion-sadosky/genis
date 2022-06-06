package types

import util.PlayEnumUtils

object MinimunFrequencyCalc extends Enumeration {
  type MinimunFrequencyCalc = Value
  
  val NRCII,Weir,BudowleMonsonChakraborty,FminValue = Value

  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(MinimunFrequencyCalc)
}
