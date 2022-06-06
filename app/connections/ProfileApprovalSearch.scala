package connections

import play.api.libs.json.Json

case class ProfileApprovalSearch(page: Int,
                                 pageSize: Int)

object ProfileApprovalSearch {
  implicit val format = Json.format[ProfileApprovalSearch]
}
