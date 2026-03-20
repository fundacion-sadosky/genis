package configdata

import play.api.libs.json.{Format, Json}
import types.AlphanumericId

case class CategoryConfiguration(
  collectionUri: String = "",
  draftUri: String = "",
  minLocusPerProfile: String = "K",
  maxOverageDeviatedLoci: String = "0",
  maxAllelesPerLocus: Int = 6,
  multiallelic: Boolean = false
)

object CategoryConfiguration {
  implicit val format: Format[CategoryConfiguration] = Json.format[CategoryConfiguration]
}

case class CategoryAssociation(`type`: Int, categoryRelated: AlphanumericId, mismatches: Int)

object CategoryAssociation {
  implicit val format: Format[CategoryAssociation] = Json.format[CategoryAssociation]
}
