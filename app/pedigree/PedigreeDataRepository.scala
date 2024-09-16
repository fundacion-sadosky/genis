package pedigree

import java.sql.Timestamp
import java.util.Calendar
import javax.inject.{Inject, Singleton}
import models.Tables
import models.Tables.{CourtCaseFiliationDataRow, CourtCaseRow, PedigreeRow}
import play.api.{Application, Logger}
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.i18n.{Messages, MessagesApi}
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.libs.json.Json
import types.{SampleCode, Sex}
import util.{DefaultDb, Transaction}

import scala.slick.jdbc.{StaticQuery => Q}
import scala.concurrent.Future

abstract class PedigreeDataRepository extends DefaultDb with Transaction {

  def getAllCourtCases(pedigreeIds: Option[Seq[Long]], pedigreeSearch: PedigreeSearch): Future[Seq[CourtCaseModelView]]
  def getTotalCourtCases(pedigreeIds: Option[Seq[Long]], pedigreeSearch: PedigreeSearch): Future[Int]
  def createCourtCase(courtCase: CourtCaseAttempt): Future[Either[String, Long]]
  def createMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Long]]
  def getCourtCase(courtCaseId: Long): Future[Option[CourtCase]]
  def getMetadata(personDataSearch:PersonDataSearch): Future[List[PersonData]]
  def updateCourtCase(id: Long, courtCase: CourtCaseAttempt)(implicit session: Session): Either[String, Long]
  def updateMetadata(idCourtCase: Long, personData: PersonData )(implicit session: Session): Either[String, Long]
  def changeCourCaseStatus(courtCaseId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]]
  def getProfiles(courtCasePedigreeSearch:CourtCasePedigreeSearch,globalCodes:List[String] = Nil, typeProfile: Option[String]):Future[List[CourtCasePedigree]]
  def getTotalProfiles(courtCasePedigreeSearch:CourtCasePedigreeSearch,globalCodes:List[String] = Nil,typeProfile:Option[String]):Future[Int]
  def getTotalProfilesOccurenceInCase(globalCodes:String):Future[Int]
  def getTotalProfilesNodeAssociation(courtCasePedigreeSearch:CourtCasePedigreeSearch,globalCodes:List[String] = Nil,typeProfile:Option[String], profilesCod: List[String]):Future[Int]
  def getTotalMetadata(personDataSearch:PersonDataSearch):Future[Int]
  def addProfiles(courtCaseProfiles:List[CaseProfileAdd]):Future[Either[String, Unit]]
  def removeProfiles(courtCaseProfiles:List[CaseProfileAdd]):Future[Either[String, Unit]]
  def removeMetadata(idCourtCase: Long, personData:PersonData):Future[Either[String, Unit]]
  def getProfilesFromBatches(courtCase:Long,batches:List[Long], tipo:Int):Future[List[(String,Boolean)]]
  def getCaseTypes() : Future[Seq[CaseType]]
  def getProfilesNodeAssociation(courtCasePedigreeSearch:CourtCasePedigreeSearch,globalCodes:List[String] = Nil,typeProfile: Option[String], profilesCod: List[String]):Future[List[ProfileNodeAssociation]]
  def getPedigrees(courtCasePedigreeSearch:CourtCasePedigreeSearch):Future[Seq[PedigreeMetaDataView]]
  def getTotalPedigrees(courtCasePedigreeSearch:CourtCasePedigreeSearch):Future[Int]
  def getPedigreeMetaData(pedigreeId: Long): Future[Option[PedigreeDataCreation]]
  def createOrUpdatePedigreeMetadata(pedigreeMetaData: PedigreeMetaData) : Future[Either[String, Long]]
  def changePedigreeStatus(pedigreeId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]]
  def deleteFisicalPedigree(pedigreeId : Long) : Future[Either[String, Long]]
  def getProfilesToDelete(courtCaseId: Long) : Future[Seq[SampleCode]]
  def getProfilesForCollapsing(idCourtCase:Long):Future[List[(String,Boolean)]]
  def getCourtCaseOfPedigree(pedigreeId : Long) : Future[Option[CourtCase]]
  def getActiveNNProfiles(courtCaseId : Long) : Future[Seq[SampleCode]]
  def disassociateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]) : Future[Either[String, Unit]]
  def getTotalProfilesInactive(courtCasePedigreeSearch:CourtCasePedigreeSearch, globalCode: List[String]):Future[Int]
  def getProfilesInactive(courtCasePedigreeSearch:CourtCasePedigreeSearch,globalcode: List[String]):Future[List[CourtCasePedigree]]
  def associateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]]
  def changePedigreeConsistencyFlag(pedigreeId: Long, consistencyRun:Boolean = true): Future[Either[String, Long]]
  def doCleanConsistency(pedigreeId:Long): Future[Either[String, Long]]
  def getPedigreeDescriptionById(pedigreeId:Long):Future[(Option[String],Option[String])]
}

@Singleton
class SlickPedigreeDataRepository @Inject() (implicit app: Application) extends PedigreeDataRepository {
  val courtCases: TableQuery[Tables.CourtCase] = Tables.CourtCase
  val profilesData: TableQuery[Tables.ProfileData] = Tables.ProfileData
  val courtCasesFiliationData: TableQuery[Tables.CourtCaseFiliationData] = Tables.CourtCaseFiliationData
  val caseTypes: TableQuery[Tables.CaseType] = Tables.CaseType
  val protoProfile: TableQuery[Tables.ProtoProfile] = Tables.ProtoProfile
  val batchProtoProfile: TableQuery[Tables.BatchProtoProfile] = Tables.BatchProtoProfile

  val courtCasesProfile: TableQuery[Tables.CourtCaseProfiles] = Tables.CourtCaseProfiles
  val pedigrees: TableQuery[Tables.Pedigree] = Tables.Pedigree

  val category: TableQuery[Tables.Category] = Tables.Category

  val logger: Logger = Logger(this.getClass())

  val queryStringGetProfilesFromBatches = """SELECT pd."GLOBAL_CODE", cat."IS_REFERENCE"
      FROM "APP"."BATCH_PROTO_PROFILE" bpp inner join "APP"."PROTO_PROFILE" pp inner join "APP"."PROFILE_DATA" pd on pp."SAMPLE_NAME" = pd."INTERNAL_SAMPLE_CODE"
          on (pp."ID_BATCH" = bpp."ID") INNER JOIN "APP"."CATEGORY" cat on (pd."CATEGORY" = cat."ID")
      WHERE pp."STATUS" = 'Imported'
            AND cat."PEDIGREE_ASSOCIATION" = TRUE
            AND cat."TYPE" = ?
            AND bpp."ID" IN RANGO
            AND pd."DELETED" = FALSE
         AND pd."GLOBAL_CODE" NOT IN
             (SELECT ccp."GLOBAL_CODE" FROM "APP"."COURT_CASE_PROFILE" ccp WHERE ccp."ID_COURT_CASE" = ?) """

  val queryProfilesToDelete = """SELECT "GLOBAL_CODE" FROM "APP"."COURT_CASE_PROFILE"
                                WHERE  "GLOBAL_CODE" IN (
                                  SELECT "GLOBAL_CODE" FROM (
                                                              SELECT "GLOBAL_CODE",COUNT("CC"."ID") AS "CANT"
                                                              FROM "APP"."COURT_CASE_PROFILE"
                                                                LEFT JOIN ( SELECT "ID" FROM "APP"."COURT_CASE"
                                                                WHERE "APP"."COURT_CASE"."STATUS" = 'Open') "CC"
                                                                  ON "APP"."COURT_CASE_PROFILE"."ID_COURT_CASE" = "CC"."ID"
                                                              GROUP BY "GLOBAL_CODE"
                                                            ) "COURT_CASE_PROFILE_GROUPED"
                                WHERE "CANT" = 0
                                ) AND "ID_COURT_CASE" = ?"""
  private def queryDefineUpdateCourtCase(id: Column[Long]) = for (
    cc <- courtCases if cc.id === id
  ) yield (cc.attorney, cc.court, cc.crimeInvolved, cc.crimeType, cc.criminalCase)

  val queryUpdateCourtCase = Compiled(queryDefineUpdateCourtCase _)

