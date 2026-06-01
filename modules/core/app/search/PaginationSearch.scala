package search

import play.api.libs.json.{Json, OFormat}

case class PaginationSearch(page: Int, pageSize: Int)

object PaginationSearch:
  implicit val format: OFormat[PaginationSearch] = Json.format