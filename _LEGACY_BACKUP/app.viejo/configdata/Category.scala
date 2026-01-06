package configdata

import java.sql.SQLException

import play.api.libs.json.JsError
import play.api.libs.json.JsObject
import play.api.libs.json.JsResult
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import profile.Profile
import types.AlphanumericId

case class Category(
  id: AlphanumericId,
  group: AlphanumericId,
  name: String,
  isReference: Boolean,
  description: Option[String])

object Category {
  
  type CategoryTree = Map[Group, Seq[Category]]
  implicit val categoryFormat = Json.format[Category]
  
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
  tipo : Option[Int] = None                     ){
  def toCategory: Category = {
    Category(id,group,name,isReference,description)
  }
}

object FullCategory {
  implicit def mapWrites: Writes[Map[Int, CategoryConfiguration]] = new Writes[Map[Int, CategoryConfiguration]] {
    override def writes(catMap: Map[Int, CategoryConfiguration]): JsValue =
      Json.toJson( catMap.map {
      case (key, value) => key.toString -> value
    })
  }

  implicit def mapReads: Reads[Map[Int, CategoryConfiguration]] = new Reads[Map[Int, CategoryConfiguration]] {
    override def reads(json: JsValue): JsResult[Map[Int, CategoryConfiguration]] = {
      json match {
        case obj: JsObject =>
          val r = obj.keys
            .map { key => key.toInt -> (obj \ key).as[CategoryConfiguration]}
            .toMap
          JsSuccess(r)
        case other =>
          JsError("JsObject expected as map of categories")
      }
    }
  }

  implicit val fullCategoryFormat = Json.format[FullCategory]
}

case class Group(
  id: AlphanumericId,
  name: String,
  description: Option[String])

object Group {
  implicit val groupFormat = Json.format[Group]
}

case class CategoryMapping(
                            id: AlphanumericId,
                            idSuperior: String)

object CategoryMapping {
  implicit val formatCategoryMapping = Json.format[CategoryMapping]
}

case class CategoryMappingList(
categoryMappingList: List[CategoryMapping])

object CategoryMappingList {
  implicit val formatCategoryMappingList = Json.format[CategoryMappingList]
}

case class FullCategoryMapping(
                            id: AlphanumericId,
                            name: String,
                            groupName: String,
                            idSuperior: String)

object FullCategoryMapping {
  implicit val formatCategoryMapping = Json.format[FullCategoryMapping]
}

case class CategoryCombo(
                     id: AlphanumericId,
                     name: String)

object CategoryCombo {

  implicit val categoryFormat = Json.format[CategoryCombo]

}