  private def queryDefineUpdateCourtCaseFiliationData(id: Column[Long] , alias: Column[String]) = for (
    ccfd <- courtCasesFiliationData if ccfd.courtCaseId === id && ccfd.alias === alias
  ) yield (ccfd.clothing,ccfd.dateOfBirth,ccfd.dateOfBirthFrom,ccfd.dateOfBirthTo,ccfd.dateOfMissing, ccfd.firstname, ccfd.haircolor, ccfd.height, ccfd.identification, ccfd.lastname,
    ccfd.nationality, ccfd.sex, ccfd.skincolor, ccfd.weight , ccfd.particularities)

  val queryUpdateCourtCaseFilationData = Compiled(queryDefineUpdateCourtCaseFiliationData _)

  private def queryDefineFiliationData(id: Column[Long] , alias: String) = for (
    ccfd <- courtCasesFiliationData
    if ccfd.courtCaseId === id && ((ccfd.alias like alias) || (ccfd.firstname like alias) || (ccfd.lastname like alias))
  ) yield (ccfd.clothing,ccfd.dateOfBirth,ccfd.dateOfBirthFrom,ccfd.dateOfBirthTo,ccfd.dateOfMissing,
    ccfd.firstname, ccfd.haircolor, ccfd.height, ccfd.identification, ccfd.lastname,
    ccfd.nationality, ccfd.sex, ccfd.skincolor, ccfd.weight , ccfd.alias ,ccfd.particularities)

  private def queryDefineGetAllCourtCases(assignee: Column[String]) = for (
    cc <- courtCases if cc.assignee === assignee
  ) yield cc

  val queryGetAllCourtCases = Compiled(queryDefineGetAllCourtCases _)

  private def queryDefineGetCourtCase(id: Column[Long]) = for (
    cc <- courtCases if cc.id === id
    ) yield (cc)
/*
*  private def queryDefineGetCourtCase(id: Column[Long]) = for (
    (cc, ccfd) <- courtCases leftJoin courtCasesFiliationData
    on (_.id === _.courtCaseId) if cc.id === id
    ) yield (cc, ccfd.?)
    */

  private def queryDefineGetMetadataCourtCase(id: Column[Long]) = for (
    (ccfd) <- courtCasesFiliationData if ccfd.courtCaseId === id
  ) yield (ccfd)

  val queryGetMetadataCourtCase = Compiled(queryDefineGetMetadataCourtCase _)

  val queryGetCourtCase = Compiled(queryDefineGetCourtCase _)

  private def queryDefineGetCourtCaseStatus(id: Column[Long]) = for (
    cc <- courtCases if cc.id === id
  ) yield (cc.id, cc.status)

  private def queryDefineGetGroupedBy(id: Column[Long],globalCode:Column[String]) = for (
    cc <- courtCasesProfile if (cc.idCourtCase === id && cc.groupedBy === globalCode)
  ) yield (cc.groupedBy)
  val queryGetGroupedBy = Compiled(queryDefineGetGroupedBy _)

  val queryGetCourtCaseStatus = Compiled(queryDefineGetCourtCaseStatus _)

  private def queryDefineGetPedigreeStatus(id: Column[Long]) = for (
    ped <- pedigrees if ped.id === id
  ) yield (ped.id, ped.status)

  val queryGetPedigreeStatus = Compiled(queryDefineGetPedigreeStatus _)

  private def queryDefineGetPedigreeConsistencyRun(id: Column[Long]) = for (
    ped <- pedigrees if ped.id === id
  ) yield (ped.id, ped.consistencyRun)

  val queryGetDefineGetPedigreeConsistencyRun = Compiled(queryDefineGetPedigreeConsistencyRun _)

  private def queryDefineGetAllCaseTypes() = for (
    ct <- caseTypes
  ) yield ct

  val queryGetAllCaseTypes = Compiled(queryDefineGetAllCaseTypes)

  private def queryDefineGetProfiles(id: Column[Long]) = for (
    ((ccp,pd),(pp,bpp)) <-(courtCasesProfile innerJoin profilesData
      on (_.globalCode === _.globalCode))
      leftJoin (protoProfile.filter(_.preexistence.isEmpty)
      .filter(_.status === "Imported")
      innerJoin batchProtoProfile on (_.idBatch === _.id)) on (_._2.internalSampleCode === _._1.sampleName )
      if (ccp.idCourtCase === id)
  ) yield (ccp.globalCode,pd.internalCode,pp.?,pd.category,pd.assignee,bpp.?,ccp.groupedBy)



  private def queryDefineGetProfilesStatusActive(id: Column[Long]) = for (
    ((ccp,pd),(pp,bpp)) <-(courtCasesProfile innerJoin profilesData on (_.globalCode === _.globalCode))
      leftJoin (protoProfile.filter(_.preexistence.isEmpty)
      .filter(_.status === "Imported")
      innerJoin batchProtoProfile on (_.idBatch === _.id)) on (_._2.internalSampleCode === _._1.sampleName )
    if (ccp.idCourtCase === id  && (ccp.groupedBy.isEmpty || ccp.groupedBy === ccp.globalCode ))
  ) yield (ccp.globalCode,pd.internalCode,pp.?,pd.category,pd.assignee,bpp.?,ccp.groupedBy)

  private def queryDefineGetProfilesIsReference(id: Column[Long], categories: String) = for (
    ((ccp,pd),(pp,bpp)) <-(courtCasesProfile innerJoin profilesData on (_.globalCode === _.globalCode))
      leftJoin (protoProfile.filter(_.preexistence.isEmpty)
      .filter(_.status === "Imported")
      innerJoin batchProtoProfile on (_.idBatch === _.id)) on (_._2.internalSampleCode === _._1.sampleName )
    if (ccp.idCourtCase === id && (ccp.profileType === categories))
  ) yield (ccp.globalCode,pd.internalCode,pp.?,pd.category,pd.assignee,bpp.?,ccp.groupedBy)

  private def queryDefineGetProfilesIsReferenceProfileStatusActive(id: Column[Long], categories: String) = for (
    ((ccp,pd),(pp,bpp)) <-(courtCasesProfile innerJoin profilesData on (_.globalCode === _.globalCode))
      leftJoin (protoProfile.filter(_.preexistence.isEmpty)
      .filter(_.status === "Imported")
      innerJoin batchProtoProfile on (_.idBatch === _.id)) on (_._2.internalSampleCode === _._1.sampleName )
    if (ccp.idCourtCase === id && (ccp.profileType === categories) && (ccp.groupedBy.isEmpty || ccp.groupedBy === ccp.globalCode ) )
  ) yield (ccp.globalCode,pd.internalCode,pp.?,pd.category,pd.assignee,bpp.?,ccp.groupedBy)

  private def queryDefineGetProfilesIsReferenceProfileStatusInactive(id: Column[Long], categories: String) = for (
    ((ccp,pd),(pp,bpp)) <-(courtCasesProfile innerJoin profilesData on (_.globalCode === _.globalCode))
      leftJoin (protoProfile.filter(_.preexistence.isEmpty)
      .filter(_.status === "Imported")
      innerJoin batchProtoProfile on (_.idBatch === _.id)) on (_._2.internalSampleCode === _._1.sampleName )
    if (ccp.idCourtCase === id && (ccp.profileType === categories) &&
      (ccp.groupedBy.isDefined && (ccp.groupedBy =!= ccp.globalCode)    ) )
  ) yield (ccp.globalCode,pd.internalCode,pp.?,pd.category,pd.assignee,bpp.?,ccp.groupedBy)

  private def queryDefineGetProfilesProfileStatusInactive(id: Column[Long], groupedBy: Column[String]) = for (
    ((ccp,pd),(pp,bpp)) <-(courtCasesProfile innerJoin profilesData on (_.globalCode === _.globalCode))
      leftJoin (protoProfile.filter(_.preexistence.isEmpty)
      .filter(_.status === "Imported")
      innerJoin batchProtoProfile on (_.idBatch === _.id)) on (_._2.internalSampleCode === _._1.sampleName )
    if (ccp.idCourtCase === id && (ccp.groupedBy === groupedBy) &&
      (ccp.groupedBy.isDefined && (ccp.groupedBy =!= ccp.globalCode)    ) )
  ) yield (ccp.globalCode,pd.internalCode,pp.?,pd.category,pd.assignee,bpp.?,ccp.groupedBy)

  private def queryDefineGetTotalProfiles(id: Column[Long]) = for (
    ccp <-courtCasesProfile if ccp.idCourtCase === id
  ) yield (ccp.globalCode)

