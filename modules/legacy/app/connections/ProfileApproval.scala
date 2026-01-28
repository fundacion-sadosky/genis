package connections

import play.api.libs.json.Json

case class ProfileApproval(
                       globalCode: String
                     )

object ProfileApproval {
  implicit val format = Json.format[ProfileApproval]
}
