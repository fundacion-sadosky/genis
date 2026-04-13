package scenarios

import java.util.Date
import play.api.libs.json.*
import types.SampleCode

case class ScenarioSearch(
  profile: SampleCode,
  name: Option[String] = None,
  hourFrom: Option[Date] = None,
  hourUntil: Option[Date] = None,
  state: Option[Boolean] = None,
  ascending: Boolean = false,
  sortField: String = "date"
)

object ScenarioSearch:
  implicit val scenarioSearchFormat: Format[ScenarioSearch] = Json.format[ScenarioSearch]