  private def queryDefineGetCollapsingProfiles(id: Column[Long]) = for (
    ccp <-courtCasesProfile if ccp.idCourtCase === id && ccp.profileType === "Resto" && (ccp.globalCode === ccp.groupedBy || ccp.groupedBy.isEmpty)
  ) yield (ccp.globalCode,ccp.groupedBy)

  private def queryDefineGetTotalProfilesAssociation(id: Column[Long], profilesCod: List[String]) = for (
    ccp <-courtCasesProfile if (ccp.idCourtCase === id && (ccp.globalCode inSet profilesCod))
  ) yield (ccp.globalCode)

  private def queryDefineGetTotalProfilesIsReference(id: Column[Long], categories: String) = for (
    ccp <-courtCasesProfile if (ccp.idCourtCase === id && (ccp.profileType === categories))
  ) yield (ccp.globalCode)

  private def queryDefineGetTotalProfilesIsReferenceStatusActive(id: Column[Long], categories: String) = for (
    ccp <-courtCasesProfile if (ccp.idCourtCase === id && (ccp.profileType === categories) && (ccp.groupedBy.isEmpty || ccp.groupedBy === ccp.globalCode ) )
  ) yield (ccp.globalCode)

  private def queryDefineGetTotalProfilesIsReferenceStatusInactive(id: Column[Long], categories: String) = for (
    ccp <-courtCasesProfile if (ccp.idCourtCase === id && (ccp.profileType === categories) &&
      (ccp.groupedBy.isDefined && (ccp.groupedBy =!= ccp.globalCode)    ) )
  ) yield (ccp.globalCode)

  private def queryDefineGetTotalProfilesStatusInactive(id: Column[Long], groupedBy: Column[String]) = for (
    ccp <-courtCasesProfile if (ccp.idCourtCase === id && (ccp.groupedBy === groupedBy) &&
      (ccp.groupedBy.isDefined && (ccp.groupedBy =!= ccp.globalCode)    ) )
  ) yield (ccp.globalCode)

  private def queryDefineGetTotalProfilesIsReferenceAssociation(id: Column[Long], categories: String, profilesCod: List[String]) = for (
    ccp <-courtCasesProfile if (ccp.idCourtCase === id && (ccp.profileType === categories) && (ccp.globalCode inSet profilesCod))
  ) yield (ccp.globalCode)

  val queryGetProfiles = Compiled(queryDefineGetProfiles _)

  val queryGetCollapsingProfiles = Compiled(queryDefineGetCollapsingProfiles _)

  private def queryDefineGetCourtCasePedigrees(courtCaseId: Column[Long], name: Column[String]) = for (
    pedd <- pedigrees if (pedd.courtCaseId === courtCaseId && (name == "" || pedd.name == name))
  ) yield (pedd)

  val queryGetCourtCasePedigrees = Compiled(queryDefineGetCourtCasePedigrees _)

  private def queryDefineGetCaseTypeOfPedigree(id: Column[Long]) = for (
    (ped, cc) <- pedigrees leftJoin  courtCases
      on (_.courtCaseId === _.id) if ped.id === id
  ) yield (cc.id,cc.internalSampleCode,cc.status,cc.caseType)

  val queryGetCaseTypeOfPedigree = Compiled(queryDefineGetCaseTypeOfPedigree _)

  private def queryDefineGetActiveNNProfiles(id: Column[Long]) = for (
    (ccp) <- courtCasesProfile if ccp.idCourtCase === id && ccp.profileType === "Resto" && (ccp.groupedBy.isEmpty || ccp.groupedBy === ccp.globalCode )
  ) yield (ccp.globalCode)

  val queryGetActiveNNProfiles = Compiled(queryDefineGetActiveNNProfiles _)

  private def queryDefineGetProfilesInSet(id: Column[Long],globalCodes:List[String]) = for (
    ((ccp,pd),(pp,bpp)) <- (courtCasesProfile leftJoin profilesData
      on (_.globalCode === _.globalCode))
      leftJoin (protoProfile.filter(_.preexistence.isEmpty)
      .filter(_.status === "Imported")
      innerJoin batchProtoProfile on (_.idBatch === _.id)) on (_._2.internalSampleCode === _._1.sampleName )
    if ccp.idCourtCase === id && (ccp.globalCode inSet globalCodes)
  ) yield (ccp.globalCode,pd.internalCode,pp.?,pd.category,pd.assignee,bpp.?,ccp.groupedBy)

  private def queryDefineGetProfilesInSetStatusActive(id: Column[Long],globalCodes:List[String]) = for (
    ((ccp,pd),(pp,bpp)) <- (courtCasesProfile leftJoin profilesData on (_.globalCode === _.globalCode))
      leftJoin (protoProfile.filter(_.preexistence.isEmpty)
      .filter(_.status === "Imported")
      innerJoin batchProtoProfile on (_.idBatch === _.id)) on (_._2.internalSampleCode === _._1.sampleName )
    if ccp.idCourtCase === id && (ccp.globalCode inSet globalCodes) && (ccp.groupedBy.isEmpty || ccp.groupedBy === ccp.globalCode )
  ) yield (ccp.globalCode,pd.internalCode,pp.?,pd.category,pd.assignee,bpp.?,ccp.groupedBy)

  private def generalGetProfilesInSet(globalCodes:List[String]) = for (
    (pd,(pp,bpp)) <- profilesData leftJoin (protoProfile.filter(_.preexistence.isEmpty)
      .filter(_.status === "Imported")
      innerJoin batchProtoProfile on ( _.idBatch === _.id)) on ( _.internalCode === _._1.sampleName )
    if  (pd.globalCode inSet globalCodes)
  ) yield (pd.globalCode,pd.internalCode,pp.?,pd.category,pd.assignee,bpp.?)

  private def queryDefineGetProfilesInSetIsReference(id: Column[Long],globalCodes:List[String], categories: String) = for (
    ((ccp,pd),(pp,bpp)) <- (courtCasesProfile leftJoin profilesData
      on (_.globalCode === _.globalCode))
      leftJoin (protoProfile.filter(_.preexistence.isEmpty)
      .filter(_.status === "Imported")
      innerJoin batchProtoProfile on (_.idBatch === _.id)) on (_._2.internalSampleCode === _._1.sampleName )
    if (ccp.idCourtCase === id && (ccp.globalCode inSet globalCodes) && (ccp.profileType === categories))
  ) yield (ccp.globalCode,pd.internalCode,pp.?,pd.category,pd.assignee,bpp.?,ccp.groupedBy)

  private def queryDefineGetProfilesInSetIsReferenceStatusActive(id: Column[Long],globalCodes:List[String], categories: String) = for (
    ((ccp,pd),(pp,bpp)) <- (courtCasesProfile leftJoin profilesData on (_.globalCode === _.globalCode))
      leftJoin (protoProfile.filter(_.preexistence.isEmpty)
      .filter(_.status === "Imported")
      innerJoin batchProtoProfile on (_.idBatch === _.id)) on (_._2.internalSampleCode === _._1.sampleName )
    if (ccp.idCourtCase === id && (ccp.globalCode inSet globalCodes) && (ccp.profileType === categories) &&
      (ccp.groupedBy.isEmpty || ccp.groupedBy === ccp.globalCode ) )
  ) yield (ccp.globalCode,pd.internalCode,pp.?,pd.category,pd.assignee,bpp.?,ccp.groupedBy)

  private def queryDefineGetProfilesInSetIsReferenceStatusInactive(id: Column[Long],globalCodes:List[String], categories: String) = for (
    ((ccp,pd),(pp,bpp)) <- (courtCasesProfile leftJoin profilesData on (_.globalCode === _.globalCode))
      leftJoin (protoProfile.filter(_.preexistence.isEmpty)
      .filter(_.status === "Imported")
      innerJoin batchProtoProfile on (_.idBatch === _.id)) on (_._2.internalSampleCode === _._1.sampleName )
    if (ccp.idCourtCase === id && (ccp.globalCode inSet globalCodes) && (ccp.profileType === categories) &&
      ccp.groupedBy.isDefined )
  ) yield (ccp.globalCode,pd.internalCode,pp.?,pd.category,pd.assignee,bpp.?,ccp.groupedBy)

