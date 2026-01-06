package profiledata

import play.api.libs.json.Json

case class DeletedMotive(solicitor: String, motive: String,selectedMotive:Long = 0)

object DeletedMotive {
  implicit val delMotiveFormat = Json.format[DeletedMotive]
}