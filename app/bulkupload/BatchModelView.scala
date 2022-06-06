package bulkupload

import play.api.libs.json.{Format, Json}

case class BatchModelView(idBatch:Long,
                          label:Option[String],
//                          totalProfiles: Long,
//                          totalProfilesToImport: Long,
                          date:String)

object BatchModelView {
  implicit val batchModelViewFormat: Format[BatchModelView] = Json.format
}

