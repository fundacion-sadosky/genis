package pedigree

import com.google.inject.Provider
import java.util.Date
import pedigree.PedigreeStatus.PedigreeStatus
import play.api.Logger
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import types.{ConstrainedText, SampleCode}

import scala.concurrent.{ExecutionContext, Future}

// ---------------------------------------------------------------------------
// PedigreeGenotypification (migrated from legacy pedigree.Pedigree)
// Stores pre-computed Bayesian CPT for each pedigree + unknowns info.
// ---------------------------------------------------------------------------

case class PedigreeGenotypification(
  _id: Long,
  genotypification: Array[PlainCPT2],
  boundary: Double,
  frequencyTable: String,
  unknowns: Array[String]
)

object PedigreeGenotypification:
  implicit val longReads: Reads[Long]   = Reads { jv => JsSuccess(jv.as[String].toLong) }
  implicit val longWrites: Writes[Long] = Writes { l => JsString(l.toString) }
  implicit val longFormat: Format[Long] = Format(longReads, longWrites)

  implicit val pedigreeGenotypificationReads: Reads[PedigreeGenotypification] = (
    (__ \ "_id").read[Long] ~
    (__ \ "genotypification").read[Array[PlainCPT2]] ~
    (__ \ "boundary").read[Double] ~
    (__ \ "frequencyTable").read[String] ~
    (__ \ "unknowns").read[Array[String]]
  )(PedigreeGenotypification.apply)

  implicit val pedigreeGenotypificationWrites: OWrites[PedigreeGenotypification] = (
    (__ \ "_id").write[Long] ~
    (__ \ "genotypification").write[Array[PlainCPT2]] ~
    (__ \ "boundary").write[Double] ~
    (__ \ "frequencyTable").write[String] ~
    (__ \ "unknowns").write[Array[String]]
  )((p: PedigreeGenotypification) => (p._id, p.genotypification, p.boundary, p.frequencyTable, p.unknowns))

  implicit val pedigreeGenotypificationFormat: OFormat[PedigreeGenotypification] =
    OFormat(pedigreeGenotypificationReads, pedigreeGenotypificationWrites)

// ---------------------------------------------------------------------------
// Individual & PedigreeGenogram (migrated from legacy pedigree.Pedigree)
// ---------------------------------------------------------------------------

object Sex extends Enumeration:
  type Sex = Value
  val Male, Female, Unknown = Value
  implicit val format: Format[Sex.Value] = _root_.util.PlayEnumUtils.enumFormat(Sex)

case class Individual(
  alias: NodeAlias,
  idFather: Option[NodeAlias],
  idMother: Option[NodeAlias],
  sex: Sex.Value,
  globalCode: Option[SampleCode],
  unknown: Boolean,
  isReference: Option[Boolean]
)

object Individual:
  implicit val individualFormat: play.api.libs.json.OFormat[Individual] = Json.format[Individual]

case class PedigreeGenogram(
  _id: Long,
  assignee: String,
  genogram: Seq[Individual],
  status: PedigreeStatus = PedigreeStatus.UnderConstruction,
  frequencyTable: Option[String] = None,
  processed: Boolean = false,
  boundary: Double = 0.5,
  executeScreeningMitochondrial: Boolean = false,
  numberOfMismatches: Option[Int],
  caseType: String,
  mutationModelId: Option[Long] = None,
  idCourtCase: Long
)

object PedigreeGenogram:
  implicit val longReads: Reads[Long]   = Reads { jv => JsSuccess(jv.as[String].toLong) }
  implicit val longWrites: Writes[Long] = Writes { l => JsString(l.toString) }
  implicit val mapFormat: Format[Long]  = Format(longReads, longWrites)

  implicit val pedigreeReads: Reads[PedigreeGenogram] = (
    (__ \ "_id").read[Long] ~
    (__ \ "assignee").read[String] ~
    (__ \ "genogram").read[Seq[Individual]] ~
    (__ \ "status").read[PedigreeStatus].orElse(Reads.pure(PedigreeStatus.UnderConstruction)) ~
    (__ \ "frequencyTable").readNullable[String] ~
    (__ \ "processed").read[Boolean].orElse(Reads.pure(false)) ~
    (__ \ "boundary").read[Double].orElse(Reads.pure(0.5)) ~
    (__ \ "executeScreeningMitochondrial").read[Boolean].orElse(Reads.pure(false)) ~
    (__ \ "numberOfMismatches").readNullable[Int] ~
    (__ \ "caseType").read[String] ~
    (__ \ "mutationModelId").readNullable[Long] ~
    (__ \ "idCourtCase").read[Long]
  )(PedigreeGenogram.apply)

  implicit val pedigreeWrites: OWrites[PedigreeGenogram] = (
    (__ \ "_id").write[Long] ~
    (__ \ "assignee").write[String] ~
    (__ \ "genogram").write[Seq[Individual]] ~
    (__ \ "status").write[PedigreeStatus] ~
    (__ \ "frequencyTable").writeNullable[String] ~
    (__ \ "processed").write[Boolean] ~
    (__ \ "boundary").write[Double] ~
    (__ \ "executeScreeningMitochondrial").write[Boolean] ~
    (__ \ "numberOfMismatches").writeNullable[Int] ~
    (__ \ "caseType").write[String] ~
    (__ \ "mutationModelId").writeNullable[Long] ~
    (__ \ "idCourtCase").write[Long]
  )((p: PedigreeGenogram) => (p._id, p.assignee, p.genogram, p.status, p.frequencyTable,
      p.processed, p.boundary, p.executeScreeningMitochondrial, p.numberOfMismatches,
      p.caseType, p.mutationModelId, p.idCourtCase))

// ---------------------------------------------------------------------------
// PedigreeMetaData / PedigreeDataCreation (used by PedigreeMatchesService)
// ---------------------------------------------------------------------------

case class PedigreeMetaData(
  id: Long,
  courtCaseId: Long,
  name: String,
  courtCaseName: String = "",
  assignee: String,
  creationDate: Option[Date] = None,
  status: PedigreeStatus = PedigreeStatus.UnderConstruction,
  consistencyRun: Option[Boolean] = Some(false)
)

object PedigreeMetaData:
  implicit val format: play.api.libs.json.OFormat[PedigreeMetaData] = Json.format[PedigreeMetaData]

case class PedigreeDataCreation(
  pedigreeMetaData: PedigreeMetaData,
  pedigreeGenogram: Option[PedigreeGenogram] = None,
  copiedFrom: Option[Long] = None
)

object PedigreeDataCreation:
  implicit val format: play.api.libs.json.OFormat[PedigreeDataCreation] = Json.format[PedigreeDataCreation]

// ---------------------------------------------------------------------------
// PersonData — filiation data for a victim/missing person in a court case.
// ---------------------------------------------------------------------------

case class PersonData(
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
  alias: String,
  particularities: Option[String]
)

object PersonData:
  implicit val format: play.api.libs.json.OFormat[PersonData] = Json.format[PersonData]

// ---------------------------------------------------------------------------
// CourtCase and related types (migrated from legacy pedigree.Pedigree)
// ---------------------------------------------------------------------------

trait Persisted:
  val id: Long
  val status: PedigreeStatus = PedigreeStatus.UnderConstruction

trait Genogram:
  val genogram: Seq[Individual]

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
  caseType: String
) extends Persisted

object CourtCase:
  implicit val format: play.api.libs.json.OFormat[CourtCase] = Json.format[CourtCase]

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
  caseType: String
) extends Persisted

object CourtCaseFull:
  implicit val format: play.api.libs.json.OFormat[CourtCaseFull] = Json.format[CourtCaseFull]

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
  caseType: String,
  numberOfPendingMatches: Int = 0
) extends Persisted

object CourtCaseModelView:
  implicit val format: play.api.libs.json.OFormat[CourtCaseModelView] = Json.format[CourtCaseModelView]

case class CourtCaseAttempt(
  internalSampleCode: String,
  attorney: Option[String],
  court: Option[String],
  assignee: String,
  crimeInvolved: Option[String],
  crimeType: Option[String],
  criminalCase: Option[String],
  caseType: String
)

object CourtCaseAttempt:
  implicit val format: play.api.libs.json.OFormat[CourtCaseAttempt] = Json.format[CourtCaseAttempt]

case class CaseType(id: String, name: String)

object CaseType:
  implicit val format: play.api.libs.json.OFormat[CaseType] = Json.format[CaseType]

case class PedigreeMetaDataView(
  id: Long,
  courtCaseId: Long,
  name: String,
  creationDate: Option[Date],
  status: PedigreeStatus
)

object PedigreeMetaDataView:
  implicit val format: play.api.libs.json.OFormat[PedigreeMetaDataView] = Json.format[PedigreeMetaDataView]

case class CollapsingRequest(courtcaseId: Long)

object CollapsingRequest:
  implicit val format: play.api.libs.json.OFormat[CollapsingRequest] = Json.format[CollapsingRequest]

// ---------------------------------------------------------------------------
// PedigreeRepository
// TODO: migrate pedigree (MongoPedigreeRepository full implementation)
// ---------------------------------------------------------------------------

