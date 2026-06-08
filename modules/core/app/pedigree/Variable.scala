package pedigree

import profile.Profile

// ---------------------------------------------------------------------------
// VariableKind — classifies the role of a variable in the Bayesian network.
// ---------------------------------------------------------------------------

object VariableKind extends Enumeration:
  type VariableKind = Value
  val Genotype, Selector, Heterocygote, Homocygote = Value

// ---------------------------------------------------------------------------
// Variable — a node in the Bayesian network CPT for a given marker.
// ---------------------------------------------------------------------------

case class Variable(
  name: String,
  marker: Profile.Marker,
  kind: VariableKind.Value,
  alleles: Option[Array[Double]] = None,
  unknown: Boolean = false
)
