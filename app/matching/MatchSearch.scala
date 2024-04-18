package matching

import java.util.Date

import play.api.libs.json.Json
import types.AlphanumericId

case class MatchSearch(
    status: Option[String] = None,
    profile: Option[String] = None,
    categoryId: Option[AlphanumericId] = None,
    laboratoryCode: Option[String] = None,
    hourFrom: Option[Date] = None,
    hourUntil: Option[Date] = None,
    ascending: Option[Boolean] = None,
    sortField: Option[String] = None)

object MatchSearch {
  implicit val searchFormat = Json.format[MatchSearch]
}