trait PedigreeRepository:
  def addGenogram(genogram: PedigreeGenogram): Future[Either[String, Long]]
  def get(id: Long): Future[Option[PedigreeGenogram]]
  def changeStatus(courtCaseId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]]
  def findByProfile(profile: String): Future[Seq[Long]]
  def setProcessed(pedigreeId: Long): Future[Either[String, Long]]
  def getUnprocessed(): Future[Seq[Long]]
  def deleteFisicalPedigree(pedigreeId: Long): Future[Either[String, Long]]
  def countByProfile(globalCode: String): Future[Int]
  def countByProfileIdPedigrees(globalCode: String, idsPedigrees: Seq[String]): Future[Int]
  def getActivePedigreesByCaseType(caseType: String): Future[Seq[PedigreeGenogram]]
  def getPedigreeByCourtCaseId(courtCaseId: Long): Future[List[PedigreeGenogram]]

@jakarta.inject.Singleton
class PedigreeRepositoryStub extends PedigreeRepository:
  override def addGenogram(genogram: PedigreeGenogram): Future[Either[String, Long]] = Future.successful(Right(genogram._id))
  override def get(id: Long): Future[Option[PedigreeGenogram]] = Future.successful(None)
  override def changeStatus(courtCaseId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]] = Future.successful(Right(courtCaseId))
  override def findByProfile(profile: String): Future[Seq[Long]] = Future.successful(Seq.empty)
  override def setProcessed(pedigreeId: Long): Future[Either[String, Long]] = Future.successful(Right(pedigreeId))
  override def getUnprocessed(): Future[Seq[Long]] = Future.successful(Seq.empty)
  override def deleteFisicalPedigree(pedigreeId: Long): Future[Either[String, Long]] = Future.successful(Right(pedigreeId))
  override def countByProfile(globalCode: String): Future[Int] = Future.successful(0)
  override def countByProfileIdPedigrees(globalCode: String, idsPedigrees: Seq[String]): Future[Int] = Future.successful(0)
  override def getActivePedigreesByCaseType(caseType: String): Future[Seq[PedigreeGenogram]] = Future.successful(Seq.empty)
  override def getPedigreeByCourtCaseId(courtCaseId: Long): Future[List[PedigreeGenogram]] = Future.successful(List.empty)

// ---------------------------------------------------------------------------
// PedigreeDataRepository
// TODO: migrate pedigree (SlickPedigreeDataRepository full implementation)
// ---------------------------------------------------------------------------

trait PedigreeDataRepository:
  def getAllCourtCases(pedigreeIds: Option[Seq[Long]], pedigreeSearch: PedigreeSearch): Future[Seq[CourtCaseModelView]]
  def getTotalCourtCases(pedigreeIds: Option[Seq[Long]], pedigreeSearch: PedigreeSearch): Future[Int]
  def createCourtCase(courtCase: CourtCaseAttempt): Future[Either[String, Long]]
  def createMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Long]]
  def getCourtCase(courtCaseId: Long): Future[Option[CourtCase]]
  def getMetadata(personDataSearch: PersonDataSearch): Future[List[PersonData]]
  def updateCourtCase(id: Long, courtCase: CourtCaseAttempt): Future[Either[String, Long]]
  def updateMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Long]]
  def changeCourCaseStatus(courtCaseId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]]
  def getProfiles(courtCasePedigreeSearch: CourtCasePedigreeSearch, globalCodes: List[String] = Nil, typeProfile: Option[String]): Future[List[CourtCasePedigree]]
  def getTotalProfiles(courtCasePedigreeSearch: CourtCasePedigreeSearch, globalCodes: List[String] = Nil, typeProfile: Option[String]): Future[Int]
  def getTotalProfilesOccurenceInCase(globalCodes: String): Future[Int]
  def getTotalProfilesNodeAssociation(courtCasePedigreeSearch: CourtCasePedigreeSearch, globalCodes: List[String] = Nil, typeProfile: Option[String], profilesCod: List[String]): Future[Int]
  def getTotalMetadata(personDataSearch: PersonDataSearch): Future[Int]
  def addProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]]
  def removeProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]]
  def removeMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Unit]]
  def getProfilesFromBatches(courtCase: Long, batches: List[Long], tipo: Int): Future[List[(String, Boolean)]]
  def getCaseTypes(): Future[Seq[CaseType]]
  def getProfilesNodeAssociation(courtCasePedigreeSearch: CourtCasePedigreeSearch, globalCodes: List[String] = Nil, typeProfile: Option[String], profilesCod: List[String]): Future[List[ProfileNodeAssociation]]
  def getPedigrees(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Seq[PedigreeMetaDataView]]
  def getTotalPedigrees(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Int]
  def getPedigreeMetaData(pedigreeId: Long): Future[Option[PedigreeDataCreation]]
  def createOrUpdatePedigreeMetadata(pedigreeMetaData: PedigreeMetaData): Future[Either[String, Long]]
  def changePedigreeStatus(pedigreeId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]]
  def deleteFisicalPedigree(pedigreeId: Long): Future[Either[String, Long]]
  def getProfilesToDelete(courtCaseId: Long): Future[Seq[SampleCode]]
  def getProfilesForCollapsing(idCourtCase: Long): Future[List[(String, Boolean)]]
  def getCourtCaseOfPedigree(pedigreeId: Long): Future[Option[CourtCase]]
  def getActiveNNProfiles(courtCaseId: Long): Future[Seq[SampleCode]]
  def disassociateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]]
  def getTotalProfilesInactive(courtCasePedigreeSearch: CourtCasePedigreeSearch, globalCode: List[String]): Future[Int]
  def getProfilesInactive(courtCasePedigreeSearch: CourtCasePedigreeSearch, globalcode: List[String]): Future[List[CourtCasePedigree]]
  def associateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]]
  def changePedigreeConsistencyFlag(pedigreeId: Long, consistencyRun: Boolean = true): Future[Either[String, Long]]
  def doCleanConsistency(pedigreeId: Long): Future[Either[String, Long]]
  def getPedigreeDescriptionById(pedigreeId: Long): Future[(Option[String], Option[String])]

@jakarta.inject.Singleton
class PedigreeDataRepositoryStub extends PedigreeDataRepository:
  override def getAllCourtCases(pedigreeIds: Option[Seq[Long]], pedigreeSearch: PedigreeSearch): Future[Seq[CourtCaseModelView]] = Future.successful(Seq.empty)
  override def getTotalCourtCases(pedigreeIds: Option[Seq[Long]], pedigreeSearch: PedigreeSearch): Future[Int] = Future.successful(0)
  override def createCourtCase(courtCase: CourtCaseAttempt): Future[Either[String, Long]] = Future.successful(Right(0L))
  override def createMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Long]] = Future.successful(Right(0L))
  override def getCourtCase(courtCaseId: Long): Future[Option[CourtCase]] = Future.successful(None)
  override def getMetadata(personDataSearch: PersonDataSearch): Future[List[PersonData]] = Future.successful(List.empty)
  override def updateCourtCase(id: Long, courtCase: CourtCaseAttempt): Future[Either[String, Long]] = Future.successful(Right(id))
  override def updateMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Long]] = Future.successful(Right(idCourtCase))
  override def changeCourCaseStatus(courtCaseId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]] = Future.successful(Right(courtCaseId))
  override def getProfiles(courtCasePedigreeSearch: CourtCasePedigreeSearch, globalCodes: List[String], typeProfile: Option[String]): Future[List[CourtCasePedigree]] = Future.successful(List.empty)
  override def getTotalProfiles(courtCasePedigreeSearch: CourtCasePedigreeSearch, globalCodes: List[String], typeProfile: Option[String]): Future[Int] = Future.successful(0)
  override def getTotalProfilesOccurenceInCase(globalCodes: String): Future[Int] = Future.successful(0)
  override def getTotalProfilesNodeAssociation(courtCasePedigreeSearch: CourtCasePedigreeSearch, globalCodes: List[String], typeProfile: Option[String], profilesCod: List[String]): Future[Int] = Future.successful(0)
  override def getTotalMetadata(personDataSearch: PersonDataSearch): Future[Int] = Future.successful(0)
  override def addProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def removeProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def removeMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def getProfilesFromBatches(courtCase: Long, batches: List[Long], tipo: Int): Future[List[(String, Boolean)]] = Future.successful(List.empty)
  override def getCaseTypes(): Future[Seq[CaseType]] = Future.successful(Seq.empty)
  override def getProfilesNodeAssociation(courtCasePedigreeSearch: CourtCasePedigreeSearch, globalCodes: List[String], typeProfile: Option[String], profilesCod: List[String]): Future[List[ProfileNodeAssociation]] = Future.successful(List.empty)
  override def getPedigrees(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Seq[PedigreeMetaDataView]] = Future.successful(Seq.empty)
  override def getTotalPedigrees(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Int] = Future.successful(0)
  override def getPedigreeMetaData(pedigreeId: Long): Future[Option[PedigreeDataCreation]] = Future.successful(None)
  override def createOrUpdatePedigreeMetadata(pedigreeMetaData: PedigreeMetaData): Future[Either[String, Long]] = Future.successful(Right(pedigreeMetaData.id))
  override def changePedigreeStatus(pedigreeId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]] = Future.successful(Right(pedigreeId))
  override def deleteFisicalPedigree(pedigreeId: Long): Future[Either[String, Long]] = Future.successful(Right(pedigreeId))
  override def getProfilesToDelete(courtCaseId: Long): Future[Seq[SampleCode]] = Future.successful(Seq.empty)
  override def getProfilesForCollapsing(idCourtCase: Long): Future[List[(String, Boolean)]] = Future.successful(List.empty)
  override def getCourtCaseOfPedigree(pedigreeId: Long): Future[Option[CourtCase]] = Future.successful(None)
  override def getActiveNNProfiles(courtCaseId: Long): Future[Seq[SampleCode]] = Future.successful(Seq.empty)
  override def disassociateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def getTotalProfilesInactive(courtCasePedigreeSearch: CourtCasePedigreeSearch, globalCode: List[String]): Future[Int] = Future.successful(0)
  override def getProfilesInactive(courtCasePedigreeSearch: CourtCasePedigreeSearch, globalcode: List[String]): Future[List[CourtCasePedigree]] = Future.successful(List.empty)
  override def associateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def changePedigreeConsistencyFlag(pedigreeId: Long, consistencyRun: Boolean): Future[Either[String, Long]] = Future.successful(Right(pedigreeId))
  override def doCleanConsistency(pedigreeId: Long): Future[Either[String, Long]] = Future.successful(Right(pedigreeId))
  override def getPedigreeDescriptionById(pedigreeId: Long): Future[(Option[String], Option[String])] = Future.successful((None, None))

