package pedigree

import kits.AnalysisType
import matching.NewMatchingResult
import pedigree.BayesianNetwork.{FrequencyTable, Linkage}
import profile.Profile

// ---------------------------------------------------------------------------
// CalculateProbabilityScenarioPed — input to the Bayesian LR computation for
// a pedigree scenario.
// ---------------------------------------------------------------------------

case class CalculateProbabilityScenarioPed(
  profiles: Array[Profile],
  genogram: Array[Individual],
  frequencyTable: FrequencyTable,
  analysisType: AnalysisType,
  linkage: Linkage,
  verbose: Boolean = false,
  mutationModelType: Option[Long] = None,
  mutationModelData: Option[List[MutationModelData]] = None,
  seenAlleles: Map[String, List[Double]] = Map.empty,
  locusRangeMap: NewMatchingResult.AlleleMatchRange = Map.empty
)
