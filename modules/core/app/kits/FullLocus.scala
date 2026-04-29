package kits

import play.api.libs.json.{Json, OFormat}

// TODO: migrate kits.Locus admin flow — stub created to preserve
// MutationService.addLocus legacy signature until Locus migration lands.
case class LocusLink(locus: String, factor: Double, distance: Double)

object LocusLink:
  given OFormat[LocusLink] = Json.format[LocusLink]

case class FullLocus(
  locus: Locus,
  alias: List[String],
  links: List[LocusLink]
)

object FullLocus:
  given OFormat[FullLocus] = Json.format[FullLocus]
