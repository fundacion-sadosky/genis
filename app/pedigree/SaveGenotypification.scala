package pedigree

import kits.AnalysisType
import pedigree.BayesianNetwork.Linkage
import profile.Profile

case class SaveGenotypification(pedigree: PedigreeGenogram, profiles: Array[Profile],
                                frequencyTable: BayesianNetwork.FrequencyTable, analysisType: AnalysisType, linkage: Linkage,
                                mutationModel: Option[MutationModel]) {

}
