package kits

import play.api.libs.json.Json

case class LocusLink(
    locus: String,
    factor: Double,
    distance: Double)

object LocusLink {
  implicit val locusLinkFormat = Json.format[LocusLink]
}

case class FullLocus(
    locus: Locus,
    alias: List[String],
    links: List[LocusLink])

object FullLocus {
  implicit val fullLocusFormat = Json.format[FullLocus]
}
