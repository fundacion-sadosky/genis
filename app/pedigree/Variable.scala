package pedigree

import profile.Profile

object VariableKind extends Enumeration {
  type VariableKind = Value
  val Genotype, Selector, Heterocygote, Homocygote = Value
}

case class Variable(
   name: String,
   marker: Profile.Marker,
   kind: VariableKind.Value,
   alleles: Option[Array[Double]] = None,
   unknown: Boolean = false
)