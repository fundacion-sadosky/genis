package trace

import play.api.libs.json.{Reads, __}
import play.api.libs.functional.syntax.*
import types.SampleCode

case class TraceSearch(
  page: Int = 0,
  pageSize: Int = 30,
  profile: SampleCode,
  user: String,
  isSuperUser: Boolean)

object TraceSearch {
  implicit val searchReads: Reads[TraceSearch] = (
    (__ \ "page").readWithDefault[Int](0) and
    (__ \ "pageSize").readWithDefault[Int](30) and
    (__ \ "profile").read[SampleCode] and
    (__ \ "user").read[String]
  )((page, pageSize, profile, user) => TraceSearch(page, pageSize, profile, user, isSuperUser = false))
}

case class TraceSearchPedigree(
  page: Int = 0,
  pageSize: Int = 30,
  pedigreeId: Int,
  user: String,
  isSuperUser: Boolean)

object TraceSearchPedigree {
  implicit val searchReads: Reads[TraceSearchPedigree] = (
    (__ \ "page").readWithDefault[Int](0) and
    (__ \ "pageSize").readWithDefault[Int](30) and
    (__ \ "pedigreeId").read[Int] and
    (__ \ "user").read[String]
  )((page, pageSize, pedigreeId, user) => TraceSearchPedigree(page, pageSize, pedigreeId, user, isSuperUser = false))
}
