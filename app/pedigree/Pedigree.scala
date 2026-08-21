package pedigree

import java.util.Date

import pedigree.PedigreeStatus.PedigreeStatus
import play.api.libs.functional.syntax._
import play.api.libs.json._
import types._

case class NodeAlias(override val text: String)
  extends ConstrainedText(text, NodeAlias.validationRe)

object NodeAlias {
  val validationRe = """^[a-zA-Z0-9\-]{1,15}$""".r
  implicit val reads = ConstrainedText.readsOf(NodeAlias.apply)
  implicit val writes = ConstrainedText.writesOf[NodeAlias]
  implicit val qsBinder = ConstrainedText.qsBinderOf(NodeAlias.apply)
  implicit val pathBinder = ConstrainedText.pathBinderOf(NodeAlias.apply)
}

case class Individual(
   alias: NodeAlias,
   idFather: Option[NodeAlias],
   idMother: Option[NodeAlias],
   sex: Sex.Value,
   globalCode: Option[SampleCode],
   unknown: Boolean,
   isReference: Option[Boolean]
)

object Individual {
  implicit val individualFormat = Json.format[Individual]
}

case class PedigreeGenogram(
  _id: Long,
  assignee: String,
  genogram: Seq[Individual],
  status: PedigreeStatus = PedigreeStatus.UnderConstruction,
  frequencyTable: Option[String] = None,
  processed: Boolean = false,
  boundary: Double = 0.5,
  executeScreeningMitochondrial : Boolean = false,
  numberOfMismatches: Option[Int],
  caseType:String,
  mutationModelId: Option[Long] = None,
  idCourtCase: Long,
  maxMendelianExclusions: Option[Int] = None
)

object PedigreeGenogram {
  implicit val longReads: Reads[Long] = new Reads[Long] {
    def reads(jv: JsValue): JsResult[Long] = JsSuccess(jv.as[String].toLong)
  }

  implicit val longWrites: Writes[Long] = new Writes[Long] {
    def writes(l: Long): JsValue = JsString(l.toString)
  }

  implicit val mapFormat: Format[Long] = Format(longReads, longWrites)

  implicit val pedigreeReads: Reads[PedigreeGenogram] = (
    (__ \ "_id").read[Long] and
    (__ \ "assignee").read[String] and
    (__ \ "genogram").read[Seq[Individual]] and
    (__ \ "status").read[PedigreeStatus].orElse(Reads.pure(PedigreeStatus.UnderConstruction)) and
    (__ \ "frequencyTable").readNullable[String] and
    (__ \ "processed").read[Boolean].orElse(Reads.pure(false)) and
    (__ \ "boundary").read[Double].orElse(Reads.pure(0.5)) and
    (__ \ "executeScreeningMitochondrial").read[Boolean].orElse(Reads.pure(false)) and
    (__ \ "numberOfMismatches").readNullable[Int] and
    (__ \ "caseType").read[String] and
    (__ \ "mutationModelId").readNullable[Long] and
    (__ \ "idCourtCase").read[Long] and
    (__ \ "maxMendelianExclusions").readNullable[Int])(PedigreeGenogram.apply _)

  implicit val pedigreeWrites: OWrites[PedigreeGenogram] = (
    (__ \ "_id").write[Long] and
    (__ \ "assignee").write[String] and
    (__ \ "genogram").write[Seq[Individual]] and
    (__ \ "status").write[PedigreeStatus] and
    (__ \ "frequencyTable").writeNullable[String] and
    (__ \ "processed").write[Boolean] and
    (__ \ "boundary").write[Double] and
    (__ \ "executeScreeningMitochondrial").write[Boolean] and
    (__ \ "numberOfMismatches").writeNullable[Int] and
    (__ \ "caseType").write[String] and
    (__ \ "mutationModelId").writeNullable[Long] and
    (__ \ "idCourtCase").write[Long] and
    (__ \ "maxMendelianExclusions").writeNullable[Int])((pedigreeGenogram: PedigreeGenogram) => (
    pedigreeGenogram._id,
    pedigreeGenogram.assignee,
    pedigreeGenogram.genogram,
    pedigreeGenogram.status,
    pedigreeGenogram.frequencyTable,
    pedigreeGenogram.processed,
    pedigreeGenogram.boundary,
    pedigreeGenogram.executeScreeningMitochondrial,
    pedigreeGenogram.numberOfMismatches,
    pedigreeGenogram.caseType,
    pedigreeGenogram.mutationModelId,
    pedigreeGenogram.idCourtCase,
    pedigreeGenogram.maxMendelianExclusions
  ))

