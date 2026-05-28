package pedigree

import jakarta.inject.{Inject, Singleton}
import models.Tables
import models.Tables.*
import play.api.Logger
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api.*
import slick.jdbc.{GetResult, PositionedResult}
import types.SampleCode

import java.sql.{Date, Timestamp}
import java.util.Calendar
import scala.concurrent.{ExecutionContext, Future}

// ---------------------------------------------------------------------------
// SlickPedigreeDataRepository
// Migrated from legacy SlickPedigreeDataRepository (Slick 2 → Slick 3.5).
// ---------------------------------------------------------------------------

@Singleton
class SlickPedigreeDataRepository @Inject() (
  db: Database
)(implicit ec: ExecutionContext) extends PedigreeDataRepository:

  private val logger: Logger = Logger(this.getClass)

  private val courtCases             = Tables.CourtCase
  private val courtCasesFiliationData = Tables.CourtCaseFiliationData
  private val caseTypes              = Tables.CaseType
  private val courtCasesProfile      = Tables.CourtCaseProfiles
  private val pedigrees              = Tables.Pedigree
  private val profilesData           = Tables.PedigreeProfileData
  private val protoProfile           = Tables.PedigreeProtoProfile
  private val batchProtoProfile      = Tables.PedigreeBatchProtoProfile

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private def getProfileCollapsingStatus(globalCode: String, groupedBy: Option[String]): String =
    if groupedBy.contains(globalCode) || groupedBy.isEmpty then
      CollapsingStatus.Active.toString
    else
      CollapsingStatus.Collapsed.toString

  /** Convert a CourtCaseFiliationDataRow to PersonData. */
  private def rowToPersonData(pd: CourtCaseFiliationDataRow): PersonData = PersonData(
    firstName      = pd.firstname,
    lastName       = pd.lastname,
    sex            = pd.sex.map(Sex.withName),
    dateOfBirth    = pd.dateOfBirth,
    dateOfBirthFrom = pd.dateOfBirthFrom,
    dateOfBirthTo  = pd.dateOfBirthTo,
    dateOfMissing  = pd.dateOfMissing,
    nationality    = pd.nationality,
    identification = pd.identification,
    height         = pd.height,
    weight         = pd.weight,
    hairColor      = pd.haircolor,
    skinColor      = pd.skincolor,
    clothing       = pd.clothing,
    alias          = pd.alias,
    particularities = pd.particularities
  )

  // Result type for profile-join queries:
  // (globalCode, internalCode, idBatch, category, assignee, batchLabel, groupedBy)
  private type ProfileJoinTuple =
    (String, String, Option[Long], String, String, Option[String], Option[String])

  private def tupleToCourtCasePedigree(x: ProfileJoinTuple): CourtCasePedigree =
    CourtCasePedigree(
      globalCode    = x._1,
      internalCode  = x._2,
      genotypification = Map.empty,
      idBatch       = x._3,
      batchLabel    = x._6,
      statusProfile = getProfileCollapsingStatus(x._1, x._7),
      groupedBy     = x._7
    )

  private def tupleToProfileNodeAssociation(x: ProfileJoinTuple): ProfileNodeAssociation =
    ProfileNodeAssociation(x._1, x._2, x._4, x._5, x._6)

  // ---------------------------------------------------------------------------
  // Base queries (Slick 3.5 style)
  // ---------------------------------------------------------------------------

  private def baseProfileQuery(ccId: Long) =
    (courtCasesProfile.filter(_.idCourtCase === ccId)
      join profilesData on (_.globalCode === _.globalCode)
      joinLeft (
        protoProfile.filter(_.preexistence.isEmpty).filter(_.status === "Imported")
          join batchProtoProfile on (_.idBatch === _.id)
      ) on { case ((ccp, pd), (pp, _)) => pp.sampleName === pd.internalSampleCode })
      .map { case ((ccp, pd), ppBppOpt) =>
        (ccp.globalCode, pd.internalCode, ppBppOpt.map(_._1.idBatch), pd.category, pd.assignee,
          ppBppOpt.map(_._2.label).flatten, ccp.groupedBy, ccp.profileType)
      }

  // ---------------------------------------------------------------------------
  // getAllCourtCases
  // ---------------------------------------------------------------------------

  override def getAllCourtCases(
    pedigreeIds: Option[Seq[Long]],
    pedigreeSearch: PedigreeSearch
  ): Future[Seq[CourtCaseModelView]] =
    val q = buildCourtCaseQuery(pedigreeIds, pedigreeSearch)
    db.run(q.result).map { rows =>
      rows.map(cc =>
        CourtCaseModelView(cc.id, cc.internalSampleCode, cc.attorney, cc.court, cc.assignee,
          cc.crimeInvolved, cc.crimeType, cc.criminalCase, PedigreeStatus.withName(cc.status), cc.caseType)
      )
    }

  override def getTotalCourtCases(
    pedigreeIds: Option[Seq[Long]],
    pedigreeSearch: PedigreeSearch
  ): Future[Int] =
    val q = buildCourtCaseQuery(pedigreeIds, pedigreeSearch)
    db.run(q.length.result)

  private def buildCourtCaseQuery(
    pedigreeIds: Option[Seq[Long]],
    s: PedigreeSearch
  ) = {
    var q = if s.isOwnCases then courtCases.filter(_.assignee === s.user) else courtCases
    pedigreeIds.foreach { ids => q = q.filter(_.id.inSet(ids)) }
    if s.input.nonEmpty then
      val pattern = s"${s.input.toLowerCase}%"
      q = q.filter(_.internalSampleCode.toLowerCase like pattern)
    s.status.foreach { st => q = q.filter(_.status === st.toString) }
    s.caseType.foreach { ct => q = q.filter(_.caseType === ct) }
    val sorted = s.sortField match
      case Some("code") if s.ascending.contains(true)  => q.sortBy(_.internalSampleCode.asc)
      case Some("code")                                  => q.sortBy(_.internalSampleCode.desc)
      case _                                             => q.sortBy(_.id.desc)
    sorted.drop(s.page * s.pageSize).take(s.pageSize)
  }

  // ---------------------------------------------------------------------------
  // createCourtCase / createMetadata
  // ---------------------------------------------------------------------------

  override def createCourtCase(courtCase: CourtCaseAttempt): Future[Either[String, Long]] =
    val row = CourtCaseRow(
      id = 0L,
      attorney = courtCase.attorney,
      court = courtCase.court,
      assignee = courtCase.assignee,
      internalSampleCode = courtCase.internalSampleCode,
      crimeInvolved = courtCase.crimeInvolved,
      crimeType = courtCase.crimeType,
      criminalCase = courtCase.criminalCase,
      status = PedigreeStatus.Open.toString,
      caseType = courtCase.caseType
    )
    db.run((courtCases returning courtCases.map(_.id)) += row)
      .map(id => Right(id))
      .recover { case e => logger.error(s"createCourtCase failed for code=${courtCase.internalSampleCode}", e); Left("error.E0630") }

  override def createMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Long]] =
    val row = CourtCaseFiliationDataRow(
      id = 0L,
      courtCaseId = idCourtCase,
      firstname = personData.firstName,
      lastname = personData.lastName,
      sex = personData.sex.map(_.toString),
      dateOfBirth = personData.dateOfBirth.map(d => new java.sql.Date(d.getTime)),
      dateOfBirthFrom = personData.dateOfBirthFrom.map(d => new java.sql.Date(d.getTime)),
      dateOfBirthTo = personData.dateOfBirthTo.map(d => new java.sql.Date(d.getTime)),
      dateOfMissing = personData.dateOfMissing.map(d => new java.sql.Date(d.getTime)),
      nationality = personData.nationality,
      identification = personData.identification,
      height = personData.height,
      weight = personData.weight,
      haircolor = personData.hairColor,
      skincolor = personData.skinColor,
      clothing = personData.clothing,
      alias = personData.alias,
      particularities = personData.particularities
    )
    db.run((courtCasesFiliationData returning courtCasesFiliationData.map(_.id)) += row)
      .map(id => Right(id))
      .recover { case e => logger.error(s"createMetadata failed for courtCase=$idCourtCase alias=${personData.alias}", e); Left("error.E0630") }

  // ---------------------------------------------------------------------------
  // getCourtCase / getMetadata
  // ---------------------------------------------------------------------------

  override def getCourtCase(courtCaseId: Long): Future[Option[CourtCase]] =
    val metaAction = courtCasesFiliationData.filter(_.courtCaseId === courtCaseId).result
    val ccAction   = courtCases.filter(_.id === courtCaseId).result.headOption
    db.run(metaAction zip ccAction).map { case (metas, ccOpt) =>
      ccOpt.map { cc =>
        val personDataList = metas.toList.map(rowToPersonData)
        pedigree.CourtCase(cc.id, cc.internalSampleCode, cc.attorney, cc.court, cc.assignee,
          cc.crimeInvolved, cc.crimeType, cc.criminalCase, PedigreeStatus.withName(cc.status),
          personDataList, cc.caseType)
      }
    }

  override def getMetadata(personDataSearch: PersonDataSearch): Future[List[PersonData]] =
    val pattern = s"%${personDataSearch.input}%"
    val q = courtCasesFiliationData.filter(r =>
      r.courtCaseId === personDataSearch.idCourtCase &&
        (r.alias.toLowerCase.like(pattern) ||
          r.firstname.toLowerCase.like(pattern) ||
          r.lastname.toLowerCase.like(pattern))
    ).sortBy(_.alias)
      .drop(personDataSearch.page * personDataSearch.pageSize)
      .take(personDataSearch.pageSize)
    db.run(q.result).map(_.toList.map(rowToPersonData))

  // ---------------------------------------------------------------------------
  // updateCourtCase / updateMetadata
  // ---------------------------------------------------------------------------

  override def updateCourtCase(id: Long, courtCase: CourtCaseAttempt): Future[Either[String, Long]] =
    val q = courtCases.filter(_.id === id)
      .map(cc => (cc.attorney, cc.court, cc.crimeInvolved, cc.crimeType, cc.criminalCase))
      .update((courtCase.attorney, courtCase.court, courtCase.crimeInvolved, courtCase.crimeType, courtCase.criminalCase))
    db.run(q).map(_ => Right(id)).recover { case e => logger.error(s"updateCourtCase failed for id=$id", e); Left("error.E0630") }

  override def updateMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Long]] =
    val q = courtCasesFiliationData
      .filter(r => r.courtCaseId === idCourtCase && r.alias === personData.alias)
      .map(r => (r.clothing, r.dateOfBirth, r.dateOfBirthFrom, r.dateOfBirthTo, r.dateOfMissing,
        r.firstname, r.haircolor, r.height, r.identification, r.lastname,
        r.nationality, r.sex, r.skincolor, r.weight, r.particularities))
      .update((
        personData.clothing,
        personData.dateOfBirth.map(d => new java.sql.Date(d.getTime)),
        personData.dateOfBirthFrom.map(d => new java.sql.Date(d.getTime)),
        personData.dateOfBirthTo.map(d => new java.sql.Date(d.getTime)),
        personData.dateOfMissing.map(d => new java.sql.Date(d.getTime)),
        personData.firstName,
        personData.hairColor,
        personData.height,
        personData.identification,
        personData.lastName,
        personData.nationality,
        personData.sex.map(_.toString),
        personData.skinColor,
        personData.weight,
        personData.particularities
      ))
    db.run(q).map((n: Int) => Right(n.toLong)).recover { case e => logger.error(s"updateMetadata failed for courtCase=$idCourtCase alias=${personData.alias}", e); Left("error.E0630") }

  // ---------------------------------------------------------------------------
  // changeCourCaseStatus
  // ---------------------------------------------------------------------------

  override def changeCourCaseStatus(courtCaseId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]] =
    db.run(
      courtCases.filter(_.id === courtCaseId)
        .map(cc => (cc.id, cc.status))
        .update((courtCaseId, status.toString))
    ).map(_ => Right(courtCaseId)).recover { case e => logger.error(s"changeCourCaseStatus failed for courtCase=$courtCaseId status=$status", e); Left("error.E0630") }

  // ---------------------------------------------------------------------------
  // getCaseTypes
  // ---------------------------------------------------------------------------

  override def getCaseTypes(): Future[Seq[CaseType]] =
    db.run(caseTypes.result).map(_.map(r => pedigree.CaseType(r.id, r.name)))

  // ---------------------------------------------------------------------------
  // getProfiles / getTotalProfiles (complex multi-branch queries)
  // ---------------------------------------------------------------------------

  override def getProfiles(
    search: CourtCasePedigreeSearch,
    globalCodes: List[String] = Nil,
    typeProfile: Option[String]
  ): Future[List[CourtCasePedigree]] =
    val q = buildProfileQuery(search, globalCodes, typeProfile, search.statusProfile)
    db.run(q.sortBy(_._1).drop(search.page * search.pageSize).take(search.pageSize).result)
      .map(_.toList.map { case (gc, ic, idBatch, cat, asn, lbl, grp, _) =>
        CourtCasePedigree(gc, ic, Map.empty, idBatch, lbl, getProfileCollapsingStatus(gc, grp), grp)
      }).recover { case e => logger.error(s"getProfiles failed for courtCase=${search.idCourtCase} typeProfile=$typeProfile", e); Nil }

  override def getTotalProfiles(
    search: CourtCasePedigreeSearch,
    globalCodes: List[String] = Nil,
    typeProfile: Option[String]
  ): Future[Int] =
    val q = buildProfileQuery(search, globalCodes, typeProfile, search.statusProfile)
    db.run(q.length.result).recover { case e => logger.error(s"getTotalProfiles failed for courtCase=${search.idCourtCase} typeProfile=$typeProfile", e); 0 }

  private def buildProfileQuery(
    search: CourtCasePedigreeSearch,
    globalCodes: List[String],
    typeProfile: Option[String],
    statusProfile: Option[CollapsingStatus.Value]
  ) = {
    var q = baseProfileQuery(search.idCourtCase)
    if globalCodes.nonEmpty then q = q.filter(_._1.inSet(globalCodes))
    typeProfile.foreach { tp => q = q.filter(_._8 === tp) }
    statusProfile match
      case Some(CollapsingStatus.Active) =>
        q = q.filter(r => r._7.isEmpty || r._7 === r._1)
      case Some(CollapsingStatus.Collapsed) =>
        q = q.filter(r => r._7.isDefined && r._7 =!= r._1)
      case None => // no filter
    q
  }

  // ---------------------------------------------------------------------------
  // getTotalProfilesOccurenceInCase
  // ---------------------------------------------------------------------------

  override def getTotalProfilesOccurenceInCase(globalCodes: String): Future[Int] =
    val q = courtCasesProfile
      .join(courtCases.filter(_.status === "Open")).on(_.idCourtCase === _.id)
      .filter(_._1.globalCode === globalCodes)
      .length
    db.run(q.result).recover { case e => logger.error(s"getTotalProfilesOccurenceInCase failed for globalCode=$globalCodes", e); 0 }

  // ---------------------------------------------------------------------------
  // getTotalProfilesNodeAssociation
  // ---------------------------------------------------------------------------

  override def getTotalProfilesNodeAssociation(
    search: CourtCasePedigreeSearch,
    globalCodes: List[String] = Nil,
    typeProfile: Option[String],
    profilesCod: List[String]
  ): Future[Int] =
    if search.idCourtCase == 0 && globalCodes.isEmpty then
      Future.successful(0)
    else
      val q =
        if search.idCourtCase == 0 then
          profilesData.filter(_.globalCode.inSet(globalCodes)).length
        else
          val base = courtCasesProfile.filter(_.idCourtCase === search.idCourtCase)
          val filtered = typeProfile match
            case Some(tp) =>
              val withType = base.filter(_.profileType === tp)
              if profilesCod.nonEmpty then withType.filter(_.globalCode.inSet(profilesCod))
              else withType.filterNot(_.globalCode.inSet(profilesCod))
            case None =>
              if globalCodes.nonEmpty then base.filter(_.globalCode.inSet(globalCodes))
              else base.filterNot(_.globalCode.inSet(profilesCod))
          filtered.length
      db.run(q.result).recover { case e => logger.error(s"getTotalProfilesNodeAssociation failed for courtCase=${search.idCourtCase} typeProfile=$typeProfile", e); 0 }

  // ---------------------------------------------------------------------------
  // getTotalMetadata
  // ---------------------------------------------------------------------------

  override def getTotalMetadata(personDataSearch: PersonDataSearch): Future[Int] =
    val pattern = s"%${personDataSearch.input}%"
    val q = courtCasesFiliationData.filter(r =>
      r.courtCaseId === personDataSearch.idCourtCase &&
        (r.alias.toLowerCase.like(pattern) ||
          r.firstname.toLowerCase.like(pattern) ||
          r.lastname.toLowerCase.like(pattern))
    ).length
    db.run(q.result).recover { case e => logger.error(s"getTotalMetadata failed for courtCase=${personDataSearch.idCourtCase}", e); 0 }

  // ---------------------------------------------------------------------------
  // addProfiles / removeProfiles / removeMetadata
  // ---------------------------------------------------------------------------

  override def addProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] =
    val rows = courtCaseProfiles.map(p =>
      CourtCaseProfilesRow(p.courtcaseId, p.globalCode, p.profileType.getOrElse(""), None)
    )
    db.run(DBIO.seq(rows.map(courtCasesProfile += _)*).transactionally)
      .map(_ => Right(())).recover { case e => logger.error(s"addProfiles failed for ${courtCaseProfiles.size} profiles", e); Left("error.E0630") }

  override def removeProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] =
    val actions = courtCaseProfiles.map { p =>
      val resetGroupedBy = courtCasesProfile
        .filter(r => r.idCourtCase === p.courtcaseId && r.globalCode === p.globalCode)
        .map(_.groupedBy).update(None)
      val deleteRow = courtCasesProfile
        .filter(r => r.idCourtCase === p.courtcaseId && r.globalCode === p.globalCode)
        .delete
      resetGroupedBy >> deleteRow
    }
    db.run(DBIO.seq(actions*).transactionally)
      .map(_ => Right(())).recover { case e => logger.error(s"removeProfiles failed for ${courtCaseProfiles.size} profiles", e); Left("error.E0630") }

  override def removeMetadata(idCourtCase: Long, personData: PersonData): Future[Either[String, Unit]] =
    db.run(
      courtCasesFiliationData
        .filter(r => r.courtCaseId === idCourtCase && r.alias === personData.alias)
        .delete
    ).map(_ => Right(())).recover { case e => logger.error(s"removeMetadata failed for courtCase=$idCourtCase alias=${personData.alias}", e); Left("error.E0630") }

  // ---------------------------------------------------------------------------
  // getProfilesFromBatches (raw SQL)
  // ---------------------------------------------------------------------------

  override def getProfilesFromBatches(courtCase: Long, batches: List[Long], tipo: Int): Future[List[(String, Boolean)]] =
    if batches.isEmpty then
      Future.successful(Nil)
    else
      // IN list de Long: splice como literal (typesafe — Long sólo contiene dígitos/signo).
      val batchIds = batches.mkString(",")
      val tipoStr  = tipo.toString
      implicit val getResult: GetResult[(String, Boolean)] =
        GetResult(r => (r.nextString(), r.nextBoolean()))
      val query =
        sql"""SELECT pd."GLOBAL_CODE", cat."IS_REFERENCE"
              FROM "APP"."BATCH_PROTO_PROFILE" bpp
                INNER JOIN "APP"."PROTO_PROFILE" pp
                INNER JOIN "APP"."PROFILE_DATA" pd ON pp."SAMPLE_NAME" = pd."INTERNAL_SAMPLE_CODE"
                  ON (pp."ID_BATCH" = bpp."ID")
                INNER JOIN "APP"."CATEGORY" cat ON (pd."CATEGORY" = cat."ID")
              WHERE pp."STATUS" = 'Imported'
                AND cat."PEDIGREE_ASSOCIATION" = TRUE
                AND cat."TYPE" = $tipoStr
                AND bpp."ID" IN (#$batchIds)
                AND pd."DELETED" = FALSE
                AND pd."GLOBAL_CODE" NOT IN (
                  SELECT ccp."GLOBAL_CODE" FROM "APP"."COURT_CASE_PROFILE" ccp
                  WHERE ccp."ID_COURT_CASE" = $courtCase)""".as[(String, Boolean)]
      db.run(query).map(_.toList).recover { case e => logger.error(s"getProfilesFromBatches failed for courtCase=$courtCase batches=$batchIds", e); Nil }

  // ---------------------------------------------------------------------------
  // getProfilesNodeAssociation
  // ---------------------------------------------------------------------------

  override def getProfilesNodeAssociation(
    search: CourtCasePedigreeSearch,
    globalCodes: List[String] = Nil,
    typeProfile: Option[String],
    profilesCod: List[String]
  ): Future[List[ProfileNodeAssociation]] =
    if search.idCourtCase == 0 && globalCodes.isEmpty then
      Future.successful(Nil)
    else
      val q =
        if search.idCourtCase == 0 then
          // general search across all cases
          (profilesData
            joinLeft (
              protoProfile.filter(_.preexistence.isEmpty).filter(_.status === "Imported")
                join batchProtoProfile on (_.idBatch === _.id)
            ) on { case (pd, (pp, _)) => pp.sampleName === pd.internalSampleCode })
            .filter(_._1.globalCode.inSet(globalCodes))
            .map { case (pd, ppBppOpt) =>
              (pd.globalCode, pd.internalCode, Option.empty[Long], pd.category, pd.assignee,
                ppBppOpt.map(_._2.label).flatten, Option.empty[String], "")
            }
        else
          buildProfileQuery(search, globalCodes, typeProfile, search.statusProfile)
      db.run(
        q.sortBy(_._1)
          .drop(search.page * search.pageSize)
          .take(search.pageSize)
          .result
      ).map { rows =>
        val profileNodeAssociations = rows.toList.map { case (gc, ic, _, cat, asn, lbl, _, _) =>
          ProfileNodeAssociation(gc, ic, cat, asn, lbl)
        }
        profileNodeAssociations.filterNot(p => profilesCod.contains(p.globalCode))
      }.recover { case e => logger.error(s"getProfilesNodeAssociation failed for courtCase=${search.idCourtCase} typeProfile=$typeProfile", e); Nil }

  // ---------------------------------------------------------------------------
  // getPedigrees / getTotalPedigrees
  // ---------------------------------------------------------------------------

  override def getPedigrees(search: CourtCasePedigreeSearch): Future[Seq[PedigreeMetaDataView]] =
    val q = buildPedigreeQuery(search)
    db.run(q.sortBy(_.creationDate.desc)
      .drop(search.page * search.pageSize)
      .take(search.pageSize)
      .result)
      .map(_.map(r => PedigreeMetaDataView(r.id, r.courtCaseId, r.name, Some(r.creationDate), PedigreeStatus.withName(r.status))))

  override def getTotalPedigrees(search: CourtCasePedigreeSearch): Future[Int] =
    db.run(buildPedigreeQuery(search).length.result)

  private def buildPedigreeQuery(search: CourtCasePedigreeSearch) =
    var q = pedigrees.filter(_.courtCaseId === search.idCourtCase)
    search.input.foreach { inp => q = q.filter(_.name.toLowerCase like s"%${inp.toLowerCase}%") }
    search.status.foreach { st => q = q.filter(_.status === st.toString) }
    search.dateFrom.foreach { df => q = q.filter(_.creationDate >= df) }
    search.dateUntil.foreach { du => q = q.filter(_.creationDate <= du) }
    q

  // ---------------------------------------------------------------------------
  // getPedigreeMetaData
  // ---------------------------------------------------------------------------

  override def getPedigreeMetaData(pedigreeId: Long): Future[Option[PedigreeDataCreation]] =
    val q = pedigrees
      .join(courtCases).on(_.courtCaseId === _.id)
      .filter(_._1.id === pedigreeId)
    db.run(q.result.headOption).map(_.map { case (pedd, cc) =>
      PedigreeDataCreation(
        PedigreeMetaData(
          id          = pedd.id,
          courtCaseId = pedd.courtCaseId,
          name        = pedd.name,
          courtCaseName = cc.internalSampleCode,
          assignee    = pedd.assignee,
          creationDate = Some(pedd.creationDate),
          status      = PedigreeStatus.withName(pedd.status),
          consistencyRun = Some(pedd.consistencyRun)
        ),
        None
      )
    })

  // ---------------------------------------------------------------------------
  // createOrUpdatePedigreeMetadata
  // ---------------------------------------------------------------------------

  override def createOrUpdatePedigreeMetadata(pedigreeMetaData: PedigreeMetaData): Future[Either[String, Long]] =
    if pedigreeMetaData.id == 0L then
      val row = PedigreeRow(
        id           = 0L,
        courtCaseId  = pedigreeMetaData.courtCaseId,
        name         = pedigreeMetaData.name,
        creationDate = new Date(Calendar.getInstance().getTimeInMillis),
        status       = PedigreeStatus.UnderConstruction.toString,
        assignee     = pedigreeMetaData.assignee
      )
      db.run((pedigrees returning pedigrees.map(_.id)) += row)
        .map(id => Right(id))
        .recover { case e => logger.error(s"createPedigreeMetadata failed for courtCase=${pedigreeMetaData.courtCaseId}", e); Left("error.E0630") }
    else
      db.run(pedigrees.filter(_.id === pedigreeMetaData.id).map(_.name).update(pedigreeMetaData.name))
        .map(_ => Right(pedigreeMetaData.id))
        .recover { case e => logger.error(s"updatePedigreeMetadata failed for pedigree=${pedigreeMetaData.id}", e); Left("error.E0630") }

  // ---------------------------------------------------------------------------
  // changePedigreeStatus / deleteFisicalPedigree
  // ---------------------------------------------------------------------------

  override def changePedigreeStatus(pedigreeId: Long, status: PedigreeStatus.Value): Future[Either[String, Long]] =
    db.run(
      pedigrees.filter(_.id === pedigreeId)
        .map(p => (p.id, p.status))
        .update((pedigreeId, status.toString))
    ).map(_ => Right(pedigreeId)).recover { case e => logger.error(s"changePedigreeStatus failed for pedigree=$pedigreeId status=$status", e); Left("error.E0630") }

  override def deleteFisicalPedigree(pedigreeId: Long): Future[Either[String, Long]] =
    db.run(pedigrees.filter(_.id === pedigreeId).delete)
      .map(_ => Right(pedigreeId))
      .recover { case e => logger.error(s"deleteFisicalPedigree failed for pedigree=$pedigreeId", e); Left("error.E0630") }

  // ---------------------------------------------------------------------------
  // getProfilesToDelete (raw SQL)
  // ---------------------------------------------------------------------------

  override def getProfilesToDelete(courtCaseId: Long): Future[Seq[SampleCode]] =
    implicit val gr: GetResult[String] = GetResult(_.nextString())
    val query =
      sql"""SELECT "GLOBAL_CODE" FROM "APP"."COURT_CASE_PROFILE"
            WHERE "GLOBAL_CODE" IN (
              SELECT "GLOBAL_CODE" FROM (
                SELECT "GLOBAL_CODE", COUNT("CC"."ID") AS "CANT"
                FROM "APP"."COURT_CASE_PROFILE"
                  LEFT JOIN (
                    SELECT "ID" FROM "APP"."COURT_CASE"
                    WHERE "APP"."COURT_CASE"."STATUS" = 'Open'
                  ) "CC" ON "APP"."COURT_CASE_PROFILE"."ID_COURT_CASE" = "CC"."ID"
                GROUP BY "GLOBAL_CODE"
              ) "COURT_CASE_PROFILE_GROUPED"
              WHERE "CANT" = 0
            ) AND "ID_COURT_CASE" = $courtCaseId""".as[String]
    db.run(query).map(_.map(SampleCode.apply))
      .recover { case e => logger.error(s"getProfilesToDelete failed for courtCase=$courtCaseId", e); Seq.empty }

  // ---------------------------------------------------------------------------
  // getProfilesForCollapsing
  // ---------------------------------------------------------------------------

  override def getProfilesForCollapsing(idCourtCase: Long): Future[List[(String, Boolean)]] =
    val q = courtCasesProfile
      .filter(r => r.idCourtCase === idCourtCase &&
        r.profileType === "Resto" &&
        (r.globalCode === r.groupedBy || r.groupedBy.isEmpty))
      .map(r => (r.globalCode, r.groupedBy))
    db.run(q.result).map(_.toList.map { case (gc, grp) => (gc, grp.contains(gc)) })

  // ---------------------------------------------------------------------------
  // getCourtCaseOfPedigree
  // ---------------------------------------------------------------------------

  override def getCourtCaseOfPedigree(pedigreeId: Long): Future[Option[CourtCase]] =
    val q = pedigrees
      .join(courtCases).on(_.courtCaseId === _.id)
      .filter(_._1.id === pedigreeId)
      .map(_._2)
    db.run(q.result.headOption).map(_.map { cc =>
      pedigree.CourtCase(cc.id, cc.internalSampleCode, None, None, "", None, None, None,
        PedigreeStatus.withName(cc.status), List(), cc.caseType)
    })

  // ---------------------------------------------------------------------------
  // getActiveNNProfiles
  // ---------------------------------------------------------------------------

  override def getActiveNNProfiles(courtCaseId: Long): Future[Seq[SampleCode]] =
    val q = courtCasesProfile.filter(r =>
      r.idCourtCase === courtCaseId &&
        r.profileType === "Resto" &&
        (r.groupedBy.isEmpty || r.groupedBy === r.globalCode)
    ).map(_.globalCode)
    db.run(q.result).map(_.map(SampleCode.apply))

  // ---------------------------------------------------------------------------
  // getPedigreeDescriptionById
  // ---------------------------------------------------------------------------

  override def getPedigreeDescriptionById(pedigreeId: Long): Future[(Option[String], Option[String])] =
    val q = pedigrees
      .join(courtCases).on(_.courtCaseId === _.id)
      .filter(_._1.id === pedigreeId)
      .map { case (ped, cc) => (ped.name, cc.internalSampleCode) }
    db.run(q.result.headOption).map { opt =>
      (opt.map(_._1), opt.map(_._2))
    }

  // ---------------------------------------------------------------------------
  // disassociateGroupedProfiles / associateGroupedProfiles
  // ---------------------------------------------------------------------------

  override def disassociateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] =
    val actions = courtCaseProfiles.map { p =>
      courtCasesProfile
        .filter(r => r.idCourtCase === p.courtcaseId && r.globalCode === p.globalCode)
        .map(_.groupedBy)
        .update(None)
    }
    db.run(DBIO.seq(actions*).transactionally)
      .map(_ => Right(())).recover { case e => logger.error(e.getMessage, e); Left("error.E0205") }

  override def associateGroupedProfiles(courtCaseProfiles: List[CaseProfileAdd]): Future[Either[String, Unit]] =
    val actions = courtCaseProfiles.map { p =>
      courtCasesProfile
        .filter(r => r.idCourtCase === p.courtcaseId && r.globalCode === p.globalCode)
        .map(_.groupedBy)
        .update(p.groupedBy)
    }
    db.run(DBIO.seq(actions*).transactionally)
      .map(_ => Right(())).recover { case e => logger.error(e.getMessage, e); Left("error.E0206") }

  // ---------------------------------------------------------------------------
  // getTotalProfilesInactive / getProfilesInactive
  // ---------------------------------------------------------------------------

  override def getTotalProfilesInactive(
    search: CourtCasePedigreeSearch,
    globalCode: List[String]
  ): Future[Int] =
    search.groupedBy match
      case None => Future.successful(0)
      case Some(groupedBy) =>
        val base = courtCasesProfile.filter(r =>
          r.idCourtCase === search.idCourtCase &&
            r.groupedBy === groupedBy &&
            r.groupedBy.isDefined &&
            r.groupedBy =!= r.globalCode
        )
        val q = if globalCode.nonEmpty then base.filter(_.globalCode.inSet(globalCode)) else base
        db.run(q.length.result).recover { case e => logger.error(s"getTotalProfilesInactive failed for courtCase=${search.idCourtCase} groupedBy=${search.groupedBy.getOrElse("")}", e); 0 }

  override def getProfilesInactive(
    search: CourtCasePedigreeSearch,
    globalCodes: List[String]
  ): Future[List[CourtCasePedigree]] =
    search.groupedBy match
      case None => Future.successful(Nil)
      case Some(groupedBy) =>
        val base = baseProfileQuery(search.idCourtCase)
          .filter(r =>
            r._7 === groupedBy &&
              r._7.isDefined &&
              r._7 =!= r._1
          )
        val q = if globalCodes.nonEmpty then base.filter(_._1.inSet(globalCodes)) else base
        db.run(q.sortBy(_._1)
          .drop(search.page * search.pageSize)
          .take(search.pageSize)
          .result)
          .map(_.toList.map { case (gc, ic, idBatch, _, _, lbl, grp, _) =>
            CourtCasePedigree(gc, ic, Map.empty, idBatch, lbl, getProfileCollapsingStatus(gc, grp), grp)
          }).recover { case e => logger.error(s"getProfilesInactive failed for courtCase=${search.idCourtCase} groupedBy=${search.groupedBy.getOrElse("")}", e); Nil }

  // ---------------------------------------------------------------------------
  // changePedigreeConsistencyFlag / doCleanConsistency
  // ---------------------------------------------------------------------------

  override def changePedigreeConsistencyFlag(pedigreeId: Long, consistencyRun: Boolean = true): Future[Either[String, Long]] =
    db.run(
      pedigrees.filter(_.id === pedigreeId)
        .map(p => (p.id, p.consistencyRun))
        .update((pedigreeId, consistencyRun))
    ).map(_ => Right(pedigreeId)).recover { case e => logger.error(s"changePedigreeConsistencyFlag failed for pedigree=$pedigreeId", e); Left("error.E0630") }

  override def doCleanConsistency(pedigreeId: Long): Future[Either[String, Long]] =
    db.run(pedigrees.filter(_.id === pedigreeId).map(_.consistencyRun).update(false))
      .map(_ => Right(pedigreeId))
      .recover { case e => logger.error(s"doCleanConsistency failed for pedigree=$pedigreeId", e); Left("error.E0630") }
