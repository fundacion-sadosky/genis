package profiledata

import play.api.libs.json.{Format, Json}

case class ProfileDataSearch(
  userId: String,
  isSuperUser: Boolean,
  page: Int = 0,
  pageSize: Int = ProfileDataSearch.defaultPageSize,
  input: String = "",
  active: Boolean = true,
  inactive: Boolean = false,
  notUploaded: Option[Boolean] = None,
  category: String = ""
)

object ProfileDataSearch {
  implicit val format: Format[ProfileDataSearch] =
    Json.format[ProfileDataSearch]
  private val defaultPageSize = 30
}
