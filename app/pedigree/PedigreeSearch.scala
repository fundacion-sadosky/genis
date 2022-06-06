package pedigree

import play.api.libs.json.Json

case class PedigreeSearch(
    page: Int = 0,
    pageSize: Int = 50,
    user: String,
    isSuperUser: Boolean,
    code: Option[String] = None,
    profile: Option[String] = None,
    status: Option[PedigreeStatus.Value] = None,
    sortField: Option[String] = None,
    ascending: Option[Boolean] = None,
    caseType: Option[String] = None
)

object PedigreeSearch {
  implicit val searchFormat = Json.format[PedigreeSearch]
}
