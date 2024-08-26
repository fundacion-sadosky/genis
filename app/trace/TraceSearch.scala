package trace

import play.api.libs.json.Json
import types.SampleCode

case class TraceSearch(
   page: Int = 0,
   pageSize: Int = 30,
   profile: SampleCode,
   user: String,
   isSuperUser: Boolean)

object TraceSearch {
  implicit val searchFormat = Json.format[TraceSearch]
}
case class TraceSearchPedigree(
                        page: Int = 0,
                        pageSize: Int = 30,
                        pedigreeId: Int,
                        user: String,
                        isSuperUser: Boolean)

object TraceSearchPedigree {
  implicit val searchFormat = Json.format[TraceSearchPedigree]
}