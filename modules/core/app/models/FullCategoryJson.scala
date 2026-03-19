package models

import play.api.libs.json.{Format, Json}
import models.CategoryModelsJson._

object FullCategoryJson {
  implicit val fullCategoryFormat: Format[FullCategory] = Json.format[FullCategory]
}