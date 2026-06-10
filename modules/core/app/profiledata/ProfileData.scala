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
  deletedMotive: Option[DeletedMotive],
  responsibleGeneticist: Option[String],
  profileExpirationDate: Option[Date],
  sampleDate: Option[Date],
  sampleEntryDate: Option[Date],
  dataFiliation: Option[DataFiliation],
  isExternal: Boolean
)

object ProfileData:
  implicit val format: Format[ProfileData] = Json.format[ProfileData]

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
  deletedMotive: Option[DeletedMotive],
  responsibleGeneticist: Option[String],
  profileExpirationDate: Option[Date],
  sampleDate: Option[Date],
  sampleEntryDate: Option[Date],
  dataFiliation: Option[DataFiliation],
  readOnly: Boolean = false,
  isExternal: Boolean
)

object ProfileDataFull:
  implicit val format: Format[ProfileDataFull] = Json.format[ProfileDataFull]
