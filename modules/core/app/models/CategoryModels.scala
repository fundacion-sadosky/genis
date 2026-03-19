package models

case class CategoryRow(
  id: String,
  group: String,
  name: String,
  isReference: Boolean,
  description: Option[String] = None,
  filiationData: Boolean = false,
  replicate: Boolean = true,
  pedigreeAssociation: Boolean = false,
  allowManualLoading: Boolean = true,
  tipo: Int = 1
)

case class CategoryConfigurationRow(
  id: Long,
  category: String,
  `type`: Int,
  collectionUri: String = "",
  draftUri: String = "",
  minLocusPerProfile: String = "K",
  maxOverageDeviatedLoci: String = "0",
  maxAllelesPerLocus: Int = 6,
  multiallelic: Boolean = false
)

case class CategoryAliasRow(
  alias: String,
  category: String
)

case class CategoryAssociationRow(
  id: Long,
  category: String,
  categoryRelated: String,
  mismatchs: Int = 0,
  `type`: Int
)

case class CategoryMatchingRow(
  id: Long,
  category: String,
  categoryRelated: String,
  mismatchs: Int = 0,
  `type`: Int
)

case class Group(
  id: String,
  name: String
)

// ...existing code...

// ...existing code...
