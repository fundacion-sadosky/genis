package profiledata

import java.util.Date
import play.api.libs.json.*
import play.api.libs.functional.syntax.*

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
  signatures: List[Long]
)

object DataFiliation:
  implicit val format: OFormat[DataFiliation] = Json.format

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
  signature: String
):
  def dfAttempToDf: DataFiliation = DataFiliation(
    fullName, nickname, birthday, birthPlace,
    nationality, identification, identificationIssuingAuthority,
    address, Nil, Nil, Nil
  )

object DataFiliationAttempt:
  given reads: Reads[DataFiliationAttempt] = (
    (__ \ "fullName").readNullable[String] and
    (__ \ "nickname").readNullable[String] and
    (__ \ "birthday").readNullable[Date] and
    (__ \ "birthPlace").readNullable[String] and
    (__ \ "nationality").readNullable[String] and
    (__ \ "identification").readNullable[String] and
    (__ \ "identificationIssuingAuthority").readNullable[String] and
    (__ \ "address").readNullable[String] and
    (__ \ "token" \ "inprint").read[String] and
    (__ \ "token" \ "picture").read[String] and
    (__ \ "token" \ "signature").read[String]
  )(DataFiliationAttempt.apply)

  given writes: Writes[DataFiliationAttempt] = (
    (__ \ "fullName").write[Option[String]] and
    (__ \ "nickname").write[Option[String]] and
    (__ \ "birthday").write[Option[Date]] and
    (__ \ "birthPlace").write[Option[String]] and
    (__ \ "nationality").write[Option[String]] and
    (__ \ "identification").write[Option[String]] and
    (__ \ "identificationIssuingAuthority").write[Option[String]] and
    (__ \ "address").write[Option[String]] and
    (__ \ "idImages").write[String]
  )(dfa => (dfa.fullName, dfa.nickname, dfa.birthday, dfa.birthPlace,
            dfa.nationality, dfa.identification, dfa.identificationIssuingAuthority,
            dfa.address, dfa.inprint))

  given format: Format[DataFiliationAttempt] = Format(reads, writes)