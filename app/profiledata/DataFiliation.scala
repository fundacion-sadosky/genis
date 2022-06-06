package profiledata

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Date

case class DataFiliation(
  fullName: String,
  nickname: String,
  birthday: Date,
  birthPlace: String,
  nationality: String,
  identification: String,
  identificationIssuingAuthority: String,
  address: String,
  inprints: List[Long],
  pictures: List[Long],
  signatures: List[Long])

object DataFiliation {
  import play.api.libs.json.Json

  implicit val dataFiliationFormat = Json.format[DataFiliation]
}

case class DataFiliationAttempt(
  fullName: String,
  nickname: String,
  birthday: Date,
  birthPlace: String,
  nationality: String,
  identification: String,
  identificationIssuingAuthority: String,
  address: String,
  inprint: String,
  picture: String,
  signature: String){
  def dfAttempToDf: DataFiliation = {
    DataFiliation(
      this.fullName,
      this.nickname,
      this.birthday,
      this.birthPlace,
      this.nationality,
      this.identification,
      this.identificationIssuingAuthority,
      this.address,
      Nil,
      Nil,
      Nil)
  }
  
  def isComplete:Boolean = {
    this.fullName != "" || this.nickname != "" ||
    this.birthday != "" || this.birthPlace != "" ||
    this.nationality != "" || this.identification != "" ||
    this.identificationIssuingAuthority != "" ||
    this.address != ""
  }
}

object DataFiliationAttempt {
  import play.api.libs.json.Json
  import play.api.data._
  import play.api.data.Forms._

  implicit val dataFiliationAttemptReads: Reads[DataFiliationAttempt] = (
    (__ \ "fullName").read[String] ~
    (__ \ "nickname").read[String] ~
    (__ \ "birthday").read[Date] ~
    (__ \ "birthPlace").read[String] ~
    (__ \ "nationality").read[String] ~
    (__ \ "identification").read[String] ~
    (__ \ "identificationIssuingAuthority").read[String] ~
    (__ \ "address").read[String] ~
    (__ \ "token" \ "inprint").read[String] ~
    (__ \ "token" \ "picture").read[String] ~
    (__ \ "token" \ "signature").read[String])(DataFiliationAttempt.apply _)

  implicit val dataFiliationAttemptWrites: Writes[DataFiliationAttempt] = (
    (__ \ "fullName").write[String] ~
    (__ \ "nickname").write[String] ~
    (__ \ "birthday").write[Date] ~
    (__ \ "birthPlace").write[String] ~
    (__ \ "nationality").write[String] ~
    (__ \ "identification").write[String] ~
    (__ \ "identificationIssuingAuthority").write[String] ~
    (__ \ "address").write[String] ~
    (__ \ "idImages").write[String])((dataFiliationAttempt: DataFiliationAttempt) => (
      dataFiliationAttempt.fullName,
      dataFiliationAttempt.fullName,
      dataFiliationAttempt.birthday,
      dataFiliationAttempt.birthPlace,
      dataFiliationAttempt.nationality,
      dataFiliationAttempt.identification,
      dataFiliationAttempt.identificationIssuingAuthority,
      dataFiliationAttempt.address,
      dataFiliationAttempt.inprint
    ))

  implicit val dataFiliationAttemptFormat: Format[DataFiliationAttempt] = Format(dataFiliationAttemptReads, dataFiliationAttemptWrites)
}
