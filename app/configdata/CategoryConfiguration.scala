package configdata

import play.api.libs.json.Json
import types.AlphanumericId

case class CategoryConfiguration(
    collectionUri: String = "",
    draftUri: String = "",
    minLocusPerProfile: String = "K",
    maxOverageDeviatedLoci: String = "0",
    maxAllelesPerLocus: Int = 6
)

object CategoryConfiguration {
  implicit val format = Json.format[CategoryConfiguration]
}

case class CategoryAssociation(
    `type`: Int,
    categoryRelated: AlphanumericId,
    mismatches: Int
)

object CategoryAssociation {
  implicit val format = Json.format[CategoryAssociation]
}