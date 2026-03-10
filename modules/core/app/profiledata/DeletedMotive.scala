package profiledata

import play.api.libs.json.{Format, Json}
case class DeletedMotive(solicitor: String, motive: String,selectedMotive:Long = 0)

object DeletedMotive {
  implicit val delMotiveFormat: Format[DeletedMotive] = Json.format[DeletedMotive]
}