  private def queryDefineGetProfilesInSetStatusInactive(id: Column[Long],globalCodes:List[String],groupedBy:Column[String]) = for (
    ((ccp,pd),(pp,bpp)) <- (courtCasesProfile leftJoin profilesData on (_.globalCode === _.globalCode))
      leftJoin (protoProfile.filter(_.preexistence.isEmpty)
      .filter(_.status === "Imported")
      innerJoin batchProtoProfile on (_.idBatch === _.id)) on (_._2.internalSampleCode === _._1.sampleName )
    if (ccp.idCourtCase === id && (ccp.globalCode inSet globalCodes )&& (ccp.groupedBy === groupedBy ) && ccp.groupedBy.isDefined )
  ) yield (ccp.globalCode,pd.internalCode,pp.?,pd.category,pd.assignee,bpp.?,ccp.groupedBy)


  private def queryDefineGetTotalProfilesInSet(id: Column[Long],globalCodes:List[String]) = for (
    ccp <- courtCasesProfile if ccp.idCourtCase === id && (ccp.globalCode inSet globalCodes)
  ) yield (ccp.globalCode)
  private def queryDefineGetTotalOccurrenceProfilesInSet(globalCodes:List[String]) = for (
    (ccp,cc) <- (courtCasesProfile innerJoin courtCases.filter(_.status === "Open") on (_.idCourtCase === _.id))
    if (ccp.globalCode inSet globalCodes)
  ) yield (ccp.globalCode)

  private def queryDefineGetTotalProfilesInSetIsReference(id: Column[Long],globalCodes:List[String],categories : String) = for (
    ccp <- courtCasesProfile if (ccp.idCourtCase === id && (ccp.globalCode inSet globalCodes) && (ccp.profileType === categories))
  ) yield (ccp.globalCode)

  private def queryDefineGetTotalProfilesInSetIsReferenceStatusActive(id: Column[Long],globalCodes:List[String],categories : String) = for (
    ccp <- courtCasesProfile if (ccp.idCourtCase === id && (ccp.globalCode inSet globalCodes) && (ccp.profileType === categories) &&
       (ccp.groupedBy.isEmpty || ccp.groupedBy === ccp.globalCode ) )
  ) yield (ccp.globalCode)

  private def queryDefineGetTotalProfilesInSetIsReferenceStatusInactive(id: Column[Long],globalCodes:List[String],categories : String) = for (
    ccp <- courtCasesProfile if (ccp.idCourtCase === id && (ccp.globalCode inSet globalCodes) && (ccp.profileType === categories) &&  ccp.groupedBy.isDefined)
  ) yield (ccp.globalCode)

  private def   queryDefineGetTotalProfilesInSetStatusInactive(id: Column[Long],globalCodes:List[String], groupedBy:Column[String]) = for (
    ccp <- courtCasesProfile if (ccp.idCourtCase === id && (ccp.globalCode inSet globalCodes) && (ccp.groupedBy === groupedBy) &&  ccp.groupedBy.isDefined)
  ) yield (ccp.globalCode)

  private def generalGetTotalProfilesInSet(globalCodes: List[String]) = for (
    pd <- profilesData if (pd.globalCode inSet globalCodes)
  ) yield (pd.globalCode)

  val queryGetProfilesInSet = Compiled(queryDefineGetProfiles _)

  private def queryDefineGetSingleProfile(id: Column[Long],globalCode: Column[String]) = for (
    ccp <- courtCasesProfile if ccp.idCourtCase === id && ccp.globalCode === globalCode
  ) yield (ccp.idCourtCase,ccp.globalCode)

  val queryGetSingleProfile = Compiled(queryDefineGetSingleProfile _)

  private def queryDefineGetPedigree(id: Column[Long]) = for (
    (pedd) <- pedigrees  if pedd.id === id
  ) yield (pedd)

  private def queryDefineGetPedigreeWithCourtCaseName(id: Column[Long]) = for (
    (pedd, cc) <- pedigrees innerJoin courtCases on (_.courtCaseId === _.id)   if pedd.id === id
  ) yield (pedd,cc.internalSampleCode)

  val queryGetPedigree = Compiled(queryDefineGetPedigree _)

  val queryGetPedigreeWithCourtCaseName = Compiled(queryDefineGetPedigreeWithCourtCaseName _)

  private def queryDefineUpdatePedigree(id: Column[Long]) = for (
    ped <- pedigrees if ped.id === id
  ) yield (ped.name)

  val queryUpdatePedigree = Compiled(queryDefineUpdatePedigree _)

  override def getAllCourtCases(pedigreeIds: Option[Seq[Long]], pedigreeSearch: PedigreeSearch): Future[Seq[CourtCaseModelView]] = Future {
    DB.withSession { implicit session =>
      val searchQuery = createSearchQuery(pedigreeIds, pedigreeSearch)
      val sortedQuery = sortQuery(searchQuery, pedigreeSearch)

      sortedQuery.drop(pedigreeSearch.page * pedigreeSearch.pageSize).take(pedigreeSearch.pageSize).iterator.toVector map {
        row => CourtCaseModelView(row.id, row.internalSampleCode, row.attorney, row.court, row.assignee, row.crimeInvolved, row.crimeType, row.criminalCase, PedigreeStatus.withName(row.status),row.caseType)
      }
    }
  }

  override def getTotalCourtCases(pedigreeIds: Option[Seq[Long]], pedigreeSearch: PedigreeSearch): Future[Int] = Future {
    DB.withSession { implicit session =>
      createSearchQuery(pedigreeIds, pedigreeSearch).list.length
    }
  }

  override def createCourtCase(courtCase: CourtCaseAttempt): Future[Either[String, Long]] = Future {
    DB.withTransaction { implicit session =>
      val row = CourtCaseRow(
        0, courtCase.attorney,
        courtCase.court,
        courtCase.assignee,
        courtCase.internalSampleCode,
        crimeInvolved = courtCase.crimeInvolved,
        status = PedigreeStatus.Open.toString,
        crimeType = courtCase.crimeType,
        criminalCase = courtCase.criminalCase, caseType = courtCase.caseType)
      val ccId = (courtCases returning courtCases.map(_.id)) += row


      /*
      if (courtCase.personData != None){

      val rowFD = CourtCaseFiliationDataRow(
        0, ccId.self,
        firstname = courtCase.personData.flatMap {_.firstName},
        lastname = courtCase.personData.flatMap {_.lastName},
        sex = courtCase.personData.flatMap {_.sex.map(_.toString)},
        dateOfBirth = courtCase.personData.flatMap {_.dateOfBirth.map { x => new java.sql.Date(x.getTime) }},
        dateOfBirthFrom = courtCase.personData.flatMap {_.dateOfBirthFrom.map { x => new java.sql.Date(x.getTime) }},
        dateOfBirthTo = courtCase.personData.flatMap {_.dateOfBirthTo.map { x => new java.sql.Date(x.getTime) }},
        dateOfMissing = courtCase.personData.flatMap {_.dateOfMissing.map { x => new java.sql.Date(x.getTime) }},
        nationality = courtCase.personData.flatMap {_.nationality},
        identification = courtCase.personData.flatMap {_.identification},
        height = courtCase.personData.flatMap {_.height},
        weight = courtCase.personData.flatMap {_.weight},
        haircolor = courtCase.personData.flatMap {_.hairColor},
        skincolor = courtCase.personData.flatMap {_.skinColor},
        clothing = courtCase.personData.flatMap {_.clothing},
        alias = courtCase.personData.get.alias,
        particularities = courtCase.personData.flatMap {_.particularities})

      val ccfdId = (courtCasesFiliationData returning courtCasesFiliationData.map(_.id)) += rowFD
    }*/

      Right(ccId)

    }
  }

override def createMetadata(idCourtCase: Long, personData:  PersonData) : Future[Either[String,Long]] = Future {
  DB.withTransaction { implicit session =>
    val rowFD = CourtCaseFiliationDataRow(
      0, idCourtCase,
      firstname = personData.firstName,
      lastname = personData.lastName,
      sex = personData.sex.map(_.toString),
      dateOfBirth = personData.dateOfBirth.map { x => new java.sql.Date(x.getTime) },
      dateOfBirthFrom = personData.dateOfBirthFrom.map { x => new java.sql.Date(x.getTime) },
      dateOfBirthTo = personData.dateOfBirthTo.map { x => new java.sql.Date(x.getTime) },
      dateOfMissing = personData.dateOfMissing.map { x => new java.sql.Date(x.getTime) },
      nationality = personData.nationality,
      identification = personData.identification,
      height = personData.height,
      weight = personData.weight,
      haircolor = personData.hairColor,
      skincolor = personData.skinColor,
      clothing = personData.clothing,
      alias = personData.alias,
      particularities = personData.particularities)

    val ccfdId = (courtCasesFiliationData returning courtCasesFiliationData.map(_.id)) += rowFD
    Right(ccfdId)
  }
}

