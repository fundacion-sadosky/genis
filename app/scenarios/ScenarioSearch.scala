package scenarios

import java.util.Date

import play.api.libs.json.Json
import types.SampleCode

/**
  * Created by pdg on 8/17/16.
  */
case class ScenarioSearch(
    profile:SampleCode,
    name: Option[String] = None,
    hourFrom: Option[Date] = None,
    hourUntil: Option[Date] = None,
    state: Option[Boolean] = None,
    ascending: Boolean = false,
    sortField: String = "date")

object ScenarioSearch {
  implicit val scenarioSearchFormat = Json.format[ScenarioSearch]
}
