package profiledata

import java.util.Date
import play.api.libs.json.{Format, Json}
import types.{AlphanumericId, SampleCode}

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
    category              = this.category,
    globalCode            = SampleCode(""),
    attorney              = this.attorney,
    bioMaterialType       = this.bioMaterialType,
    court                 = this.court,
    crimeInvolved         = this.crimeInvolved,
    crimeType             = this.crimeType,
    criminalCase          = this.criminalCase,
    internalSampleCode    = this.internalSampleCode,
    assignee              = this.assignee,
    laboratory            = this.laboratory.getOrElse(labCode),
    deleted               = false,
    responsibleGeneticist = this.responsibleGeneticist.map(_.toString),
    profileExpirationDate = this.profileExpirationDate,
    sampleDate            = this.sampleDate,
    sampleEntryDate       = this.sampleEntryDate,
    isExternal            = false
  )

object ProfileDataAttempt:
  implicit val format: Format[ProfileDataAttempt] = Json.format