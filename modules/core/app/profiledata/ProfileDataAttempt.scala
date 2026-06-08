package profiledata

import play.api.libs.json.{Format, Json}
import types.{AlphanumericId, SampleCode}
import java.util.Date

case class ProfileDataAttempt(
  category: AlphanumericId,
  attorney: Option[String],
  bioMaterialType: Option[String],
  court: Option[String],
  crimeInvolved: Option[String],
  crimeType: Option[String],
  criminalCase: Option[String],
  internalSampleCode: String,
  assignee: String,
  laboratory: Option[String],
  responsibleGeneticist: Option[Long],
  profileExpirationDate: Option[Date],
  sampleDate: Option[Date],
  sampleEntryDate: Option[Date],
  dataFiliation: Option[DataFiliationAttempt]
):
  def pdAttempToPd(labCode: String): ProfileData = ProfileData(
    category = this.category,
    globalCode = null,
    attorney = this.attorney,
    bioMaterialType = this.bioMaterialType,
    court = this.court,
    crimeInvolved = this.crimeInvolved,
    crimeType = this.crimeType,
    criminalCase = this.criminalCase,
    internalSampleCode = this.internalSampleCode,
    assignee = this.assignee,
    laboratory = this.laboratory.getOrElse(labCode),
    deleted = false,
    deletedMotive = None,
    responsibleGeneticist = this.responsibleGeneticist.map(_.toString),
    profileExpirationDate = this.profileExpirationDate,
    sampleDate = this.sampleDate,
    sampleEntryDate = this.sampleEntryDate,
    dataFiliation = this.dataFiliation.map(_.dfAttempToDf),
    isExternal = false
  )

object ProfileDataAttempt:
  implicit val profileAttemptFormat: Format[ProfileDataAttempt] = Json.format