// ---------------------------------------------------------------------------
// PedigreeService
// ---------------------------------------------------------------------------

trait PedigreeService:
  def getAllCourtCases(pedigreeSearch: PedigreeSearch): Future[Seq[CourtCaseModelView]]
  def getTotalCourtCases(pedigreeSearch: PedigreeSearch): Future[Int]
  def createCourtCase(courtCase: CourtCaseAttempt): Future[Either[String, Long]]
  def createMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Long]]
  def addGenogram(genogram: PedigreeGenogram): Future[Either[String, Long]]
  def getCourtCase(courtCaseId: Long, userId: String, isSuperUser: Boolean): Future[Option[CourtCaseFull]]
  def getMetadata(personDataSearch: PersonDataSearch): Future[List[PersonData]]
  def getPedigree(pedigreeId: Long): Future[Option[PedigreeDataCreation]]
  def updateCourtCase(id: Long, courtCase: CourtCaseAttempt, isSuperUser: Boolean): Future[Either[String, Long]]
  def updateMetadata(idCourtCase: Long, assignee: String, personData: PersonData, isSuperUser: Boolean): Future[Either[String, Long]]
  def changePedigreeStatus(pedigreeId: Long, status: PedigreeStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, Long]]
  def getCaseTypes(): Future[Seq[CaseType]]
  def getProfiles(courtCasePedigreeSearch: CourtCasePedigreeSearch, isReference: Boolean): Future[List[CourtCasePedigree]]
  def getProfilesNodeAssociation(courtCasePedigreeSearch: CourtCasePedigreeSearch, isReference: Boolean, profilesCod: List[String]): Future[List[ProfileNodeAssociation]]
  def getProfilesInactive(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[List[CourtCasePedigree]]
  def getTotalProfiles(courtCasePedigreeSearch: CourtCasePedigreeSearch, isReference: Boolean): Future[Long]
  def getTotalProfilesInactive(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Long]
  def getTotalProfilesOccurenceInCase(globalCode: SampleCode): Future[Int]
  def getTotalProfilesPedigreeMatches(globalCode: SampleCode): Future[Int]
  def getTotalProfilesNodeAssociation(courtCasePedigreeSearch: CourtCasePedigreeSearch, isReference: Boolean, profilesCod: List[String]): Future[Long]
  def getTotalMetadata(personDataSearch: PersonDataSearch): Future[Long]
  def addProfiles(courtCaseProfiles: List[CaseProfileAdd], isReference: Boolean): Future[Either[String, Unit]]
  def removeProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]]
  def removeMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Unit]]
  def filterProfileDatasWithFilter(input: String, idCase: Long)(filter: profiledata.ProfileData => Boolean): Future[Seq[profiledata.ProfileDataView]]
  def filterProfileDatasWithFilterPaging(input: String, idCase: Long, page: Int, pageSize: Int)(filter: profiledata.ProfileData => Boolean): Future[Seq[profiledata.ProfileDataView]]
  def addBatches(courtCaseProfiles: CaseBatchAdd): Future[Either[String, Unit]]
  def filterProfileDatasWithFilterNodeAssociation(input: String, idCase: Long)(filter: profiledata.ProfileDataWithBatch => Boolean): Future[Seq[profiledata.ProfileDataView]]
  def getCourtCasePedigrees(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Seq[PedigreeMetaDataView]]
  def getTotalCourtCasePedigrees(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Int]
  def createOrUpdatePedigreeMetadata(pedigreeMetaData: PedigreeMetaData): Future[Either[String, Long]]
  def createPedigree(pedigreeDataCreation: PedigreeDataCreation, userId: String, copiedFrom: Option[Long] = None): Future[Either[String, Long]]
  def fisicalDeletePredigree(pedigreeId: Long, userId: String, isSuperUser: Boolean): Future[Either[String, Long]]
  def changeCourtCaseStatus(courtCaseId: Long, status: PedigreeStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, Long]]
  def getProfilesNodo(id: Long, codigo: String): Future[Boolean]
  def doesntHaveGenotification(pedigreeId: Long): Future[Boolean]
  def clonePedigree(pedigreeId: Long, userId: String): Future[Either[String, Long]]
  def doesntHavePedigrees(courtCaseId: Long): Future[Boolean]
  def doesntHaveActivePedigrees(courtCaseId: Long): Future[Boolean]
  def hasPendingPedigreeMatches(courtCaseId: Long): Future[Boolean]
  def hasPedigreeMatches(courtCaseId: Long): Future[Boolean]
  def closeAllPedigrees(courtCaseId: Long, userId: String): Future[Either[String, Long]]
  def countPendingCourCaseMatches(courtCaseId: Long): Future[Int]
  def getProfilesToDelete(courtCaseId: Long): Future[Seq[SampleCode]]
  def countPendingScenariosByProfile(globalCode: String): Future[Int]
  def countActivePedigreesByProfile(globalCode: String): Future[Int]
  def getTotalProfileNumberOfMatches(globalCode: SampleCode): Future[Int]
  def collapse(idCourtCase: Long, user: String): Unit
  def getProfilesForCollapsing(idCourtCase: Long): Future[List[(String, Boolean)]]
  def disassociateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]]
  def collapseGroup(collapseRequest: matching.CollapseRequest): Future[Either[String, Unit]]
  def areAssignedToPedigree(globalCodes: List[String], courtCaseId: Long): Future[Either[String, Unit]]
  def getPedigreeCoincidencia(id: Long): Future[PedigreeMatchCard]
  def profileNumberOfPendingMatches(globalCode: String): Future[Int]
  def countProfilesHitPedigrees(globalCodes: String): Future[Int]
  def countProfilesDiscardedPedigrees(globalCodes: String): Future[Int]
  def getMatchByProfile(globalCode: String): Future[profiledata.ProfileDataViewCoincidencia]
  def getPedigreeByCourtCase(courtCaseId: Long): Future[List[PedigreeGenogram]]