  implicit val pedigreeFormat: OFormat[PedigreeGenogram] = OFormat(pedigreeReads, pedigreeWrites)
}

case class MarkerPlainCPT2(marker: String, cpt: PlainCPT2)

object MarkerPlainCPT2 {
  implicit val format: OFormat[MarkerPlainCPT2] = Json.format[MarkerPlainCPT2]
}

case class PedigreeGenotypification(
  _id: Long,
  genotypification: Array[PlainCPT2],
  boundary: Double,
  frequencyTable: String,
  unknowns: Array[String],
  strictGenotypification: Array[PlainCPT2] = Array.empty,
  genotypificationH2: Option[Array[MarkerPlainCPT2]] = None,
  genotypificationWithMarkers: Array[MarkerPlainCPT2] = Array.empty
)

object PedigreeGenotypification {
  implicit val longReads: Reads[Long] = new Reads[Long] {
    def reads(jv: JsValue): JsResult[Long] = JsSuccess(jv.as[String].toLong)
  }

  implicit val longWrites: Writes[Long] = new Writes[Long] {
    def writes(l: Long): JsValue = JsString(l.toString)
  }

  implicit val longFormat: Format[Long] = Format(longReads, longWrites)

  implicit val pedigreeGenotypificationReads: Reads[PedigreeGenotypification] = (
    (__ \ "_id").read[Long] and
    (__ \ "genotypification").read[Array[PlainCPT2]] and
    (__ \ "boundary").read[Double] and
    (__ \ "frequencyTable").read[String] and
    (__ \ "unknowns").read[Array[String]] and
    (__ \ "strictGenotypification").read[Array[PlainCPT2]].orElse(Reads.pure(Array.empty[PlainCPT2])) and
    (__ \ "genotypificationH2").readNullable[Array[MarkerPlainCPT2]].orElse(Reads.pure(None)) and
    (__ \ "genotypificationWithMarkers").read[Array[MarkerPlainCPT2]].orElse(Reads.pure(Array.empty[MarkerPlainCPT2])))(PedigreeGenotypification.apply _)

  implicit val pedigreeGenotypificationWrites: OWrites[PedigreeGenotypification] = (
    (__ \ "_id").write[Long] and
    (__ \ "genotypification").write[Array[PlainCPT2]] and
    (__ \ "boundary").write[Double] and
    (__ \ "frequencyTable").write[String] and
    (__ \ "unknowns").write[Array[String]] and
    (__ \ "strictGenotypification").write[Array[PlainCPT2]] and
    (__ \ "genotypificationH2").writeNullable[Array[MarkerPlainCPT2]] and
    (__ \ "genotypificationWithMarkers").write[Array[MarkerPlainCPT2]])((pedigreeGenotypification: PedigreeGenotypification) => (
    pedigreeGenotypification._id,
    pedigreeGenotypification.genotypification,
    pedigreeGenotypification.boundary,
    pedigreeGenotypification.frequencyTable,
    pedigreeGenotypification.unknowns,
    pedigreeGenotypification.strictGenotypification,
    pedigreeGenotypification.genotypificationH2,
    pedigreeGenotypification.genotypificationWithMarkers))

  implicit val pedigreeGenotypificationFormat: OFormat[PedigreeGenotypification] = OFormat(pedigreeGenotypificationReads, pedigreeGenotypificationWrites)
}

