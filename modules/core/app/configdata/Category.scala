package configdata

import play.api.libs.json._
import types.AlphanumericId

case class Category(
  id: AlphanumericId,
  group: AlphanumericId,
  name: String,
  isReference: Boolean,
  description: Option[String]
)

object Category {
  type CategoryTree = Map[Group, Seq[Category]]
  implicit val categoryFormat: Format[Category] = Json.format[Category]
}

case class FullCategory(
  id: AlphanumericId,
  name: String,
  description: Option[String],
  group: AlphanumericId,
  isReference: Boolean,
  filiationDataRequired: Boolean,
  replicate: Boolean = true,
  pedigreeAssociation: Boolean = false,
  manualLoading: Boolean = true,
  configurations: Map[Int, CategoryConfiguration],
  associations: Seq[CategoryAssociation],
  aliases: Seq[String],
  matchingRules: Seq[MatchingRule],
  tipo: Option[Int] = None
) {
  def toCategory: Category = Category(id, group, name, isReference, description)
}

object FullCategory {
  import CategoryConfiguration.format

  implicit val mapWrites: Writes[Map[Int, CategoryConfiguration]] =
    Writes(m => Json.toJson(m.map { case (k, v) => k.toString -> Json.toJson(v) }))

  implicit val mapReads: Reads[Map[Int, CategoryConfiguration]] = Reads {
    case obj: JsObject =>
      JsSuccess(obj.fields.map { case (k, v) => k.toInt -> v.as[CategoryConfiguration] }.toMap)
    case _ => JsError("JsObject expected")
  }

  implicit val fullCategoryFormat: Format[FullCategory] = Json.format[FullCategory]
}

case class Group(id: AlphanumericId, name: String, description: Option[String])

object Group {
  implicit val groupFormat: Format[Group] = Json.format[Group]
}

case class CategoryMapping(id: AlphanumericId, idSuperior: String)

object CategoryMapping {
  implicit val format: Format[CategoryMapping] = Json.format[CategoryMapping]
}

case class CategoryMappingList(categoryMappingList: List[CategoryMapping])

object CategoryMappingList {
  implicit val format: Format[CategoryMappingList] = Json.format[CategoryMappingList]
}

case class FullCategoryMapping(id: AlphanumericId, name: String, groupName: String, idSuperior: String)

object FullCategoryMapping {
  implicit val format: Format[FullCategoryMapping] = Json.format[FullCategoryMapping]
}

case class CategoryCombo(id: AlphanumericId, name: String)

object CategoryCombo {
  implicit val format: Format[CategoryCombo] = Json.format[CategoryCombo]
}
