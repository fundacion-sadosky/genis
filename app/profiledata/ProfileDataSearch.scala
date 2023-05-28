package profiledata

import play.api.libs.json.Json

case class ProfileDataSearch(
  userId: String,
  isSuperUser: Boolean,
  page: Int = 0,
  pageSize: Int = 30,
  input: String = "",
  active: Boolean = true,
  inactive: Boolean = false,
  notUploaded: Option[Boolean] = None,
  category: String="")

object ProfileDataSearch {
  implicit val format = Json.format[ProfileDataSearch]
}
