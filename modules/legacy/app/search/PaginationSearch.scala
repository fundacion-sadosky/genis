package search

import java.util.Date

import play.api.libs.json.Json

case class PaginationSearch(
    page: Int,
    pageSize: Int)

object PaginationSearch {
  implicit val searchFormat = Json.format[PaginationSearch]
}
