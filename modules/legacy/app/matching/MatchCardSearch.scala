package matching

import java.util.Date

import play.api.libs.json.Json

case class MatchCardSearch(
   user: String,
   isSuperUser: Boolean,
   page: Int = 0,
   pageSize: Int = 30,
   ascending: Boolean = false,
   profile: Option[String] = None,
   hourFrom: Option[Date] = None,
   hourUntil: Option[Date] = None,
   status: Option[MatchGlobalStatus.Value] = None,
   laboratoryCode: Option[String] = None,
   isCollapsing:Option[Boolean] = None,
   courtCaseId: Option[Long] = None,
   categoria : Option[String] = None
)

object MatchCardSearch {
  implicit val searchFormat = Json.format[MatchCardSearch]
}