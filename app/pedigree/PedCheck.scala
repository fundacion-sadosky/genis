package pedigree

import play.api.libs.json.Json

case class PedCheck(id: Long, idPedigree: Long, locus: String,globalCode: String)

object PedCheck {
  implicit val pedCheck = Json.format[PedCheck]
}