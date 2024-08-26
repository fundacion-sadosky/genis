package pedigree

import play.api.libs.json.Json

case class PedCheckResult(pedigreeConsistency:List[PedigreeConsistency],isConsistent:Boolean,consistencyRun:Boolean)

object PedCheckResult {
  implicit val pedCheckResult = Json.format[PedCheckResult]
}