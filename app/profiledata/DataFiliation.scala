package profiledata

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.util.Date

case class DataFiliation(
  fullName: Option[String],
  nickname: Option[String],
  birthday: Option[Date],
  birthPlace: Option[String],
  nationality: Option[String],
  identification: Option[String],
  identificationIssuingAuthority: Option[String],
  address: Option[String],
  inprints: List[Long],
  pictures: List[Long],
  signatures: List[Long])

object DataFiliation {
  import play.api.libs.json.Json

  implicit val dataFiliationFormat = Json.format[DataFiliation]
}

case class DataFiliationAttempt(
  fullName: Option[String],
  nickname: Option[String],
  birthday: Option[Date],
  birthPlace: Option[String],
  nationality: Option[String],
  identification: Option[String],
  identificationIssuingAuthority: Option[String],
  address: Option[String],
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
    (__ \ "fullName").readNullable[String] ~
    (__ \ "nickname").readNullable[String] ~
    (__ \ "birthday").readNullable[Date] ~
    (__ \ "birthPlace").readNullable[String] ~
    (__ \ "nationality").readNullable[String] ~
    //(__ \ "nationality").read[String].orElse(Reads.pure(null)) ~
    (__ \ "identification").readNullable[String] ~
    (__ \ "identificationIssuingAuthority").readNullable[String] ~
    (__ \ "address").readNullable[String] ~
    (__ \ "token" \ "inprint").read[String] ~
    (__ \ "token" \ "picture").read[String] ~
    (__ \ "token" \ "signature").read[String])(DataFiliationAttempt.apply _)

  implicit val dataFiliationAttemptWrites: Writes[DataFiliationAttempt] = (
    (__ \ "fullName").write[Option[String]] ~
    (__ \ "nickname").write[Option[String]] ~
    (__ \ "birthday").write[Option[Date]] ~
    (__ \ "birthPlace").write[Option[String]] ~
    (__ \ "nationality").write[Option[String]] ~
    (__ \ "identification").write[Option[String]] ~
    (__ \ "identificationIssuingAuthority").write[Option[String]] ~
    (__ \ "address").write[Option[String]] ~
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

//  implicit val dataFiliationAttemptFormat: Format[DataFiliationAttempt] = Json.format

}
