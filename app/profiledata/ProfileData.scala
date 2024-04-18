package profiledata

import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Format
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.__
import types.SampleCode
import java.util.Date
import types.AlphanumericId
import play.api.libs.json.Json

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
  isExternal: Boolean)

object ProfileData {
  implicit val profiledataFormat: Format[ProfileData] = Json.format
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
                        deletedMotive: Option[DeletedMotive],
                        responsibleGeneticist: Option[String],
                        profileExpirationDate: Option[Date],
                        sampleDate: Option[Date],
                        sampleEntryDate: Option[Date],
                        dataFiliation: Option[DataFiliation],
                        readOnly :Boolean = false,
                        isExternal: Boolean)

object ProfileDataFull {
  implicit val profiledataFullFormat: Format[ProfileDataFull] = Json.format
}

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
  dataFiliation: Option[DataFiliationAttempt]){
  def pdAttempToPd(labCode: String) = {
    ProfileData(
      this.category,
      null,
      this.attorney,
      this.bioMaterialType,
      this.court,
      this.crimeInvolved,
      this.crimeType,
      this.criminalCase,
      this.internalSampleCode,
      this.assignee,
      this.laboratory.getOrElse(labCode),
      false,
      None,
      this.responsibleGeneticist.map(_.toString()),
      this.profileExpirationDate,
      this.sampleDate,
      this.sampleEntryDate,
      this.dataFiliation map {_.dfAttempToDf},
      false)
  }
}

object ProfileDataAttempt {
  implicit val profileAttemptFormat: Format[ProfileDataAttempt] = Json.format
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
                        deletedMotive: Option[DeletedMotive],
                        responsibleGeneticist: Option[String],
                        profileExpirationDate: Option[Date],
                        sampleDate: Option[Date],
                        sampleEntryDate: Option[Date],
                        dataFiliation: Option[DataFiliation],
                        label: Option[String]       )

object ProfileDataWithBatch {
  implicit val profiledataFormat: Format[ProfileDataWithBatch] = Json.format
}




case class ProfileDataViewCoincidencia( globalCode: SampleCode,
                            category: String,
                            internalSampleCode: String,
                            assignee: String,
                            lastMatch: Date,
                                        hit: Int,
                                        pending: Int,
                                        descarte: Int                                      )

object ProfileDataViewCoincidencia {
  implicit val profiledataViewCoincidenciaFormat: Format[ProfileDataViewCoincidencia] = Json.format
}
