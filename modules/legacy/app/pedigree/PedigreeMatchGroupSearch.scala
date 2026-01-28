package pedigree

import play.api.libs.json._

case class PedigreeMatchGroupSearch(
   user: String,
   isSuperUser: Boolean,
   id: String,
   groupBy: String,
   kind: PedigreeMatchKind.Value,
   page: Int,
   pageSize: Int,
   sortField: String,
   ascending: Boolean,
   status: Option [String] = None,
   idCourCase: Option[Long] = None
)

object PedigreeMatchGroupSearch {
  implicit val searchFormat = Json.format[PedigreeMatchGroupSearch]
}