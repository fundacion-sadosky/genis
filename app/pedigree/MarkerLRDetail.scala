package pedigree

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * LR de un marcador en el resultado de parentesco.
  *  - classification: "normal" | "mutation" (salto mutacional) | "singleBand"
  *    (el candidato tiene un solo alelo en este marcador; queda fuera del
  *    cálculo con LR 1).
  *  - singleBandIndividuals: alias de los familiares con un solo alelo en este
  *    marcador; quedaron sin datos ahí.
  */
case class MarkerLRDetail(
  lr: Double,
  classification: String,
  singleBandIndividuals: List[String] = Nil
)

object MarkerLRDetail {
  val SingleBand = "singleBand"

  // Play 2.3 no aplica valores por defecto en Json.format: lectura explícita.
  implicit val reads: Reads[MarkerLRDetail] = (
    (__ \ "lr").read[Double] and
    (__ \ "classification").read[String] and
    (__ \ "singleBandIndividuals").read[List[String]].orElse(Reads.pure(Nil))
  )(MarkerLRDetail.apply _)

  implicit val writes: OWrites[MarkerLRDetail] = Json.writes[MarkerLRDetail]

  implicit val format: OFormat[MarkerLRDetail] = OFormat(reads, writes)
}
