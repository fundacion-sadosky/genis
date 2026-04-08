package trace

import play.api.libs.json.{JsError, JsString, JsSuccess, Reads, Writes}

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
    interconnectionDeletedInSuperior,
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

  implicit val reads: Reads[TraceType] = Reads {
    case JsString(s) => values.find(_.toString == s).map(JsSuccess(_)).getOrElse(JsError(s"TraceType desconocido: $s"))
    case _           => JsError("String expected")
  }
  implicit val writes: Writes[TraceType] = Writes(v => JsString(v.toString))
}
