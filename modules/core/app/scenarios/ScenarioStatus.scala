package scenarios

import play.api.libs.json.*

object ScenarioStatus extends Enumeration:
  type ScenarioStatus = Value
  val Pending, Validated, Deleted = Value

  implicit val reads: Reads[ScenarioStatus] = Reads:
    case JsString(s) =>
      try JsSuccess(ScenarioStatus.withName(s))
      catch case _: NoSuchElementException => JsError(s"Unknown ScenarioStatus: $s")
    case _ => JsError("String value expected")

  implicit val writes: Writes[ScenarioStatus] = Writes(v => JsString(v.toString))
  implicit val format: Format[ScenarioStatus] = Format(reads, writes)
