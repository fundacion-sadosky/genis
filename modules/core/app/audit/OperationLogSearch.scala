package audit

import java.util.Date
import play.api.libs.json.{Format, Json}

case class OperationLogSearch(
  lotId:      Long,
  page:       Int             = 0,
  pageSize:   Int             = 50,
  user:       Option[String]  = None,
  operations: Option[List[String]] = None,
  hourFrom:   Option[Date]    = None,
  hourUntil:  Option[Date]    = None,
  result:     Option[Boolean] = None,
  ascending:  Option[Boolean] = None,
  sortField:  Option[String]  = None
)

object OperationLogSearch {
  given searchFormat: Format[OperationLogSearch] = Json.format[OperationLogSearch]
}