trait Genogram {
  val genogram: Seq[Individual]
}

case class PersonData (
  firstName: Option[String],
  lastName: Option[String],
  sex: Option[Sex.Value],
  dateOfBirth: Option[Date],
  dateOfBirthFrom: Option[Date],
  dateOfBirthTo: Option[Date],
  dateOfMissing: Option[Date],
  nationality: Option[String],
  identification: Option[String],
  height: Option[String],
  weight: Option[String],
  hairColor: Option[String],
  skinColor: Option[String],
  clothing: Option[String],
  alias : String,
  particularities: Option[String]
)

object PersonData{
  implicit val personDataFormat = Json.format[PersonData]
}

trait Persisted {
  val id: Long
  val status: PedigreeStatus = PedigreeStatus.UnderConstruction
}

case class CourtCase(
  id: Long,
  internalSampleCode: String,
  attorney: Option[String],
  court: Option[String],
  assignee: String,
  crimeInvolved: Option[String],
  crimeType: Option[String],
  criminalCase: Option[String],
  override val status: PedigreeStatus,
  personData: List[PersonData],
  caseType:String) extends Persisted

case class CourtCaseFull(
  id: Long,
  internalSampleCode: String,
  attorney: Option[String],
  court: Option[String],
  assignee: String,
  crimeInvolved: Option[String],
  crimeType: Option[String],
  criminalCase: Option[String],
  override val status: PedigreeStatus,
  personData: List[PersonData],
  caseType:String) extends Persisted

object CourtCaseFull{
  implicit val formatCCF = Json.format[CourtCaseFull]
}

case class CourtCaseModelView(
  id: Long,
  internalSampleCode: String,
  attorney: Option[String],
  court: Option[String],
  assignee: String,
  crimeInvolved: Option[String],
  crimeType: Option[String],
  criminalCase: Option[String],
  override val status: PedigreeStatus,
  caseType:String,
  numberOfPendingMatches:Int = 0,
  pedigreeStatus: Option[String] = None) extends Persisted

object CourtCaseModelView {
  implicit val formatCCMV = Json.format[CourtCaseModelView]
}

case class CourtCaseAttempt(
  internalSampleCode: String,
  attorney: Option[String],
  court: Option[String],
  assignee: String,
  crimeInvolved: Option[String],
  crimeType: Option[String],
  criminalCase: Option[String],
  caseType:String)

object CourtCaseAttempt {
  implicit val formatCCA = Json.format[CourtCaseAttempt]
}

case class CaseType (
  id: String,
  name:String
)

object CaseType {
  implicit val caseTypeFormat = Json.format[CaseType]
}

case class PedigreeMetaData (
id: Long,
courtCaseId: Long,
name: String,
creationDate: Date,
override val status: PedigreeStatus,
assignee: String,
courtCaseName: String = "",
consistencyRun:Option[Boolean] = Some(false)
) extends Persisted

object PedigreeMetaData {
  implicit val pedigreeMetaDataFormat = Json.format[PedigreeMetaData]
}

case class PedigreeMetaDataView (
                              id: Long,
                              courtCaseId: Long,
                              name: String,
                              creationDate: Date,
                              status: PedigreeStatus
                            )

object PedigreeMetaDataView {
  implicit val pedigreeMetaDataViewFormat = Json.format[PedigreeMetaDataView]
}

case class PedigreeDataCreation (
  pedigreeMetaData: PedigreeMetaData,
  pedigreeGenogram: Option[PedigreeGenogram],
  copiedFrom:Option[Long] = None
)

object PedigreeDataCreation{
  implicit val pedigreeDataCreationFormat = Json.format[PedigreeDataCreation]
}

case class CollapsingRequest (
                               courtcaseId: Long
                                )

object CollapsingRequest{
  implicit val pedigreeDataCreationFormat = Json.format[CollapsingRequest]
}
