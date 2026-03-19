package profiledata

import play.api.libs.json.{Format, Json}

object ProfileDataModelsJson {
  implicit val profileDataFormat: Format[ProfileData] = Json.format[ProfileData]
  implicit val profileDataWithBatchFormat: Format[ProfileDataWithBatch] = Json.format[ProfileDataWithBatch]
  implicit val profileDataFullFormat: Format[ProfileDataFull] = Json.format[ProfileDataFull]
}