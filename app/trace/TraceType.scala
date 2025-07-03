package trace

import util.PlayEnumUtils

object TraceType extends Enumeration {
  type TraceType = Value
  val analysis,
  matchProcess,
  `match`,
  hit,
  discard,
  profileData,
  association,
  delete,
  categoryModification,
  superiorInstanceCategoryModification,
  superiorInstanceCategoryRejection,
  interconectionUpdload,
  interconectionAproved,
  interconnectionDeletedInInferior,
  interconnectiodDeletedInSuperior,
  interconectionCategoryAproved,
  importedFromInferior,
  interconectionRejected,
  interconectionCategoryRejected,
  pedigreeMatchProcess,
  pedigreeMatch,
  pedigreeDiscard,
  pedigreeConfirm,
  pedigreeStatusChange,
  pedigriCopy,
  pedigriEdit,
  pedigriNewScenario,
  pedigreeMatch2,
  pedigreeDiscard2,
  pedigreeConfirm2 = Value
  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(TraceType)
}
