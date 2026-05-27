package pedigree

import kits.AnalysisType
import pedigree.BayesianNetwork.{FrequencyTable, Linkage}
import profile.Profile

// ---------------------------------------------------------------------------
// SaveGenotypification — payload for persisting a computed pedigree genotypification.
// ---------------------------------------------------------------------------

case class SaveGenotypification(
  pedigree: PedigreeGenogram,
  profiles: Array[Profile],
  frequencyTable: FrequencyTable,
  analysisType: AnalysisType,
  linkage: Linkage,
  mutationModel: Option[MutationModel]
)
