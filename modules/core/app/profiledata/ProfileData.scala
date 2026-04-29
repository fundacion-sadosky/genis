package profiledata

import play.api.libs.json.*
import types.{AlphanumericId, SampleCode}

import java.util.Date

case class ProfileData(
  category: AlphanumericId,
  globalCode: SampleCode,
  attorney: Option[String],
  bioMaterialType: Option[String],
  court: Option[String],
  crimeInvolved: Option[String],
  crimeType: Option[String],
  criminalCase: Option[String],
  internalSampleCode: String,
  assignee: String,
  laboratory: String,
  deleted: Boolean,
  responsibleGeneticist: Option[String],
  profileExpirationDate: Option[Date],
  sampleDate: Option[Date],
  sampleEntryDate: Option[Date],
  isExternal: Boolean
)

object ProfileData {
  implicit val format: Format[ProfileData] = Json.format[ProfileData]
}

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

object DataFiliation {
  implicit val format: Format[DataFiliation] = Json.format[DataFiliation]
}

case class ProfileDataFull(
  category: AlphanumericId,
  globalCode: SampleCode,
  attorney: Option[String],
  bioMaterialType: Option[String],
  court: Option[String],
  crimeInvolved: Option[String],
  crimeType: Option[String],
  criminalCase: Option[String],
  internalSampleCode: String,
  assignee: String,
  laboratory: String,
  deleted: Boolean,
  deletedMotive: Option[String],
  responsibleGeneticist: Option[String],
  profileExpirationDate: Option[Date],
  sampleDate: Option[Date],
  sampleEntryDate: Option[Date],
  dataFiliation: Option[DataFiliation],
  readOnly: Boolean = false,
  isExternal: Boolean
)

object ProfileDataFull {
  implicit val format: Format[ProfileDataFull] = Json.format[ProfileDataFull]
}

case class ProfileDataWithBatch(
  category: AlphanumericId,
  globalCode: SampleCode,
  attorney: Option[String],
  bioMaterialType: Option[String],
  court: Option[String],
  crimeInvolved: Option[String],
  crimeType: Option[String],
  criminalCase: Option[String],
  internalSampleCode: String,
  assignee: String,
  laboratory: String,
  deleted: Boolean,
  deletedMotive: Option[String],
  responsibleGeneticist: Option[String],
  profileExpirationDate: Option[Date],
  sampleDate: Option[Date],
  sampleEntryDate: Option[Date],
  dataFiliation: Option[DataFiliation],
  label: Option[String]
)

object ProfileDataWithBatch {
  implicit val format: Format[ProfileDataWithBatch] = Json.format[ProfileDataWithBatch]
}
