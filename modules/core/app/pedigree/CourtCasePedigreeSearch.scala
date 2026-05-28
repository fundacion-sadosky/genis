package pedigree

import java.sql.Date
import play.api.libs.json.Json

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

object CourtCasePedigreeSearch:
  implicit val format: play.api.libs.json.OFormat[CourtCasePedigreeSearch] =
    Json.format[CourtCasePedigreeSearch]

case class PersonDataSearch(
  page: Int = 0,
  pageSize: Int = 50,
  idCourtCase: Long,
  input: String
)

object PersonDataSearch:
  implicit val format: play.api.libs.json.OFormat[PersonDataSearch] =
    Json.format[PersonDataSearch]