  override def getMetadata(personDataSearch:PersonDataSearch): Future[List[PersonData]] = Future {
    DB.withSession { implicit session =>
      queryDefineFiliationData(personDataSearch.idCourtCase,"%" + personDataSearch.input + "%").sortBy(_._15)
        .drop(personDataSearch.page * personDataSearch.pageSize)
        .take(personDataSearch.pageSize).list.map(pd =>
        PersonData(pd._6, pd._10, pd._12.map(Sex.withName(_)), pd._2, pd._3, pd._4, pd._5, pd._11,
          pd._9, pd._8, pd._14, pd._7, pd._13, pd._1, pd._15, pd._16)
      )
      }
    }

  override def getCourtCase(id: Long): Future[Option[CourtCase]] = Future {
    DB.withSession { implicit session =>
     val listMetadata = queryGetMetadataCourtCase(id).list.map {
       pd =>(
         PersonData(
           pd. firstname,
           pd.lastname,
           pd.sex.map(Sex.withName(_)),
           pd.dateOfBirth,
           pd.dateOfBirthFrom,
           pd.dateOfBirthTo,
           pd.dateOfMissing,
           pd.nationality,
           pd.identification,
           pd.height,
           pd.weight,
           pd.haircolor,
           pd.skincolor,
           pd.clothing,
           pd.alias,
           pd.particularities)
         )
     }
      queryGetCourtCase(id).firstOption.map { cc =>
        CourtCase(
            cc.id,
            cc.internalSampleCode,
            cc.attorney,
            cc.court,
            cc.assignee,
            cc.crimeInvolved,
            cc.crimeType,
            cc.criminalCase,
            PedigreeStatus.withName(cc.status),
            listMetadata,
            cc.caseType)
      }
    }
  }

  override def updateCourtCase(id: Long, courtCase: CourtCaseAttempt)(implicit session: Session): Either[String, Long] = {
    DB.withTransaction { implicit session =>
      val valueCC = (
        courtCase.attorney,
        courtCase.court, courtCase.crimeInvolved,
        courtCase.crimeType, courtCase.criminalCase)

      queryUpdateCourtCase(id).update(valueCC)
      val ret = Right(id)


//      if (firstResult < 1 && secondResult == 0) false else true
      ret
    }
  }

  override def updateMetadata(idCourtCase: Long, personData: PersonData) (implicit session: Session): Either[String,Long]  ={

      val valueCCFD = (
        personData.clothing,
        personData.dateOfBirth.map { x => new java.sql.Date(x.getTime) },
        personData.dateOfBirthFrom.map { x => new java.sql.Date(x.getTime) },
        personData.dateOfBirthTo.map { x => new java.sql.Date(x.getTime) },
        personData.dateOfMissing.map { x => new java.sql.Date(x.getTime) },
        personData.firstName,
        personData.hairColor,
        personData.height,
        personData.identification,
        personData.lastName,
        personData.nationality,
        personData.sex.map(_.toString()),
        personData.skinColor,
        personData.weight,
        personData.particularities)

      val resultPdf = queryUpdateCourtCaseFilationData(idCourtCase, personData.alias).update(valueCCFD)
      Right(resultPdf)
  }

  /*
  override def getAllAssociatedProfiles: Future[Seq[(SampleCode, Long)]] = Future {
    DB.withSession { implicit session =>
      queryGetAllProfiles.list.map { row => (SampleCode(row.globalCode), row.courtCase) }
    }
  }
  */

  private def createSearchQuery(pedigreeIds: Option[Seq[Long]], pedigreeSearch: PedigreeSearch) = {
    var query = for {
      cc <- courtCases
      if cc.assignee === pedigreeSearch.user || pedigreeSearch.isSuperUser
    } yield cc

    if (pedigreeIds.isDefined) {
      query = query.withFilter(cc => cc.id inSet pedigreeIds.get)
    }

    if (pedigreeSearch.code.isDefined) {
      query = query.withFilter(cc => cc.internalSampleCode.toLowerCase like s"%${pedigreeSearch.code.get.toLowerCase}%")
    }
    if (pedigreeSearch.status.isDefined) {
      query = query.withFilter(cc => cc.status === pedigreeSearch.status.get.toString)
    }
    if (pedigreeSearch.caseType.isDefined) {
      query = query.withFilter(cc => cc.caseType === pedigreeSearch.caseType.get.toString)
    }
    query
  }

  private def sortQuery(query: Query[Tables.CourtCase, Tables.CourtCaseRow, scala.Seq], pedigreeSearch: PedigreeSearch) = {
    if (pedigreeSearch.sortField.isDefined) {
      if (pedigreeSearch.ascending.isDefined && pedigreeSearch.ascending.get) {
        pedigreeSearch.sortField.get match {
          case "code" => query.sortBy(_.internalSampleCode.asc)
        }
      } else {
        pedigreeSearch.sortField.get match {
          case "code" => query.sortBy(_.internalSampleCode.desc)
        }
      }
    } else {
      query.sortBy(_.id.desc)
    }
  }