@jakarta.inject.Singleton
class PedigreeServiceStub extends PedigreeService:
  override def getAllCourtCases(pedigreeSearch: PedigreeSearch): Future[Seq[CourtCaseModelView]] = Future.successful(Seq.empty)
  override def getTotalCourtCases(pedigreeSearch: PedigreeSearch): Future[Int] = Future.successful(0)
  override def createCourtCase(courtCase: CourtCaseAttempt): Future[Either[String, Long]] = Future.successful(Right(0L))
  override def createMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Long]] = Future.successful(Right(0L))
  override def addGenogram(genogram: PedigreeGenogram): Future[Either[String, Long]] = Future.successful(Right(0L))
  override def getCourtCase(courtCaseId: Long, userId: String, isSuperUser: Boolean): Future[Option[CourtCaseFull]] = Future.successful(None)
  override def getMetadata(personDataSearch: PersonDataSearch): Future[List[PersonData]] = Future.successful(List.empty)
  override def getPedigree(pedigreeId: Long): Future[Option[PedigreeDataCreation]] = Future.successful(None)
  override def updateCourtCase(id: Long, courtCase: CourtCaseAttempt, isSuperUser: Boolean): Future[Either[String, Long]] = Future.successful(Right(id))
  override def updateMetadata(idCourtCase: Long, assignee: String, personData: PersonData, isSuperUser: Boolean): Future[Either[String, Long]] = Future.successful(Right(idCourtCase))
  override def changePedigreeStatus(pedigreeId: Long, status: PedigreeStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, Long]] = Future.successful(Right(pedigreeId))
  override def getCaseTypes(): Future[Seq[CaseType]] = Future.successful(Seq.empty)
  override def getProfiles(courtCasePedigreeSearch: CourtCasePedigreeSearch, isReference: Boolean): Future[List[CourtCasePedigree]] = Future.successful(List.empty)
  override def getProfilesNodeAssociation(courtCasePedigreeSearch: CourtCasePedigreeSearch, isReference: Boolean, profilesCod: List[String]): Future[List[ProfileNodeAssociation]] = Future.successful(List.empty)
  override def getProfilesInactive(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[List[CourtCasePedigree]] = Future.successful(List.empty)
  override def getTotalProfiles(courtCasePedigreeSearch: CourtCasePedigreeSearch, isReference: Boolean): Future[Long] = Future.successful(0L)
  override def getTotalProfilesInactive(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Long] = Future.successful(0L)
  override def getTotalProfilesOccurenceInCase(globalCode: SampleCode): Future[Int] = Future.successful(0)
  override def getTotalProfilesPedigreeMatches(globalCode: SampleCode): Future[Int] = Future.successful(0)
  override def getTotalProfilesNodeAssociation(courtCasePedigreeSearch: CourtCasePedigreeSearch, isReference: Boolean, profilesCod: List[String]): Future[Long] = Future.successful(0L)
  override def getTotalMetadata(personDataSearch: PersonDataSearch): Future[Long] = Future.successful(0L)
  override def addProfiles(courtCaseProfiles: List[CaseProfileAdd], isReference: Boolean): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def removeProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def removeMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def filterProfileDatasWithFilter(input: String, idCase: Long)(filter: profiledata.ProfileData => Boolean): Future[Seq[profiledata.ProfileDataView]] = Future.successful(Seq.empty)
  override def filterProfileDatasWithFilterPaging(input: String, idCase: Long, page: Int, pageSize: Int)(filter: profiledata.ProfileData => Boolean): Future[Seq[profiledata.ProfileDataView]] = Future.successful(Seq.empty)
  override def addBatches(courtCaseProfiles: CaseBatchAdd): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def filterProfileDatasWithFilterNodeAssociation(input: String, idCase: Long)(filter: profiledata.ProfileDataWithBatch => Boolean): Future[Seq[profiledata.ProfileDataView]] = Future.successful(Seq.empty)
  override def getCourtCasePedigrees(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Seq[PedigreeMetaDataView]] = Future.successful(Seq.empty)
  override def getTotalCourtCasePedigrees(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Int] = Future.successful(0)
  override def createOrUpdatePedigreeMetadata(pedigreeMetaData: PedigreeMetaData): Future[Either[String, Long]] = Future.successful(Right(0L))
  override def createPedigree(pedigreeDataCreation: PedigreeDataCreation, userId: String, copiedFrom: Option[Long] = None): Future[Either[String, Long]] = Future.successful(Right(0L))
  override def fisicalDeletePredigree(pedigreeId: Long, userId: String, isSuperUser: Boolean): Future[Either[String, Long]] = Future.successful(Right(pedigreeId))
  override def changeCourtCaseStatus(courtCaseId: Long, status: PedigreeStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, Long]] = Future.successful(Right(courtCaseId))
  override def getProfilesNodo(id: Long, codigo: String): Future[Boolean] = Future.successful(false)
  override def doesntHaveGenotification(pedigreeId: Long): Future[Boolean] = Future.successful(true)
  override def clonePedigree(pedigreeId: Long, userId: String): Future[Either[String, Long]] = Future.successful(Right(pedigreeId))
  override def doesntHavePedigrees(courtCaseId: Long): Future[Boolean] = Future.successful(true)
  override def doesntHaveActivePedigrees(courtCaseId: Long): Future[Boolean] = Future.successful(true)
  override def hasPendingPedigreeMatches(courtCaseId: Long): Future[Boolean] = Future.successful(false)
  override def hasPedigreeMatches(courtCaseId: Long): Future[Boolean] = Future.successful(false)
  override def closeAllPedigrees(courtCaseId: Long, userId: String): Future[Either[String, Long]] = Future.successful(Right(courtCaseId))
  override def countPendingCourCaseMatches(courtCaseId: Long): Future[Int] = Future.successful(0)
  override def getProfilesToDelete(courtCaseId: Long): Future[Seq[SampleCode]] = Future.successful(Seq.empty)
  override def countPendingScenariosByProfile(globalCode: String): Future[Int] = Future.successful(0)
  override def countActivePedigreesByProfile(globalCode: String): Future[Int] = Future.successful(0)
  override def getTotalProfileNumberOfMatches(globalCode: SampleCode): Future[Int] = Future.successful(0)
  override def collapse(idCourtCase: Long, user: String): Unit = ()
  override def getProfilesForCollapsing(idCourtCase: Long): Future[List[(String, Boolean)]] = Future.successful(List.empty)
  override def disassociateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def collapseGroup(collapseRequest: matching.CollapseRequest): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def areAssignedToPedigree(globalCodes: List[String], courtCaseId: Long): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def getPedigreeCoincidencia(id: Long): Future[PedigreeMatchCard] = Future.failed(new NotImplementedError("stub"))
  override def profileNumberOfPendingMatches(globalCode: String): Future[Int] = Future.successful(0)
  override def countProfilesHitPedigrees(globalCodes: String): Future[Int] = Future.successful(0)
  override def countProfilesDiscardedPedigrees(globalCodes: String): Future[Int] = Future.successful(0)
  override def getMatchByProfile(globalCode: String): Future[profiledata.ProfileDataViewCoincidencia] = Future.failed(new NotImplementedError("stub"))
  override def getPedigreeByCourtCase(courtCaseId: Long): Future[List[PedigreeGenogram]] = Future.successful(List.empty)

case class PedigreeSearch(
  page: Int,
  pageSize: Int,
  input: String,
  isOwnCases: Boolean,
  idCourtCase: Option[Long],
  profile: Option[String],
  status: Option[PedigreeStatus.Value] = None,
  sortField: Option[String] = None,
  ascending: Option[Boolean] = None,
  caseType: Option[String] = None,
  user: String = ""
)

object PedigreeSearch:
  import play.api.libs.functional.syntax.*
  import play.api.libs.json.*

  // Reads acepta el formato nuevo {input, isOwnCases, idCourtCase, ...}
  // y el legacy {code, isSuperUser, ...} para compat de frontend.
  // - `code` (legacy, Option[String]) → `input` (default "")
  // - `isSuperUser` (legacy) → `isOwnCases = !isSuperUser`
  //   (el controller siempre re-deriva con userService, así que esto es defensivo)
  implicit val reads: Reads[PedigreeSearch] = (
    (JsPath \ "page").read[Int] and
    (JsPath \ "pageSize").read[Int] and
    ((JsPath \ "input").readNullable[String]
      orElse (JsPath \ "code").readNullable[String])
      .map(_.getOrElse("")) and
    ((JsPath \ "isOwnCases").readNullable[Boolean]
      orElse (JsPath \ "isSuperUser").readNullable[Boolean].map(_.map(!_)))
      .map(_.getOrElse(false)) and
    (JsPath \ "idCourtCase").readNullable[Long] and
    (JsPath \ "profile").readNullable[String] and
    (JsPath \ "status").readNullable[PedigreeStatus.Value] and
    (JsPath \ "sortField").readNullable[String] and
    (JsPath \ "ascending").readNullable[Boolean] and
    (JsPath \ "caseType").readNullable[String] and
    (JsPath \ "user").readNullable[String].map(_.getOrElse(""))
  )(PedigreeSearch.apply _)

  implicit val writes: OWrites[PedigreeSearch] = Json.writes[PedigreeSearch]

case class CourtCaseSummary(
  id: Long,
  internalSampleCode: String,
  status: PedigreeStatus.Value
)

object CourtCaseSummary:
  implicit val format: play.api.libs.json.OFormat[CourtCaseSummary] = Json.format[CourtCaseSummary]

// ---------------------------------------------------------------------------
// PedigreeServiceImpl
// ---------------------------------------------------------------------------

