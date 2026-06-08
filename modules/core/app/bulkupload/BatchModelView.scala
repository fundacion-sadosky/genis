package bulkupload

import play.api.libs.json.{Format, Json}

case class BatchModelView(
  idBatch: Long,
  label: Option[String],
  date: String
)

object BatchModelView:
  implicit val batchModelViewFormat: Format[BatchModelView] = Json.format
