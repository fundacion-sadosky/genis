package pedigree

import java.util.Date

import matching.MatchGlobalStatus
import play.api.libs.json.Json

case class PedigreeMatchCardSearch(
   user: String,
   isSuperUser: Boolean,
   group: String,
   page: Int = 0,
   pageSize: Int = 30,
   profile: Option[String] = None,
   hourFrom: Option[Date] = None,
   hourUntil: Option[Date] = None,
   category: Option[String] = None,
   caseType: Option[String] = None,
   status:  Option[MatchGlobalStatus.Value] = None,
   idCourtCase: Option[Long] = None
                                  )

object PedigreeMatchCardSearch {
  implicit val searchFormat = Json.format[PedigreeMatchCardSearch]
}



case class PedMatchCardSearch(
                                    user: String,
                                    isSuperUser: Boolean,
                                    group: String,
                                    page: Int = 0,
                                    pageSize: Int = 30,
                                    profile: Option[String] = None,
                                    hourFrom: Option[Date] = None,
                                    hourUntil: Option[Date] = None,
                                    category: Option[String] = None,
                                    caseType: Option[String] = None,
                                    status:  Option[MatchGlobalStatus.Value] = None,
                                    idCourtCase: Option[Long] = None,
                                    sortField: String,
                                    ascending: Boolean,
                                    pageMatch: Int = 0,
                                    pageSizeMatch: Int = 0
                                  )

object PedMatchCardSearch {
  implicit val searchFormat = Json.format[PedMatchCardSearch]
}
