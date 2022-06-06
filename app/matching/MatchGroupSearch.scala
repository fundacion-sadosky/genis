package matching

import play.api.libs.json.Json
import types.SampleCode

case class MatchGroupSearch(
   user: String,
   isSuperUser: Boolean,
   globalCode: SampleCode,
   kind: MatchKind.Value,
   page: Int,
   pageSize: Int,
   sortField: String,
   ascending: Boolean,
   isCollapsing:Option[Boolean] = None,
   courtCaseId:Option[Long] = None,
   status: Option [String] = None,
   tipo: Option[Int] = None
)

object MatchGroupSearch {
  implicit val searchFormat = Json.format[MatchGroupSearch]
}