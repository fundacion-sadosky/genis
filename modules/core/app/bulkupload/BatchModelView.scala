package bulkupload

import play.api.libs.json.{Json, OFormat}

case class BatchModelView(
  idBatch: Long,
  label: Option[String],
  date: String
)

object BatchModelView:
  implicit val format: OFormat[BatchModelView] = Json.format