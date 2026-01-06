package pedigree

import play.api.libs.json.Json
import java.sql.Date

case class CourtCasePedigreeSearch(
                           page: Int = 0,
                           pageSize: Int = 50,
                           idCourtCase: Long,
                           input: Option[String] = None,
                           status: Option[PedigreeStatus.Value] = None,
                           dateFrom: Option[Date] = None,
                           dateUntil: Option[Date] = None,
                           statusProfile: Option[CollapsingStatus.Value] = None,
                           groupedBy: Option[String] = None
                         )

object CourtCasePedigreeSearch {
  implicit val searchFormat = Json.format[CourtCasePedigreeSearch]
}

case class PersonDataSearch(
                             page: Int = 0,
                             pageSize: Int = 50,
                             idCourtCase: Long,
                             input: String
                           )

object PersonDataSearch {
  implicit val PersonDataSearchFormat = Json.format[PersonDataSearch]
}