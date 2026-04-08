package configdata

import play.api.libs.json.{Json, Writes}

case class CategoryConfigurationExport(
  id: Long, category: String, `type`: Int, collectionUri: String = "",
  draftUri: String = "", minLocusPerProfile: String = "K",
  maxOverageDeviatedLoci: String = "0", maxAllelesPerLocus: Int = 6, multiallelic: Boolean = false
)

object CategoryConfigurationExport:
  given Writes[CategoryConfigurationExport] = Json.writes[CategoryConfigurationExport]

case class CategoryAssociationExport(
  id: Long, category: String, categoryRelated: String, mismatchs: Int = 0, `type`: Int
)

object CategoryAssociationExport:
  given Writes[CategoryAssociationExport] = Json.writes[CategoryAssociationExport]

case class CategoryAliasExport(alias: String, category: String)

object CategoryAliasExport:
  given Writes[CategoryAliasExport] = Json.writes[CategoryAliasExport]

case class CategoryMatchingExport(
  id: Long, category: String, categoryRelated: String, priority: Int = 1,
  minimumStringency: String = "ImpossibleMatch", failOnMatch: Option[Boolean] = Some(false),
  forwardToUpper: Option[Boolean] = Some(false), matchingAlgorithm: String = "ENFSI",
  minLocusMatch: Int = 10, mismatchsAllowed: Int = 0, `type`: Int, considerForN: Boolean = true
)

object CategoryMatchingExport:
  given Writes[CategoryMatchingExport] = Json.writes[CategoryMatchingExport]