  override def changeCourCaseStatus(courtCaseId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]] = Future {
    DB.withTransaction { implicit session =>
      try {
        queryGetCourtCaseStatus(courtCaseId).update(courtCaseId, status.toString)
        Right(courtCaseId)
      } catch {
        case e: Exception => Left(e.getMessage)
      }
    }
  }
  private def getProfileCollapsingStatus(globalCode:String,groupedBy:Option[String]):String = {
    if(groupedBy.contains(globalCode) || groupedBy.isEmpty){
      CollapsingStatus.Active.toString
    }
    else{
      CollapsingStatus.Collapsed.toString
    }
  }
  override def getProfiles(courtCasePedigreeSearch:CourtCasePedigreeSearch,globalCodes:List[String] = Nil, typeProfile: Option[String]):Future[List[CourtCasePedigree]] = {
    DB.withTransaction { implicit session =>
      try {

            (globalCodes, typeProfile,courtCasePedigreeSearch.statusProfile) match {
              case (Nil, None,None) => {
                Future.successful(queryDefineGetProfiles(courtCasePedigreeSearch.idCourtCase).sortBy(_._1)
                  .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                  .take(courtCasePedigreeSearch.pageSize).list.map(x => {
                  CourtCasePedigree(x._1, x._2, Map.empty, x._3.map(_.idBatch), x._6.flatMap(_.label), getProfileCollapsingStatus(x._1, x._7), x._7)
                }))
              }
              case (Nil, Some(typProfile),None) => {
                Future.successful(queryDefineGetProfilesIsReference(courtCasePedigreeSearch.idCourtCase, typProfile).sortBy(_._1)
                  .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                  .take(courtCasePedigreeSearch.pageSize).list.map(x => {
                  CourtCasePedigree(x._1, x._2, Map.empty, x._3.map(_.idBatch), x._6.flatMap(_.label), getProfileCollapsingStatus(x._1, x._7), x._7)
                }))
              }
              case (_, None,None) => {
                if (globalCodes.isEmpty) {
                  Future.successful(Nil)
                } else {
                  Future.successful(queryDefineGetProfilesInSet(courtCasePedigreeSearch.idCourtCase, globalCodes).sortBy(_._1)
                    .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                    .take(courtCasePedigreeSearch.pageSize).list.map(x => CourtCasePedigree(x._1, x._2, Map.empty, x._3.map(_.idBatch), x._6.flatMap(_.label), getProfileCollapsingStatus(x._1, x._7), x._7)))
                }
              }
              case (_, Some(typProfile),None) => {
                if (globalCodes.isEmpty) {
                  Future.successful(Nil)
                } else {
                  Future.successful(queryDefineGetProfilesInSetIsReference(courtCasePedigreeSearch.idCourtCase, globalCodes, typProfile).sortBy(_._1)
                    .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                    .take(courtCasePedigreeSearch.pageSize).list.map(x => CourtCasePedigree(x._1, x._2, Map.empty, x._3.map(_.idBatch), x._6.flatMap(_.label), getProfileCollapsingStatus(x._1, x._7), x._7)))
                }
              }
              case (Nil, Some(typProfile),Some(CollapsingStatus.Active) ) => {
                Future.successful(queryDefineGetProfilesIsReferenceProfileStatusActive(courtCasePedigreeSearch.idCourtCase, typProfile).sortBy(_._1)
                  .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                  .take(courtCasePedigreeSearch.pageSize).list.map(x => {
                  CourtCasePedigree(x._1, x._2, Map.empty, x._3.map(_.idBatch), x._6.flatMap(_.label), getProfileCollapsingStatus(x._1, x._7), x._7)
                }))
              }
              case (_, Some(typProfile),Some(CollapsingStatus.Active) ) => {
                if (globalCodes.isEmpty) {
                  Future.successful(Nil)
                } else {
                  Future.successful(queryDefineGetProfilesInSetIsReferenceStatusActive(courtCasePedigreeSearch.idCourtCase, globalCodes, typProfile).sortBy(_._1)
                    .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                    .take(courtCasePedigreeSearch.pageSize).list.map(x => CourtCasePedigree(x._1, x._2, Map.empty, x._3.map(_.idBatch), x._6.flatMap(_.label), getProfileCollapsingStatus(x._1, x._7), x._7)))
                }
              }
              case (Nil, Some (typProfile),Some(CollapsingStatus.Collapsed)  ) => {
                 Future.successful (queryDefineGetProfilesIsReferenceProfileStatusInactive (courtCasePedigreeSearch.idCourtCase, typProfile).sortBy (_._1)
                    .drop (courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                    .take (courtCasePedigreeSearch.pageSize).list.map (x => {
                    CourtCasePedigree (x._1, x._2, Map.empty, x._3.map (_.idBatch), x._6.flatMap (_.label), getProfileCollapsingStatus (x._1, x._7), x._7)
                  }) )
              }
              case (_, Some (typProfile),Some(CollapsingStatus.Collapsed)  ) => {
                if (globalCodes.isEmpty) {
                  Future.successful (Nil)
                } else {
                  Future.successful (queryDefineGetProfilesInSetIsReferenceStatusInactive (courtCasePedigreeSearch.idCourtCase, globalCodes, typProfile).sortBy (_._1)
                  .drop (courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                  .take (courtCasePedigreeSearch.pageSize).list.map (x => CourtCasePedigree (x._1, x._2, Map.empty, x._3.map (_.idBatch), x._6.flatMap (_.label), getProfileCollapsingStatus (x._1, x._7), x._7) ) )
                  }
              }
        }
      } catch {
        case e: Exception =>
          logger.error("",e)
          Future.successful(Nil)
      }
    }
  }
  override def getProfilesNodeAssociation(courtCasePedigreeSearch:CourtCasePedigreeSearch,globalCodes:List[String] = Nil, typeProfile: Option[String], profilesCod: List[String]): Future[List[ProfileNodeAssociation]] = {
    DB.withTransaction { implicit session =>
      try {
        (globalCodes,typeProfile,courtCasePedigreeSearch.idCourtCase,courtCasePedigreeSearch.statusProfile) match {
          case (Nil,_,0,_) => {
            Future.successful(Nil)
          }
          case (_,_,0,_) => {
            if(globalCodes.isEmpty){
              Future.successful(Nil)
            }else{
              Future.successful(generalGetProfilesInSet(globalCodes).sortBy(_._1)
                .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                .take(courtCasePedigreeSearch.pageSize).list.map(x => ProfileNodeAssociation(x._1,x._2,x._4,x._5,x._6.flatMap(_.label))))
            }}
          case (Nil,None,_,None) => {
            Future.successful(
              queryDefineGetProfiles(courtCasePedigreeSearch.idCourtCase).sortBy(_._1)
              .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
              .take(courtCasePedigreeSearch.pageSize).list.map(x => ProfileNodeAssociation(x._1,x._2,x._4,x._5,x._6.flatMap(_.label)))
                .filterNot(x => profilesCod.contains(x.globalCode))
            )
          }
          case (Nil,None,_,Some(CollapsingStatus.Active)) => {
            Future.successful(
              queryDefineGetProfilesStatusActive(courtCasePedigreeSearch.idCourtCase).sortBy(_._1)
                .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                .take(courtCasePedigreeSearch.pageSize).list.map(x => ProfileNodeAssociation(x._1,x._2,x._4,x._5,x._6.flatMap(_.label)))
                .filterNot(x => profilesCod.contains(x.globalCode))
            )
          }
          case (Nil,Some(typProfile),_,None) => {
            Future.successful(queryDefineGetProfilesIsReference(courtCasePedigreeSearch.idCourtCase,typProfile).sortBy(_._1)
              .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
              .take(courtCasePedigreeSearch.pageSize).list.map(x => ProfileNodeAssociation(x._1,x._2,x._4,x._5,x._6.flatMap(_.label)))
              .filterNot(x => profilesCod.contains(x.globalCode))
            )
          }
          case (Nil,Some(typProfile),_,Some(CollapsingStatus.Active)) => {
            Future.successful(queryDefineGetProfilesIsReferenceProfileStatusActive(courtCasePedigreeSearch.idCourtCase,typProfile).sortBy(_._1)
              .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
              .take(courtCasePedigreeSearch.pageSize).list.map(x => ProfileNodeAssociation(x._1,x._2,x._4,x._5,x._6.flatMap(_.label)))
              .filterNot(x => profilesCod.contains(x.globalCode))
            )
          }
          case (_,None,_,None) => {
            if(globalCodes.isEmpty){
              Future.successful(Nil)
            }else{
              Future.successful(queryDefineGetProfilesInSet(courtCasePedigreeSearch.idCourtCase,globalCodes).sortBy(_._1)
                .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                .take(courtCasePedigreeSearch.pageSize).list.map(x => ProfileNodeAssociation(x._1,x._2,x._4,x._5,x._6.flatMap(_.label))))
            }}
          case (_,None,_,Some(CollapsingStatus.Active)) => {
            if(globalCodes.isEmpty){
              Future.successful(Nil)
            }else{
              Future.successful(queryDefineGetProfilesInSetStatusActive(courtCasePedigreeSearch.idCourtCase,globalCodes).sortBy(_._1)
                .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                .take(courtCasePedigreeSearch.pageSize).list.map(x => ProfileNodeAssociation(x._1,x._2,x._4,x._5,x._6.flatMap(_.label))))
            }}
            case (_,Some(typProfile),_,None) => {
              if(globalCodes.isEmpty){
                Future.successful(Nil)
              }else{
                Future.successful(queryDefineGetProfilesInSetIsReference(courtCasePedigreeSearch.idCourtCase,globalCodes,typProfile).sortBy(_._1)
                  .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                  .take(courtCasePedigreeSearch.pageSize).list.map(x => ProfileNodeAssociation(x._1,x._2,x._4,x._5,x._6.flatMap(_.label))))
              }
          }
          case (_,Some(typProfile),_,Some(CollapsingStatus.Active)) => {
            if(globalCodes.isEmpty){
              Future.successful(Nil)
            }else{
              Future.successful(queryDefineGetProfilesInSetIsReferenceStatusActive(courtCasePedigreeSearch.idCourtCase,globalCodes,typProfile).sortBy(_._1)
                .drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                .take(courtCasePedigreeSearch.pageSize).list.map(x => ProfileNodeAssociation(x._1,x._2,x._4,x._5,x._6.flatMap(_.label))))
            }
          }
          case(_,_,_,_) => {
            Future.successful(Nil)
          }
        }
      } catch {
        case e: Exception =>
          logger.error("",e)
          Future.successful(Nil)
      }
    }
  }
  override def getCaseTypes() : Future[Seq[CaseType]]= Future {
    DB.withSession { implicit session =>
      queryGetAllCaseTypes.list.map { cc =>
        CaseType(cc.id, cc.name)
      }
    }
  }

  override def getTotalProfiles(courtCasePedigreeSearch:CourtCasePedigreeSearch,globalCodes:List[String] = Nil, typeProfile: Option[String]):Future[Int] = {
    DB.withTransaction { implicit session =>
      try {

            (globalCodes, typeProfile, courtCasePedigreeSearch.idCourtCase,courtCasePedigreeSearch.statusProfile) match {
              case (Nil, _, 0,None) => {
                Future.successful(0)
              }
              case (_, _, 0,None) => {
                Future.successful(generalGetTotalProfilesInSet(globalCodes).length.run)
              }
              case (Nil, None, _,None) => {
                Future.successful(queryDefineGetTotalProfiles(courtCasePedigreeSearch.idCourtCase)
                  .length.run)
              }
              case (Nil, Some(typProfile), _,None) => {
                Future.successful(queryDefineGetTotalProfilesIsReference(courtCasePedigreeSearch.idCourtCase, typProfile)
                  .length.run)
              }
              case (_, None, _,None) => {
                Future.successful(queryDefineGetTotalProfilesInSet(courtCasePedigreeSearch.idCourtCase, globalCodes)
                  .length.run)
              }
              case (_, Some(typProfile), _,None) => {
                Future.successful(queryDefineGetTotalProfilesInSetIsReference(courtCasePedigreeSearch.idCourtCase, globalCodes, typProfile)
                  .length.run)
              }
              case (Nil, Some(typProfile), _, Some(CollapsingStatus.Active)) => {
                Future.successful(queryDefineGetTotalProfilesIsReferenceStatusActive(courtCasePedigreeSearch.idCourtCase, typProfile)
                  .length.run)
              }
              case (_, Some(typProfile), _,Some(CollapsingStatus.Active)) => {
                Future.successful(queryDefineGetTotalProfilesInSetIsReferenceStatusActive(courtCasePedigreeSearch.idCourtCase, globalCodes, typProfile)
                  .length.run)
              }
              case (Nil, Some(typProfile), _, Some(CollapsingStatus.Collapsed)) => {
                Future.successful(queryDefineGetTotalProfilesIsReferenceStatusInactive(courtCasePedigreeSearch.idCourtCase, typProfile)
                  .length.run)
              }
              case (_, Some(typProfile), _, Some(CollapsingStatus.Collapsed)) => {
                Future.successful(queryDefineGetTotalProfilesInSetIsReferenceStatusInactive(courtCasePedigreeSearch.idCourtCase, globalCodes, typProfile)
                  .length.run)
              }

            }
      } catch {
        case e: Exception =>
          logger.error("",e)
          Future.successful(0)
      }
    }
  }

  override def getTotalProfilesNodeAssociation(courtCasePedigreeSearch:CourtCasePedigreeSearch,globalCodes:List[String] = Nil, typeProfile: Option[String], profilesCod: List[String]):Future[Int] = {
    DB.withTransaction { implicit session =>
      try {
        (globalCodes,typeProfile, courtCasePedigreeSearch.idCourtCase) match {
          case (Nil,_,0) => {
            Future.successful(0)
          }
          case (_,_,0) => {
            Future.successful(generalGetTotalProfilesInSet(globalCodes).length.run)
          }
          case (Nil,None,_) => {
            Future.successful(queryDefineGetTotalProfilesAssociation(courtCasePedigreeSearch.idCourtCase,profilesCod)
                .length.run
            )
          }
          case (Nil,Some(typProfile),_) => {
            Future.successful(
              queryDefineGetTotalProfilesIsReferenceAssociation(courtCasePedigreeSearch.idCourtCase,typProfile, profilesCod)
              .length.run)
          }
          case (_,None,_) => {
            Future.successful(queryDefineGetTotalProfilesInSet(courtCasePedigreeSearch.idCourtCase,globalCodes)
              .length.run)
          }
          case (_,Some(typProfile),_) => {
            Future.successful(queryDefineGetTotalProfilesInSetIsReference(courtCasePedigreeSearch.idCourtCase,globalCodes,typProfile)
              .length.run)
          }

        }
      } catch {
        case e: Exception =>
          logger.error("",e)
          Future.successful(0)
      }
    }
  }
  override def getTotalProfilesOccurenceInCase(globalCode:String):Future[Int] = {
    DB.withTransaction { implicit session =>
      try {
        Future.successful(queryDefineGetTotalOccurrenceProfilesInSet(List(globalCode)).length.run)
      } catch {
        case e: Exception =>
          logger.error("", e)
          Future.successful(0)
      }

    }
  }
  override def getTotalMetadata(personDataSearch:PersonDataSearch):Future[Int] = {
    DB.withTransaction { implicit session =>
      try {
            Future.successful(queryDefineFiliationData(personDataSearch.idCourtCase,"%"+ personDataSearch.input + "%")
              .list.length.run
              )

            }
      catch {
        case e: Exception =>
          logger.error("",e)
          Future.successful(0)
      }
    }
  }


  override def addProfiles(courtCaseProfiles:List[CaseProfileAdd]):Future[Either[String, Unit]] = {
    DB.withTransaction { implicit session =>
      try {
        courtCaseProfiles.foreach(profileCase =>{
          courtCasesProfile += models.Tables.CourtCaseProfilesRow(globalCode = profileCase.globalCode,idCourtCase = profileCase.courtcaseId, profileType = profileCase.profileType.get,groupedBy = None)
        })
        Future.successful(Right(()))
      } catch {
        case e: Exception =>
          logger.error("",e)
          Future.successful(Left(e.getMessage))
      }
    }
  }


  override def removeProfiles(courtCaseProfiles:List[CaseProfileAdd]):Future[Either[String, Unit]] = {
    DB.withTransaction { implicit session =>
      try {
        courtCaseProfiles.foreach(profileCase =>{
          queryGetGroupedBy(profileCase.courtcaseId,profileCase.globalCode).update(None)
          queryGetSingleProfile(profileCase.courtcaseId,profileCase.globalCode).delete

        })
        Future.successful(Right(()))
      } catch {
        case e: Exception =>
          logger.error("",e)
          Future.successful(Left(e.getMessage))
      }
    }
  }

  override def removeMetadata( idCourtCase: Long, personData: PersonData):Future[Either[String, Unit]] = {
    DB.withTransaction { implicit session =>
      try {
          queryUpdateCourtCaseFilationData(idCourtCase, personData.alias).delete

        Future.successful(Right(()))
      } catch {
        case e: Exception =>
          logger.error("",e)
          Future.successful(Left(e.getMessage))
      }
    }
  }

  override def getProfilesFromBatches(
    courtCase:Long,
    batches:List[Long],
    tipo: Int
  ):Future[List[(String,Boolean)]] = Future{
    DB.withSession {
      implicit session =>
        val range = batches.mkString("(",",",")")
        val sqlRawQuery = queryStringGetProfilesFromBatches
          .replaceAll("RANGO", range)
        val queryGetProfilesFromBatches = Q.query[(Int, Long), (String, Boolean)](
          sqlRawQuery
        )
        queryGetProfilesFromBatches(tipo, courtCase).list
    }
  }

  override def getPedigrees(courtCasePedigreeSearch: CourtCasePedigreeSearch): Future[Seq[PedigreeMetaDataView]] = {
   Future {
     DB.withSession { implicit session =>
       val searchQuery = createSearchQueryForPedigrees(courtCasePedigreeSearch)
       val sortedQuery = sortQueryForPedigrees(searchQuery)

       sortedQuery.drop(courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
         .take(courtCasePedigreeSearch.pageSize).iterator.toVector map {
         x => PedigreeMetaDataView(x.id, x.courtCaseId, x.name, x.creationDate, PedigreeStatus.withName(x.status))
       }
     }
   }
  }

  override def getTotalPedigrees(courtCasePedigreeSearch:CourtCasePedigreeSearch):Future[Int] = {
    Future.successful(0)
  }

  override def getPedigreeMetaData(pedigreeId: Long): Future[Option[PedigreeDataCreation]] = Future {
    DB.withSession { implicit session =>
      queryGetPedigreeWithCourtCaseName(pedigreeId).firstOption.map { case (pedd,cc) =>
        PedigreeDataCreation(
          PedigreeMetaData(pedd.id, pedd.courtCaseId, pedd.name, pedd.creationDate, PedigreeStatus.withName(pedd.status), pedd.assignee, cc,Some(pedd.consistencyRun)),
          None)
      }
    }
  }

  private def createSearchQueryForPedigrees(courtCasePedigreeSearch: CourtCasePedigreeSearch) = {
    var query = for {
      pedd <- pedigrees
      if pedd.courtCaseId === courtCasePedigreeSearch.idCourtCase
    } yield pedd

    if (courtCasePedigreeSearch.input.isDefined) {
      query = query.withFilter(pedd => pedd.name.toLowerCase like s"%${courtCasePedigreeSearch.input.get.toLowerCase}%")
    }
    if (courtCasePedigreeSearch.status.isDefined) {
      query = query.withFilter(pedd => pedd.status === courtCasePedigreeSearch.status.get.toString)
    }

    if (courtCasePedigreeSearch.dateFrom.isDefined) {
      val dateFrom = courtCasePedigreeSearch.dateFrom.get
      query = query.withFilter(pedd => pedd.creationDate >= dateFrom)
    }

    if (courtCasePedigreeSearch.dateUntil.isDefined) {
      val dateUntil = courtCasePedigreeSearch.dateUntil.get
      query = query.withFilter(pedd => pedd.creationDate <= dateUntil)
    }

    query
  }

  private def sortQueryForPedigrees(query: Query[Tables.Pedigree, Tables.PedigreeRow, scala.Seq]) = {
    query.sortBy(_.creationDate.desc)
  }

  override def createOrUpdatePedigreeMetadata(pedigreeMetaData: PedigreeMetaData): Future[Either[String, Long]] = Future {
    DB.withTransaction { implicit session =>
      pedigreeMetaData.id match {
        case 0 => {
          val row = PedigreeRow(0, pedigreeMetaData.courtCaseId, pedigreeMetaData.name, new Timestamp(Calendar.getInstance().getTimeInMillis), assignee = pedigreeMetaData.assignee)
          val ccId = (pedigrees returning pedigrees.map(_.id)) += row
          Right(ccId)
        }
        case _ => {
          queryUpdatePedigree(pedigreeMetaData.id).update(pedigreeMetaData.name)
          Right(pedigreeMetaData.id)
        }
      }
    }
  }

 override def deleteFisicalPedigree(pedigreeId : Long) : Future[Either[String, Long]] = {
   DB.withTransaction { implicit session =>
     try {
       queryGetPedigree(pedigreeId).delete
       Future.successful(Right(pedigreeId))
     } catch {
       case e: Exception =>
         logger.error("", e)
         Future.successful(Left(e.getMessage))
     }
   }
 }

  override def changePedigreeStatus(pedigreeId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]] = Future {
    DB.withTransaction { implicit session =>
      try {
        queryGetPedigreeStatus(pedigreeId).update(pedigreeId, status.toString)
        Right(pedigreeId)
      } catch {
        case e: Exception => Left(e.getMessage)
      }
    }
  }

  override def getProfilesToDelete(courtCaseId: Long) : Future[Seq[SampleCode]] = Future{
    DB.withSession { implicit session =>
      val queryGetProfilesToDelete = Q.query[(Long), (String)](queryProfilesToDelete)
      queryGetProfilesToDelete(courtCaseId).list
        .map(x => SampleCode(x))
    }
  }
// devuelve una lista con el perfil y un booleano que indica si es padre o no
  override def getProfilesForCollapsing(idCourtCase:Long):Future[List[(String,Boolean)]] = Future{
    DB.withSession { implicit session =>
      queryGetCollapsingProfiles(idCourtCase).list.map(x => (x._1,x._2.contains(x._1)))
    }
  }

  override def getCourtCaseOfPedigree(pedigreeId : Long) : Future[Option[CourtCase]] = Future {
    DB.withSession { implicit session =>
      queryGetCaseTypeOfPedigree(pedigreeId).firstOption.map { cc =>
        CourtCase(cc._1, cc._2, None, None, "", None, None, None, PedigreeStatus.withName(cc._3), List(), cc._4)
      }
    }
  }

  override def getActiveNNProfiles(courtCaseId : Long) : Future[Seq[SampleCode]] = Future {
    DB.withSession { implicit session =>
      queryGetActiveNNProfiles(courtCaseId).list.map { x => SampleCode(x) }
    }
  }

  override def getPedigreeDescriptionById(pedigreeId:Long):Future[(Option[String],Option[String])] = {
    Future {
      DB.withSession { implicit session =>

        val searchQuery = (pedigrees.innerJoin(courtCases).on(_.courtCaseId === _.id)).filter(_._1.id === pedigreeId)

        val r = searchQuery.list.headOption.map(x => (x._1.name,x._2.internalSampleCode))
        (r.map(_._1),r.map(_._2))
      }
    }
  }

  override def disassociateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] =  {
      this.runInTransactionAsync { implicit session => {
        try{
          courtCaseProfiles.foreach( profilesGrouped =>
          courtCasesProfile.filter(_.idCourtCase=== profilesGrouped.courtcaseId)
            .filter( _.globalCode === profilesGrouped.globalCode)
            .map(x=>x.groupedBy).update(None) )

          Right(())
        }catch{
          case e: Exception => {
            logger.error(e.getMessage,e)
            Left(Messages("error.E0205"))
        }
      }
      }
    }
    }


  override def getTotalProfilesInactive(courtCasePedigreeSearch:CourtCasePedigreeSearch, globalCode: List[String]):Future[Int] = {
    DB.withTransaction { implicit session =>
      try {

        (globalCode,courtCasePedigreeSearch.groupedBy) match {

          case (Nil ,Some(groupedBy)) => {
            Future.successful(queryDefineGetTotalProfilesStatusInactive(courtCasePedigreeSearch.idCourtCase, groupedBy)
              .length.run)
          }
          case ( _,Some( groupedBy)) => {
            Future.successful(queryDefineGetTotalProfilesInSetStatusInactive(courtCasePedigreeSearch.idCourtCase, globalCode, groupedBy)
              .length.run)
          }

        }
      } catch {
        case e: Exception =>
          logger.error("",e)
          Future.successful(0)
      }
    }
  }

  override def getProfilesInactive(courtCasePedigreeSearch:CourtCasePedigreeSearch, globalCodes: List[String]):Future[List[CourtCasePedigree]] = {
    DB.withTransaction { implicit session =>
      try {

        (globalCodes,courtCasePedigreeSearch.groupedBy ) match {

          case (Nil,Some(groupedBy)  ) => {
            Future.successful (queryDefineGetProfilesProfileStatusInactive(courtCasePedigreeSearch.idCourtCase, groupedBy).sortBy (_._1)
              .drop (courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
              .take (courtCasePedigreeSearch.pageSize).list.map (x => {
              CourtCasePedigree (x._1, x._2, Map.empty, x._3.map (_.idBatch), x._6.flatMap (_.label), getProfileCollapsingStatus (x._1, x._7), x._7)
            }) )
          }
          case (_, Some(groupedBy) ) => {
            if (globalCodes.isEmpty) {
              Future.successful (Nil)
            } else {
              Future.successful (queryDefineGetProfilesInSetStatusInactive (courtCasePedigreeSearch.idCourtCase, globalCodes, groupedBy).sortBy (_._1)
                .drop (courtCasePedigreeSearch.page * courtCasePedigreeSearch.pageSize)
                .take (courtCasePedigreeSearch.pageSize).list.map (x => CourtCasePedigree (x._1, x._2, Map.empty, x._3.map (_.idBatch), x._6.flatMap (_.label), getProfileCollapsingStatus (x._1, x._7), x._7) ) )
            }
          }
        }
      } catch {
        case e: Exception =>
          logger.error("",e)
          Future.successful(Nil)
      }
    }
  }

  override def associateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] =  {
    this.runInTransactionAsync { implicit session => {
      try{
        courtCaseProfiles.foreach( profilesGrouped =>
          courtCasesProfile.filter(_.idCourtCase=== profilesGrouped.courtcaseId)
            .filter( _.globalCode === profilesGrouped.globalCode)
            .map(x=>x.groupedBy).update(profilesGrouped.groupedBy) )

        Right(())
      }catch{
        case e: Exception => {
          logger.error(e.getMessage,e)
          Left(Messages("error.E0206"))
        }
      }
    }
    }
  }
  override def changePedigreeConsistencyFlag(pedigreeId: Long, consistencyRun:Boolean = true): Future[Either[String, Long]] =  Future {
    DB.withTransaction { implicit session =>
      try {
        queryGetDefineGetPedigreeConsistencyRun(pedigreeId).update(pedigreeId, consistencyRun)
        Right(pedigreeId)
      } catch {
        case e: Exception => Left(e.getMessage)
      }
    }
  }

  private def queryCleanConsistencyPedigree(id: Column[Long]) = for (
    ped <- pedigrees if ped.id === id
  ) yield (ped.consistencyRun)

  val queryDoCleanConsistencyPedigree = Compiled(queryCleanConsistencyPedigree _)

  override def doCleanConsistency(pedigreeId:Long): Future[Either[String, Long]] = Future {
    DB.withTransaction { implicit session =>
      queryDoCleanConsistencyPedigree(pedigreeId).update(false)
      Right(pedigreeId)
    }
  }

}