@jakarta.inject.Singleton
class PedigreeServiceImpl @jakarta.inject.Inject() (
  pedigreeDataRepository: PedigreeDataRepository,
  pedigreeRepository: PedigreeRepository,
  cache: services.CacheService,
  profileServiceProvider: Provider[profile.ProfileService],
  fullTextSearch: search.FullTextSearchService,
  categoryService: configdata.CategoryService,
  pedigreeGenotificationRepository: PedigreeGenotypificationRepository,
  pedigreeMatchesRepository: PedigreeMatchesRepository,
  pedigreeScenarioRepository: PedigreeScenarioRepository,
  matchingService: matching.MatchingService,
  pedCheckService: PedCheckService,
  traceService: trace.TraceService
)(using ec: ExecutionContext) extends PedigreeService:

  private def profileService: profile.ProfileService = profileServiceProvider.get()

  private val logger: Logger = Logger(this.getClass)

  private val errorPf: PartialFunction[Throwable, Either[String, Long]] = {
    case ex: org.postgresql.util.PSQLException =>
      ex.getSQLState match
        case "23505" => Left("error.E0901")
        case _       => Left("error.E0630")
  }

  private def getPedigreesFromProfile(pedigreeSearch: PedigreeSearch): Future[Option[Seq[Long]]] =
    if pedigreeSearch.profile.isDefined then
      pedigreeRepository.findByProfile(pedigreeSearch.profile.get).map(ids => Some(ids))
    else
      Future.successful(None)

  private def fillPendingMatches(list: Seq[CourtCaseModelView]): Future[Seq[CourtCaseModelView]] =
    Future.sequence(list.map(cc => countPendingCourCaseMatches(cc.id).map(n => cc.copy(numberOfPendingMatches = n))))

  override def getAllCourtCases(pedigreeSearch: PedigreeSearch): Future[Seq[CourtCaseModelView]] =
    getPedigreesFromProfile(pedigreeSearch).flatMap {
      case Some(ids) =>
        if ids.isEmpty then Future.successful(Seq.empty)
        else pedigreeDataRepository.getAllCourtCases(Some(ids), pedigreeSearch).flatMap(fillPendingMatches)
      case None =>
        pedigreeDataRepository.getAllCourtCases(None, pedigreeSearch).flatMap(fillPendingMatches)
    }

  override def getTotalCourtCases(pedigreeSearch: PedigreeSearch): Future[Int] =
    getPedigreesFromProfile(pedigreeSearch).flatMap {
      case Some(ids) =>
        if ids.isEmpty then Future.successful(0)
        else pedigreeDataRepository.getTotalCourtCases(Some(ids), pedigreeSearch)
      case None =>
        pedigreeDataRepository.getTotalCourtCases(None, pedigreeSearch)
    }

  override def createCourtCase(courtCase: CourtCaseAttempt): Future[Either[String, Long]] =
    pedigreeDataRepository.createCourtCase(courtCase).recover(errorPf)

  override def createMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Long]] =
    pedigreeDataRepository.createMetadata(idCourtCase, personData).recover(errorPf)

  override def addGenogram(genogram: PedigreeGenogram): Future[Either[String, Long]] =
    val result = pedigreeRepository.addGenogram(genogram).recover(errorPf)
    result.onComplete(_ => pedCheckService.cleanConsistency(genogram._id))
    result

  override def getPedigree(pedigreeId: Long): Future[Option[PedigreeDataCreation]] =
    if pedigreeId == 0 then Future.successful(None)
    else
      for
        dataCreation <- pedigreeDataRepository.getPedigreeMetaData(pedigreeId)
        genogram     <- pedigreeRepository.get(pedigreeId)
      yield dataCreation.map(pd => PedigreeDataCreation(pd.pedigreeMetaData, genogram))

  override def getCourtCase(courtCaseId: Long, userId: String, isSuperUser: Boolean): Future[Option[CourtCaseFull]] =
    pedigreeDataRepository.getCourtCase(courtCaseId).map(_.flatMap { cc =>
      if cc.assignee == userId || isSuperUser then
        Some(CourtCaseFull(cc.id, cc.internalSampleCode, cc.attorney, cc.court, cc.assignee,
          cc.crimeInvolved, cc.crimeType, cc.criminalCase, cc.status, cc.personData, cc.caseType))
      else None
    })

  override def getMetadata(personDataSearch: PersonDataSearch): Future[List[PersonData]] =
    pedigreeDataRepository.getMetadata(personDataSearch)

  override def updateCourtCase(id: Long, courtCase: CourtCaseAttempt, isSuperUser: Boolean): Future[Either[String, Long]] =
    getCourtCase(id, courtCase.assignee, isSuperUser).flatMap {
      case None => Future.successful(Left(s"error.E0643|${courtCase.assignee}"))
      case Some(_) =>
        pedigreeDataRepository.updateCourtCase(id, courtCase).map(_ => Right(id)).recover(errorPf)
    }

  override def updateMetadata(idCourtCase: Long, assignee: String, personData: PersonData, isSuperUser: Boolean): Future[Either[String, Long]] =
    getCourtCase(idCourtCase, assignee, isSuperUser).flatMap {
      case None => Future.successful(Left(s"error.E0643|$assignee"))
      case Some(_) =>
        pedigreeDataRepository.updateMetadata(idCourtCase, personData).map(_ => Right(idCourtCase)).recover(errorPf)
    }

  private def validTransition(orig: PedigreeStatus.Value, next: PedigreeStatus.Value): Boolean =
    (orig, next) match
      case (PedigreeStatus.UnderConstruction, PedigreeStatus.Active)   => true
      case (PedigreeStatus.UnderConstruction, PedigreeStatus.Deleted)  => true
      case (PedigreeStatus.UnderConstruction, PedigreeStatus.Closed)   => true
      case (PedigreeStatus.Active, _)                                  => true
      case (PedigreeStatus.Validated, PedigreeStatus.Validated)        => true
      case _                                                           => false

  private def validCourtCaseTransition(orig: PedigreeStatus.Value, next: PedigreeStatus.Value): Boolean =
    (orig, next) match
      case (PedigreeStatus.Open, PedigreeStatus.Deleted) => true
      case (PedigreeStatus.Open, PedigreeStatus.Closed)  => true
      case _                                             => false

  override def changePedigreeStatus(pedigreeId: Long, status: PedigreeStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, Long]] =
    pedigreeDataRepository.getPedigreeMetaData(pedigreeId).flatMap { pedigreeOpt =>
      val pedigree = pedigreeOpt.get
      if pedigree.pedigreeMetaData.assignee == userId || isSuperUser then
        if validTransition(pedigree.pedigreeMetaData.status, status) then
          pedigreeRepository.changeStatus(pedigreeId, status).flatMap {
            case Right(id) =>
              pedigreeDataRepository.changePedigreeStatus(pedigreeId, status).flatMap {
                case Right(cc) =>
                  traceService.addTracePedigree(trace.TracePedigree(pedigreeId, userId, new Date(),
                    trace.PedigreeStatusChangeInfo(status.toString))).map(_ => Right(cc))
                case Left(error) =>
                  pedigreeRepository.changeStatus(pedigreeId, pedigree.pedigreeMetaData.status).map {
                    case Right(_) => Left(error)
                    case Left(revertErr) =>
                      logger.error(s"changePedigreeStatus rollback failed for pedigree=$pedigreeId — postgres error: '$error', mongo revert error: '$revertErr'. Forensic data may be inconsistent.")
                      Left(error)
                  }
              }
            case Left(error) => Future.successful(Left(error))
          }
        else
          if pedigree.pedigreeMetaData.status != PedigreeStatus.Validated then
            Future.successful(Left(s"error.E0930|${pedigree.pedigreeMetaData.status}|$status"))
          else
            Future.successful(Right(pedigreeId))
      else
        Future.successful(Left(s"error.E0644|$userId"))
    }

  override def getCaseTypes(): Future[Seq[CaseType]] =
    cache.asyncGetOrElse(services.CaseTypesKey)(pedigreeDataRepository.getCaseTypes())

  private def getGenotipification(list: List[CourtCasePedigree]): Future[List[CourtCasePedigree]] =
    profileService.findByCodes(list.map(x => SampleCode(x.globalCode))).map { profiles =>
      list.flatMap { element =>
        profiles.find(p => p.globalCode.text == element.globalCode)
          .map(x => element.copy(genotypification = x.genotypification))
      }
    }

  override def getProfiles(courtCasePedigreeSearch: CourtCasePedigreeSearch, isReference: Boolean): Future[List[CourtCasePedigree]] =
    val typeProfile = if isReference then "Referencia" else "Resto"
    val fut = courtCasePedigreeSearch.input match
      case None =>
        pedigreeDataRepository.getProfiles(courtCasePedigreeSearch, Nil, Some(typeProfile)).flatMap(getGenotipification)
      case Some(input) =>
        categoryService.listCategories.flatMap { categories =>
          filterProfileDatasWithFilter(input, courtCasePedigreeSearch.idCourtCase) { p =>
            categories.get(p.category).exists(c => c.pedigreeAssociation && c.isReference == isReference)
          }.flatMap { list =>
            if list.isEmpty then Future.successful(Nil)
            else pedigreeDataRepository.getProfiles(courtCasePedigreeSearch, list.toList.map(_.globalCode.text), Some(typeProfile)).flatMap(getGenotipification)
          }
        }
    fut.map(result => result.groupBy(_.internalCode).values.map(_.sortBy(_.idBatch).reverse.head).toList)

  override def getProfilesNodeAssociation(courtCasePedigreeSearch: CourtCasePedigreeSearch, isReference: Boolean, profilesCod: List[String]): Future[List[ProfileNodeAssociation]] =
    val typeProfile = if isReference then "Referencia" else "Resto"
    courtCasePedigreeSearch.input match
      case None =>
        if !isReference then
          pedigreeDataRepository.getProfilesNodeAssociation(courtCasePedigreeSearch, Nil, Some(typeProfile), profilesCod)
        else
          pedigreeDataRepository.getProfilesNodeAssociation(courtCasePedigreeSearch, Nil, None, profilesCod)
      case Some(input) =>
        categoryService.listCategories.flatMap { categories =>
          val filterFn: profiledata.ProfileData => Boolean = p =>
            if courtCasePedigreeSearch.idCourtCase != 0 && !isReference then
              categories.get(p.category).exists(c => c.pedigreeAssociation && c.isReference == isReference)
            else if isReference then
              categories.get(p.category).exists(_.pedigreeAssociation)
            else
              categories.get(p.category).exists(c => c.pedigreeAssociation && p.category.text != "IR" && c.tipo.getOrElse(1) == 2)
          for
            profiledatas  <- filterProfileDatasWithFilter(input, courtCasePedigreeSearch.idCourtCase)(filterFn)
            profiledataLabelFn = (pw: profiledata.ProfileDataWithBatch) =>
              if courtCasePedigreeSearch.idCourtCase != 0 && !isReference then
                categories.get(pw.category).exists(c => c.pedigreeAssociation && c.isReference == isReference)
              else if isReference then
                categories.get(pw.category).exists(_.pedigreeAssociation)
              else
                categories.get(pw.category).exists(c => c.pedigreeAssociation && pw.category.text != "IR" && c.tipo.getOrElse(1) == 2)
            profiledataLabel <- filterProfileDatasWithFilterNodeAssociation(input, courtCasePedigreeSearch.idCourtCase)(profiledataLabelFn)
            profileSearch = (profiledatas.toSet ++ profiledataLabel.toSet).filterNot(x => profilesCod.contains(x.globalCode.text))
            result <-
              if profileSearch.isEmpty then Future.successful(Nil)
              else if !isReference then
                pedigreeDataRepository.getProfilesNodeAssociation(courtCasePedigreeSearch, profileSearch.toList.map(_.globalCode.text), Some(typeProfile), Nil)
              else
                pedigreeDataRepository.getProfilesNodeAssociation(courtCasePedigreeSearch, profileSearch.toList.map(_.globalCode.text), None, Nil)
          yield result
        }

  override def getProfilesInactive(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[List[CourtCasePedigree]] =
    val fut = courtCasePedigreeSearch.input match
      case None =>
        pedigreeDataRepository.getProfilesInactive(courtCasePedigreeSearch, Nil).flatMap(getGenotipification)
      case Some(input) =>
        categoryService.listCategories.flatMap { categories =>
          filterProfileDatasWithFilter(input, courtCasePedigreeSearch.idCourtCase) { p =>
            categories.get(p.category).exists(c => c.pedigreeAssociation && !c.isReference)
          }.flatMap { list =>
            if list.isEmpty then Future.successful(Nil)
            else pedigreeDataRepository.getProfilesInactive(courtCasePedigreeSearch, list.toList.map(_.globalCode.text)).flatMap(getGenotipification)
          }
        }
    fut.map { list =>
      if courtCasePedigreeSearch.groupedBy.isDefined then
        list.filter(_.globalCode != courtCasePedigreeSearch.groupedBy.get)
      else list
    }

  override def getTotalProfiles(courtCasePedigreeSearch: CourtCasePedigreeSearch, isReference: Boolean): Future[Long] =
    val typeProfile = if isReference then "Referencia" else "Resto"
    courtCasePedigreeSearch.input match
      case None =>
        pedigreeDataRepository.getTotalProfiles(courtCasePedigreeSearch, Nil, Some(typeProfile)).map(_.toLong)
      case Some(input) =>
        categoryService.listCategories.flatMap { categories =>
          filterProfileDatasWithFilter(input, courtCasePedigreeSearch.idCourtCase) { p =>
            categories.get(p.category).exists(c => c.pedigreeAssociation && c.isReference == isReference)
          }.flatMap { list =>
            if list.isEmpty then Future.successful(0L)
            else pedigreeDataRepository.getTotalProfiles(courtCasePedigreeSearch, list.toList.map(_.globalCode.text), Some(typeProfile)).map(_.toLong)
          }
        }

  override def getTotalProfilesInactive(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Long] =
    courtCasePedigreeSearch.input match
      case None =>
        pedigreeDataRepository.getTotalProfilesInactive(courtCasePedigreeSearch, Nil).map(_.toLong)
      case Some(input) =>
        categoryService.listCategories.flatMap { categories =>
          filterProfileDatasWithFilter(input, courtCasePedigreeSearch.idCourtCase) { p =>
            categories.get(p.category).exists(c => c.pedigreeAssociation && !c.isReference)
          }.flatMap { list =>
            if list.isEmpty then Future.successful(0L)
            else pedigreeDataRepository.getTotalProfilesInactive(courtCasePedigreeSearch, list.toList.map(_.globalCode.text)).map(_.toLong)
          }
        }

  override def getTotalProfilesPedigreeMatches(globalCode: SampleCode): Future[Int] =
    pedigreeRepository.findByProfile(globalCode.text).flatMap { pedigreesIds =>
      Future.sequence(pedigreesIds.map(id => pedigreeMatchesRepository.numberOfPendingMatches(id))).map(_.sum)
    }

  override def getTotalProfileNumberOfMatches(globalCode: SampleCode): Future[Int] =
    pedigreeMatchesRepository.profileNumberOfPendingMatches(globalCode.text)

  override def getTotalProfilesOccurenceInCase(globalCode: SampleCode): Future[Int] =
    pedigreeDataRepository.getTotalProfilesOccurenceInCase(globalCode.text)

  override def getTotalProfilesNodeAssociation(courtCasePedigreeSearch: CourtCasePedigreeSearch, isReference: Boolean, profilesCod: List[String]): Future[Long] =
    val typeProfile = if isReference then "Referencia" else "Resto"
    courtCasePedigreeSearch.input match
      case None =>
        if isReference then
          pedigreeDataRepository.getTotalProfilesNodeAssociation(courtCasePedigreeSearch, Nil, None, profilesCod).map(_.toLong)
        else
          pedigreeDataRepository.getTotalProfilesNodeAssociation(courtCasePedigreeSearch, Nil, Some(typeProfile), profilesCod).map(_.toLong)
      case Some(input) =>
        categoryService.listCategories.flatMap { categories =>
          val filterFn: profiledata.ProfileData => Boolean = p =>
            if courtCasePedigreeSearch.idCourtCase != 0 && !isReference then
              categories.get(p.category).exists(c => c.pedigreeAssociation && c.isReference == isReference)
            else if isReference then categories.get(p.category).exists(_.pedigreeAssociation)
            else categories.get(p.category).exists(c => c.pedigreeAssociation && p.category.text != "IR")
          for
            profiledatas <- filterProfileDatasWithFilter(input, courtCasePedigreeSearch.idCourtCase)(filterFn)
            profiledataLabel <- filterProfileDatasWithFilterNodeAssociation(input, courtCasePedigreeSearch.idCourtCase) { pw =>
              if courtCasePedigreeSearch.idCourtCase != 0 && !isReference then
                categories.get(pw.category).exists(c => c.pedigreeAssociation && c.isReference == isReference)
              else if isReference then categories.get(pw.category).exists(_.pedigreeAssociation)
              else categories.get(pw.category).exists(c => c.pedigreeAssociation && pw.category.text != "IR")
            }
            profileSearch = (profiledatas.toSet ++ profiledataLabel.toSet).filterNot(x => profilesCod.contains(x.globalCode.text))
            result <-
              if profileSearch.isEmpty then Future.successful(0L)
              else if isReference then
                pedigreeDataRepository.getTotalProfilesNodeAssociation(courtCasePedigreeSearch, profileSearch.toList.map(_.globalCode.text), None, Nil).map(_.toLong)
              else
                pedigreeDataRepository.getTotalProfilesNodeAssociation(courtCasePedigreeSearch, profileSearch.toList.map(_.globalCode.text), Some(typeProfile), Nil).map(_.toLong)
          yield result
        }

  override def getTotalMetadata(personDataSearch: PersonDataSearch): Future[Long] =
    pedigreeDataRepository.getTotalMetadata(personDataSearch).map(_.toLong)

  override def addProfiles(courtCaseProfiles: List[CaseProfileAdd], isReference: Boolean): Future[Either[String, Unit]] =
    val typed = courtCaseProfiles.map { cpp =>
      CaseProfileAdd(cpp.courtcaseId, cpp.globalCode,
        Some(if isReference then ProfileType.Referencia.toString else ProfileType.Resto.toString))
    }
    val restos = typed.filter(p => p.profileType.contains(ProfileType.Resto.toString))
    if restos.nonEmpty then
      doesntHaveActivePedigrees(courtCaseProfiles.head.courtcaseId).flatMap {
        case false => Future.successful(Left("error.E0211"))
        case true  => pedigreeDataRepository.addProfiles(typed)
      }
    else
      pedigreeDataRepository.addProfiles(typed)

  override def removeProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] =
    pedigreeDataRepository.removeProfiles(courtCaseProfiles)

  override def removeMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Unit]] =
    pedigreeDataRepository.removeMetadata(idCourtCase, personData)

  override def filterProfileDatasWithFilter(input: String, idCase: Long)(filter: profiledata.ProfileData => Boolean): Future[Seq[profiledata.ProfileDataView]] =
    fullTextSearch.searchProfileDatasWithFilter(input)(filter).flatMap { list =>
      pedigreeDataRepository.getProfiles(CourtCasePedigreeSearch(0, Int.MaxValue, idCase, None), list.toList.map(_.globalCode.text), None).map { assoc =>
        list.map { pd =>
          profiledata.ProfileDataView(pd.globalCode, pd.category, pd.internalSampleCode, pd.assignee,
            assoc.exists(_.globalCode == pd.globalCode.text))
        }.sortBy(r => (r.associated.toString, r.globalCode.text))
      }
    }

  override def filterProfileDatasWithFilterPaging(input: String, idCase: Long, page: Int, pageSize: Int)(filter: profiledata.ProfileData => Boolean): Future[Seq[profiledata.ProfileDataView]] =
    fullTextSearch.searchProfileDatasWithFilterPaging(input, page, pageSize)(filter).flatMap { list =>
      pedigreeDataRepository.getProfiles(CourtCasePedigreeSearch(0, Int.MaxValue, idCase, None), list.toList.map(_.globalCode.text), None).map { assoc =>
        list.map { pd =>
          profiledata.ProfileDataView(pd.globalCode, pd.category, pd.internalSampleCode, pd.assignee,
            assoc.exists(_.globalCode == pd.globalCode.text))
        }.sortBy(r => (r.associated.toString, r.globalCode.text))
      }
    }

  override def filterProfileDatasWithFilterNodeAssociation(input: String, idCase: Long)(filter: profiledata.ProfileDataWithBatch => Boolean): Future[Seq[profiledata.ProfileDataView]] =
    fullTextSearch.searchProfileDatasWithFilterNodeAssociation(input)(filter).flatMap { list =>
      pedigreeDataRepository.getProfiles(CourtCasePedigreeSearch(0, Int.MaxValue, idCase, None), list.toList.map(_.globalCode.text), None).map { assoc =>
        list.map { pd =>
          profiledata.ProfileDataView(pd.globalCode, pd.category, pd.internalSampleCode, pd.assignee,
            assoc.exists(_.globalCode == pd.globalCode.text))
        }.sortBy(r => (r.associated.toString, r.globalCode.text))
      }
    }

  override def addBatches(courtCaseProfiles: CaseBatchAdd): Future[Either[String, Unit]] =
    pedigreeDataRepository.getProfilesFromBatches(courtCaseProfiles.courtcaseId, courtCaseProfiles.batches, courtCaseProfiles.tipo).flatMap {
      case Nil => Future.successful(Left("error.E0203"))
      case globalCodes =>
        pedigreeDataRepository.getCourtCase(courtCaseProfiles.courtcaseId).flatMap {
          case None => Future.successful(Left("error.E0203"))
          case Some(courtCase) =>
            val profilesFut: Future[List[CaseProfileAdd]] =
              if courtCase.caseType == "DVI" then
                val resto = globalCodes.map {
                  case (globCode, true)  => CaseProfileAdd(courtCaseProfiles.courtcaseId, globCode, Some(ProfileType.Referencia.toString))
                  case (globCode, false) => CaseProfileAdd(courtCaseProfiles.courtcaseId, globCode, Some(ProfileType.Resto.toString))
                }
                val restos = resto.filter(_.profileType.contains(ProfileType.Resto.toString))
                if restos.nonEmpty then
                  doesntHaveActivePedigrees(courtCaseProfiles.courtcaseId).map { noActive =>
                    if noActive then resto else resto.filter(_.profileType.contains(ProfileType.Referencia.toString))
                  }
                else Future.successful(resto)
              else
                Future.successful(
                  globalCodes.filter(_._2).map { case (globCode, _) =>
                    CaseProfileAdd(courtCaseProfiles.courtcaseId, globCode, Some(ProfileType.Referencia.toString))
                  }
                )
            profilesFut.flatMap(pedigreeDataRepository.addProfiles)
        }
    }

  override def getCourtCasePedigrees(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Seq[PedigreeMetaDataView]] =
    pedigreeDataRepository.getPedigrees(courtCasePedigreeSearch)

  override def getTotalCourtCasePedigrees(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Int] =
    pedigreeDataRepository.getTotalPedigrees(courtCasePedigreeSearch)

  override def createOrUpdatePedigreeMetadata(pedigreeMetaData: PedigreeMetaData): Future[Either[String, Long]] =
    pedigreeDataRepository.createOrUpdatePedigreeMetadata(pedigreeMetaData)

  override def createPedigree(pedigreeDataCreation: PedigreeDataCreation, userId: String, copiedFrom: Option[Long] = None): Future[Either[String, Long]] =
    pedigreeDataCreation.pedigreeGenogram match
      case None =>
        Future.successful(Left("error.E0201"))
      case Some(genogram) =>
        pedigreeDataRepository.createOrUpdatePedigreeMetadata(pedigreeDataCreation.pedigreeMetaData).flatMap {
          case Left(error) => Future.successful(Left(error))
          case Right(id) =>
            if pedigreeDataCreation.pedigreeMetaData.id == 0 then
              traceService.addTracePedigree(trace.TracePedigree(id, userId, new Date(),
                trace.PedigreeStatusChangeInfo(PedigreeStatus.UnderConstruction.toString)))
              copiedFrom.foreach { from =>
                traceService.addTracePedigree(trace.TracePedigree(from, userId, new Date(),
                  trace.PedigreeCopyInfo(from, pedigreeDataCreation.pedigreeMetaData.name)))
              }
            else
              traceService.addTracePedigree(trace.TracePedigree(id, userId, new Date(), trace.PedigreeEditInfo(id)))
            val pedigreeGenogram = PedigreeGenogram(id, genogram.assignee, genogram.genogram, genogram.status,
              genogram.frequencyTable, genogram.processed, genogram.boundary, genogram.executeScreeningMitochondrial,
              genogram.numberOfMismatches, genogram.caseType, genogram.mutationModelId, genogram.idCourtCase)
            addGenogram(pedigreeGenogram)
        }

  override def fisicalDeletePredigree(pedigreeId: Long, userId: String, isSuperUser: Boolean): Future[Either[String, Long]] =
    pedigreeDataRepository.getPedigreeMetaData(pedigreeId).flatMap { pedigreeOpt =>
      val pedigree = pedigreeOpt.get
      if pedigree.pedigreeMetaData.assignee == userId || isSuperUser then
        pedigreeDataRepository.deleteFisicalPedigree(pedigreeId).flatMap {
          case Right(id) =>
            pedigreeRepository.deleteFisicalPedigree(pedigreeId).map {
              case Right(cc) => Right(cc)
              case Left(error) => Left(error)
            }
          case Left(error) => Future.successful(Left(error))
        }
      else
        Future.successful(Left(s"error.E0644|$userId"))
    }

  override def changeCourtCaseStatus(courtCaseId: Long, status: PedigreeStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, Long]] =
    pedigreeDataRepository.getCourtCase(courtCaseId).flatMap { courtCaseOpt =>
      val courtCase = courtCaseOpt.get
      if courtCase.assignee == userId || isSuperUser then
        if validCourtCaseTransition(courtCase.status, status) then
          pedigreeDataRepository.changeCourCaseStatus(courtCaseId, status).map {
            case Right(cc)    => Right(cc)
            case Left(error)  => Left(error)
          }
        else
          Future.successful(Left(s"error.E0930|${courtCase.status}|$status"))
      else
        Future.successful(Left(s"error.E0644|$userId"))
    }

  override def getProfilesNodo(idCourtCase: Long, codigoGlobal: String): Future[Boolean] =
    categoryService.listCategories.flatMap { categories =>
      filterProfileDatasWithFilter(codigoGlobal, idCourtCase) { p =>
        categories.get(p.category).exists(_.pedigreeAssociation)
      }.map(b => categories.get(b.head.category).exists(_.isReference))
    }

  override def doesntHaveGenotification(pedigreeId: Long): Future[Boolean] =
    pedigreeGenotificationRepository.doesntHaveGenotification(pedigreeId)

  override def clonePedigree(pedigreeId: Long, userId: String): Future[Either[String, Long]] =
    val now = new Date()
    getPedigree(pedigreeId).flatMap {
      case None => Future.successful(Left("error.E0201"))
      case Some(ped) =>
        ped.pedigreeGenogram match
          case None => Future.successful(Left("error.E0201"))
          case Some(genogram) =>
            val newMeta = PedigreeMetaData(0, ped.pedigreeMetaData.courtCaseId, "Copia" + ped.pedigreeMetaData.name,
              assignee = ped.pedigreeMetaData.assignee, creationDate = Some(now),
              status = PedigreeStatus.UnderConstruction)
            val newGenogram = PedigreeGenogram(0, genogram.assignee, genogram.genogram, PedigreeStatus.UnderConstruction,
              genogram.frequencyTable, genogram.processed, genogram.boundary, genogram.executeScreeningMitochondrial,
              genogram.numberOfMismatches, genogram.caseType, genogram.mutationModelId, genogram.idCourtCase)
            createPedigree(PedigreeDataCreation(newMeta, Some(newGenogram)), userId, Some(pedigreeId))
    }

  override def doesntHavePedigrees(courtCaseId: Long): Future[Boolean] =
    pedigreeDataRepository.getPedigrees(CourtCasePedigreeSearch(idCourtCase = courtCaseId)).map(_.isEmpty)

  override def doesntHaveActivePedigrees(courtCaseId: Long): Future[Boolean] =
    pedigreeDataRepository.getPedigrees(CourtCasePedigreeSearch(idCourtCase = courtCaseId, status = Some(PedigreeStatus.Active))).map(_.isEmpty)

  override def hasPendingPedigreeMatches(courtCaseId: Long): Future[Boolean] =
    pedigreeDataRepository.getPedigrees(CourtCasePedigreeSearch(idCourtCase = courtCaseId))
      .flatMap(list => Future.sequence(list.map(ped => pedigreeMatchesRepository.hasPendingMatches(ped.id))))
      .map(_.exists(identity))

  override def hasPedigreeMatches(courtCaseId: Long): Future[Boolean] =
    pedigreeDataRepository.getPedigrees(CourtCasePedigreeSearch(idCourtCase = courtCaseId))
      .flatMap(list => Future.sequence(list.map(ped => pedigreeMatchesRepository.hasMatches(ped.id))))
      .map(_.exists(identity))

  override def countPendingCourCaseMatches(courtCaseId: Long): Future[Int] =
    pedigreeDataRepository.getPedigrees(CourtCasePedigreeSearch(idCourtCase = courtCaseId))
      .flatMap(list => Future.sequence(list.map(ped => pedigreeMatchesRepository.numberOfPendingMatches(ped.id))))
      .map(_.sum)

  override def closeAllPedigrees(courtCaseId: Long, userId: String): Future[Either[String, Long]] =
    pedigreeDataRepository.getPedigrees(CourtCasePedigreeSearch(idCourtCase = courtCaseId)).flatMap { pedigrees =>
      val toClose = pedigrees.filter(ped =>
        ped.status == PedigreeStatus.UnderConstruction || ped.status == PedigreeStatus.Active
      )
      Future.sequence(
        toClose.map(p => changePedigreeStatus(p.id, PedigreeStatus.Closed, userId, isSuperUser = true))
      ).map { results =>
        val errors = results.collect { case Left(err) => err }
        if errors.isEmpty then Right(courtCaseId)
        else
          logger.error(s"closeAllPedigrees: ${errors.size}/${results.size} pedigree closes failed for courtCase=$courtCaseId: ${errors.mkString(", ")}")
          Left(errors.head)
      }
    }

  override def getProfilesToDelete(courtCaseId: Long): Future[Seq[SampleCode]] =
    pedigreeDataRepository.getProfilesToDelete(courtCaseId)

  override def countPendingScenariosByProfile(globalCode: String): Future[Int] =
    pedigreeScenarioRepository.countByProfile(globalCode)

  override def countActivePedigreesByProfile(globalCode: String): Future[Int] =
    pedigreeRepository.countByProfile(globalCode)

  override def collapse(idCourtCase: Long, user: String): Unit =
    matchingService.collapse(idCourtCase, user)

  override def getProfilesForCollapsing(idCourtCase: Long): Future[List[(String, Boolean)]] =
    pedigreeDataRepository.getProfilesForCollapsing(idCourtCase)

  override def disassociateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] =
    doesntHaveActivePedigrees(courtCaseProfiles.head.courtcaseId).flatMap {
      case false => Future.successful(Left("error.E0211"))
      case true =>
        pedigreeDataRepository.disassociateGroupedProfiles(courtCaseProfiles).flatMap { result =>
          getTotalProfilesInactive(CourtCasePedigreeSearch(0, 1, courtCaseProfiles.head.courtcaseId, None, None, None, None, None, courtCaseProfiles.head.groupedBy))
            .flatMap { count =>
              if count == 0 && courtCaseProfiles.head.groupedBy.isDefined then
                pedigreeDataRepository.disassociateGroupedProfiles(
                  List(CaseProfileAdd(courtCaseProfiles.head.courtcaseId, courtCaseProfiles.head.groupedBy.get, None, None)))
              else
                Future.successful(result)
            }
        }
    }

  override def areAssignedToPedigree(globalCodes: List[String], courtCaseId: Long): Future[Either[String, Unit]] =
    pedigreeDataRepository.getPedigrees(CourtCasePedigreeSearch(0, Int.MaxValue, courtCaseId)).flatMap { pedigrees =>
      val idsPedigrees = pedigrees.map(_.id)
      Future.sequence(globalCodes.map { globalCode =>
        pedigreeRepository.countByProfileIdPedigrees(globalCode, idsPedigrees.map(_.toString))
          .map(count => if count > 0 then Left(globalCode) else Right(()))
      }).flatMap { results =>
        if results.forall(_.isRight) then
          Future.sequence(globalCodes.map { globalCode =>
            pedigreeMatchesRepository.profileNumberOfPendingMatchesInPedigrees(globalCode, idsPedigrees)
              .map(count => if count > 0 then Left(globalCode) else Right(()))
          }).map { matchResults =>
            val lefts = matchResults.collect { case Left(v) => v }
            if lefts.isEmpty then Right(())
            else if lefts.size > 1 then Left(s"error.E0209|${lefts.mkString(",")}")
            else Left(s"error.E0210|${lefts.mkString(",")}")
          }
        else
          val lefts = results.collect { case Left(v) => v }
          Future.successful(
            if lefts.size > 1 then Left(s"error.E0207|${lefts.mkString(",")}")
            else Left(s"error.E0208|${lefts.mkString(",")}")
          )
      }
    }

  override def collapseGroup(collapseRequest: matching.CollapseRequest): Future[Either[String, Unit]] =
    val courtCaseProfiles = collapseRequest.globalCodeChildren.map(child =>
      CaseProfileAdd(collapseRequest.courtCaseId, child, None, Some(collapseRequest.globalCodeParent)))
    val parent = CaseProfileAdd(collapseRequest.courtCaseId, collapseRequest.globalCodeParent, None, Some(collapseRequest.globalCodeParent))
    val all = courtCaseProfiles :+ parent
    areAssignedToPedigree(courtCaseProfiles.map(_.globalCode), collapseRequest.courtCaseId).flatMap {
      case Left(m) => Future.successful(Left(m))
      case Right(()) =>
        pedigreeDataRepository.associateGroupedProfiles(all).flatMap {
          case Left(err) => Future.successful(Left(err))
          case Right(()) =>
            Future.sequence(
              courtCaseProfiles.map(p => matchingService.discardCollapsingByLeftAndRightProfile(p.globalCode, p.courtcaseId)) :+
              matchingService.discardCollapsingByRightProfile(collapseRequest.globalCodeParent, collapseRequest.courtCaseId)
            ).map(_ => Right(())).recover { case e =>
              logger.error(s"collapseGroup: associate succeeded but discardCollapsing failed for courtCase=${collapseRequest.courtCaseId} parent=${collapseRequest.globalCodeParent} (manual cleanup of associations may be needed)", e)
              Left("error.E0630")
            }
        }
    }

  override def profileNumberOfPendingMatches(globalCode: String): Future[Int] =
    pedigreeMatchesRepository.profileNumberOfPendingMatches(globalCode)

  override def countProfilesHitPedigrees(globalCodes: String): Future[Int] =
    pedigreeMatchesRepository.countProfilesHitPedigrees(globalCodes)

  override def countProfilesDiscardedPedigrees(globalCodes: String): Future[Int] =
    pedigreeMatchesRepository.countProfilesDiscardedPedigrees(globalCodes)

  override def getPedigreeCoincidencia(id: Long): Future[PedigreeMatchCard] =
    pedigreeMatchesRepository.getMatchByPedigree(id).flatMap {
      case Some(m) =>
        m._id match
          case Left(idPedigree) =>
            pedigreeDataRepository.getPedigreeMetaData(idPedigree).flatMap {
              case Some(pedigree) =>
                for
                  dis      <- pedigreeMatchesRepository.numberOfDiscardedMatches(idPedigree)
                  pending  <- pedigreeMatchesRepository.numberOfPendingMatches(idPedigree)
                  hit      <- pedigreeMatchesRepository.numberOfHitMatches(idPedigree)
                  caseType <- pedigreeMatchesRepository.getTypeCourtCasePedigree(idPedigree)
                yield PedigreeMatchCard(pedigree.pedigreeMetaData.name, idPedigree.toString, pedigree.pedigreeMetaData.assignee,
                  m.lastMatchDate.date, dis + pending + hit, "pedigree", pedigree.pedigreeMetaData.courtCaseId.toString,
                  pedigree.pedigreeMetaData.courtCaseName, caseType, hit, pending, dis)
              case None => Future.failed(new NoSuchElementException(s"Pedigree $idPedigree not found"))
            }
          case Right(_) => Future.failed(new IllegalStateException("Expected pedigree id on left"))
      case None => Future.failed(new NoSuchElementException(s"No match found for pedigree $id"))
    }

  override def getMatchByProfile(globalCode: String): Future[profiledata.ProfileDataViewCoincidencia] =
    pedigreeMatchesRepository.getMatchByProfile(globalCode).flatMap {
      case Some(m) =>
        m._id match
          case Right(globalCodes) =>
            profileService.get(SampleCode(globalCodes)).flatMap {
              case Some(pd) =>
                categoryService.getCategory(pd.categoryId).flatMap {
                  case Some(categoria) =>
                    for
                      dis     <- countProfilesDiscardedPedigrees(globalCodes)
                      pending <- profileNumberOfPendingMatches(globalCodes)
                      hit     <- countProfilesHitPedigrees(globalCodes)
                    yield profiledata.ProfileDataViewCoincidencia(SampleCode(globalCodes), categoria.name,
                      pd.internalSampleCode, pd.assignee, m.lastMatchDate.date, hit, pending, dis)
                  case None => Future.failed(new NoSuchElementException(s"Category not found for $globalCodes"))
                }
              case None => Future.failed(new NoSuchElementException(s"Profile $globalCodes not found"))
            }
          case Left(_) => Future.failed(new IllegalStateException("Expected profile global code on right"))
      case None => Future.failed(new NoSuchElementException(s"No match found for profile $globalCode"))
    }

  override def getPedigreeByCourtCase(courtCaseId: Long): Future[List[PedigreeGenogram]] =
    pedigreeRepository.getPedigreeByCourtCaseId(courtCaseId)
