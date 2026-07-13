package pedigree

import play.api.libs.json.{Format, Json}

// Detalle de LR individual por marcador, para mostrar en el reporte de
// escenario (rojo = exclusion mendeliana tolerada, naranja = requirio un
// salto mutacional para explicarse, sin color = match directo normal).
// classification en {"excluded", "mutation", "normal"}.
case class MarkerLRDetail(lr: Double, classification: String)

object MarkerLRDetail {
  implicit val format: Format[MarkerLRDetail] = Json.format[MarkerLRDetail]
}
