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
