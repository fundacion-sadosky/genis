package search

import play.api.libs.json.{Format, Json}

case class PaginationSearch(page: Int, pageSize: Int)

object PaginationSearch:
  implicit val searchFormat: Format[PaginationSearch] = Json.format[PaginationSearch]
