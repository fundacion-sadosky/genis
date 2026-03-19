package models

import play.api.libs.json.{Format, Json}

object CategoryModelsJson {
  implicit val categoryRowFormat: Format[CategoryRow] = Json.format[CategoryRow]
  implicit val categoryConfigurationRowFormat: Format[CategoryConfigurationRow] = Json.format[CategoryConfigurationRow]
  implicit val categoryAliasRowFormat: Format[CategoryAliasRow] = Json.format[CategoryAliasRow]
  implicit val categoryAssociationRowFormat: Format[CategoryAssociationRow] = Json.format[CategoryAssociationRow]
  implicit val categoryMatchingRowFormat: Format[CategoryMatchingRow] = Json.format[CategoryMatchingRow]
  implicit val groupFormat: Format[Group] = Json.format[Group]
}