package pedigree

import play.api.libs.json.Json

case class CaseBatchAdd(
  courtcaseId: Long,
  batches: List[Long],
  tipo: Int
)

object CaseBatchAdd:
  implicit val format: play.api.libs.json.OFormat[CaseBatchAdd] = Json.format[CaseBatchAdd]
