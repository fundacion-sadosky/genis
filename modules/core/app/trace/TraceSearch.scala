package trace

import play.api.libs.json.{Format, Json}
import types.SampleCode

case class TraceSearch(
  page: Int = 0,
  pageSize: Int = 30,
  profile: SampleCode,
  user: String,
  isSuperUser: Boolean)

object TraceSearch {
  implicit val searchFormat: Format[TraceSearch] = Json.format[TraceSearch]
}

case class TraceSearchPedigree(
  page: Int = 0,
  pageSize: Int = 30,
  pedigreeId: Int,
  user: String,
  isSuperUser: Boolean)

object TraceSearchPedigree {
  implicit val searchFormat: Format[TraceSearchPedigree] = Json.format[TraceSearchPedigree]
}
