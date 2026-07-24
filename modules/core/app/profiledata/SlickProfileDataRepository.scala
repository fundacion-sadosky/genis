package profiledata

import jakarta.inject.{Inject, Named, Singleton}
import models.Tables
import models.Tables.*
import play.api.Logging
import play.api.i18n.{Lang, MessagesApi}
import profile.MtRCRS
import slick.jdbc.PostgresProfile.api.*
import types.{AlphanumericId, SampleCode}

import java.sql.Timestamp
import java.util.Calendar
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class SlickProfileDataRepository @Inject()(
  db: slick.jdbc.JdbcBackend.Database,
  messagesApi: MessagesApi
)(implicit ec: ExecutionContext) extends ProfileDataRepository with Logging:

  private implicit val lang: Lang = Lang("es")
  private def msg(key: String, args: Any*): String = messagesApi(key, args*)(lang)

  // ---------------------------------------------------------------------------
  // Table references (protected so SlickStashProfileDataRepository can override)
  // ---------------------------------------------------------------------------
  protected val pdTable          = Tables.profilesData
  protected val pdfTable         = Tables.profileDataFiliations
  protected val pdfResTable      = Tables.profileDataFiliationResources
  private val extPdTable       = Tables.externalProfileData
  private val pdMotiveTable    = Tables.profileDataMotive
  private val profileUplTable  = Tables.profileUploaded
  private val profileSentTable = Tables.profileSent
  private val profileRecTable  = Tables.profileReceived
  private val mtRcrsTable      = Tables.mitochondrialRcrs
  private val supApprTable     = Tables.superiorInstanceProfileApproval
  private val labTable         = Tables.laboratories

  // ---------------------------------------------------------------------------
  // Private DBIO helpers
  // ---------------------------------------------------------------------------

  private def profileDataById(id: Long): DBIO[Option[(ProfileDataRow, Option[ProfileDataFiliationRow], Option[ExternalProfileDataRow])]] =
    (pdTable
      .filter(_.id === id)
      .joinLeft(pdfTable).on(_.globalCode === _.profileData)
      .joinLeft(extPdTable).on(_._1.id === _.id))
      .result
      .headOption
      .map(_.map { case ((pd, pdf), epd) => (pd, pdf, epd) })

  private def profileDataByGc(gc: String): DBIO[Option[(ProfileDataRow, Option[ProfileDataFiliationRow], Option[ExternalProfileDataRow])]] =
    (pdTable
      .filter(_.globalCode === gc)
      .joinLeft(pdfTable).on(_.globalCode === _.profileData)
      .joinLeft(extPdTable).on(_._1.id === _.id))
      .result
      .headOption
      .map(_.map { case ((pd, pdf), epd) => (pd, pdf, epd) })

  private def resourcesAction(gc: String, rType: String): DBIO[Seq[Long]] =
    pdfResTable.filter(r => r.profileDataFiliation === gc && r.resourceType === rType).map(_.id).result

  private def toDataFiliation(gc: String, df: ProfileDataFiliationRow): DBIO[DataFiliation] =
    for
      inprints   <- resourcesAction(gc, "I")
      pictures   <- resourcesAction(gc, "P")
      signatures <- resourcesAction(gc, "S")
    yield DataFiliation(
      df.fullName, df.nickname, df.birthday.map(d => new java.util.Date(d.getTime)),
      df.birthPlace, df.nationality, df.identification, df.identificationIssuingAuthority,
      df.address, inprints.toList, pictures.toList, signatures.toList
    )

  private def rowToProfileData(pd: ProfileDataRow, pdf: Option[ProfileDataFiliationRow], epd: Option[ExternalProfileDataRow]): DBIO[ProfileData] =
    pdf match
      case None => DBIO.successful(buildProfileData(pd, None, epd.isDefined))
      case Some(df) =>
        toDataFiliation(pd.globalCode, df).map { filiation =>
          buildProfileData(pd, Some(filiation), epd.isDefined)
        }

  private def buildProfileData(pd: ProfileDataRow, df: Option[DataFiliation], isExternal: Boolean): ProfileData =
    ProfileData(
      category = AlphanumericId(pd.category),
      globalCode = SampleCode(pd.globalCode),
      attorney = pd.attorney,
      bioMaterialType = pd.bioMaterialType,
      court = pd.court,
      crimeInvolved = pd.crimeInvolved,
      crimeType = pd.crimeType,
      criminalCase = pd.criminalCase,
      internalSampleCode = pd.internalSampleCode,
      assignee = pd.assignee,
      laboratory = pd.laboratory,
      deleted = pd.deleted,
      deletedMotive = None,
      responsibleGeneticist = pd.responsibleGeneticist,
      profileExpirationDate = pd.profileExpirationDate.map(d => new java.util.Date(d.getTime)),
      sampleDate = pd.sampleDate.map(d => new java.util.Date(d.getTime)),
      sampleEntryDate = pd.sampleEntryDate.map(d => new java.util.Date(d.getTime)),
      dataFiliation = df,
      isExternal = isExternal
    )

  // ---------------------------------------------------------------------------
  // ProfileDataRepository interface
  // ---------------------------------------------------------------------------

  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] =
    db.run(
      (pdTable.filter(_.globalCode === globalCode.text)
        .joinLeft(extPdTable).on(_.id === _.id))
        .result
        .headOption
        .map(_.map { case (pd, epd) =>
          buildProfileData(pd, None, epd.isDefined)
        })
    )

  override def isDeleted(globalCode: SampleCode): Future[Option[Boolean]] =
    db.run(pdTable.filter(_.globalCode === globalCode.text).map(_.deleted).result.headOption)

  override def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]] =
    db.run(profileUplTable.filter(_.globalCode === globalCode.text).map(_.status).result.headOption)
      .recover { case _ => None }

  override def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]] =
    db.run(
      profileUplTable
        .filter(_.globalCode inSet codes.map(_.text))
        .map(_.globalCode)
        .result
        .map(_.toList.map(SampleCode(_)))
    )

  override def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]] =
    db.run(
      pdTable
        .filter(pd => pd.internalSampleCode === internalSampleCode && !pd.deleted)
        .map(_.globalCode)
        .result
        .headOption
        .map(_.flatMap(s => Try(SampleCode(s)).toOption))
    )

  // Full interface methods

  def get(id: Long): Future[ProfileData] =
    db.run(profileDataById(id).flatMap {
      case None => DBIO.failed(new NoSuchElementException(s"ProfileData not found for id=$id"))
      case Some((pd, pdf, epd)) => rowToProfileData(pd, pdf, epd)
    })

  def get(globalCode: SampleCode): Future[Option[ProfileData]] =
    db.run(profileDataByGc(globalCode.text).flatMap {
      case None => DBIO.successful(None)
      case Some((pd, pdf, epd)) => rowToProfileData(pd, pdf, epd).map(Some(_))
    })

  def add(
    profileData: ProfileData,
    completeLabCode: String,
    imageList: Option[List[java.io.File]] = None,
    picturesList: Option[List[java.io.File]] = None,
    signaturesList: Option[List[java.io.File]] = None
  ): Future[SampleCode] =
    val action = for
      nextVal    <- sql"""select nextval('"APP"."PROFILE_DATA_GLOBAL_CODE_seq"')""".as[Long].head
      globalCode  = completeLabCode + "-" + nextVal
      pdRow       = ProfileDataRow(
                      id = 0L,
                      category = profileData.category.text,
                      globalCode = globalCode,
                      internalCode = profileData.internalSampleCode,
                      attorney = profileData.attorney,
                      bioMaterialType = profileData.bioMaterialType,
                      court = profileData.court,
                      crimeInvolved = profileData.crimeInvolved,
                      crimeType = profileData.crimeType,
                      criminalCase = profileData.criminalCase,
                      internalSampleCode = profileData.internalSampleCode,
                      assignee = profileData.assignee,
                      laboratory = profileData.laboratory,
                      profileExpirationDate = profileData.profileExpirationDate.map(d => new java.sql.Date(d.getTime)),
                      responsibleGeneticist = profileData.responsibleGeneticist,
                      sampleDate = profileData.sampleDate.map(d => new java.sql.Date(d.getTime)),
                      sampleEntryDate = profileData.sampleEntryDate.map(d => new java.sql.Date(d.getTime))
                    )
      _          <- pdTable += pdRow
      _          <- profileData.dataFiliation.fold(DBIO.successful(()))(df => insertFiliation(globalCode, df, imageList, picturesList, signaturesList))
    yield SampleCode(globalCode)
    db.run(action.transactionally)

  private def insertFiliation(
    gc: String,
    df: DataFiliation,
    imageList: Option[List[java.io.File]],
    picturesList: Option[List[java.io.File]],
    signaturesList: Option[List[java.io.File]]
  ): DBIO[Unit] =
    val fRow = ProfileDataFiliationRow(
      id = 0L, profileData = gc,
      fullName = df.fullName, nickname = df.nickname,
      birthday = df.birthday.map(d => new java.sql.Date(d.getTime)),
      birthPlace = df.birthPlace, nationality = df.nationality,
      identification = df.identification,
      identificationIssuingAuthority = df.identificationIssuingAuthority,
      address = df.address
    )
    for
      _ <- pdfTable += fRow
      _ <- insertResources(gc, "I", imageList)
      _ <- insertResources(gc, "P", picturesList)
      _ <- insertResources(gc, "S", signaturesList)
    yield ()

  protected def insertResources(gc: String, rType: String, files: Option[List[java.io.File]]): DBIO[Unit] =
    files.fold(DBIO.successful(())) { list =>
      DBIO.sequence(list.map { file =>
        val bytes = scala.util.Using.resource(new java.io.FileInputStream(file))(_.readAllBytes())
        pdfResTable += ProfileDataFiliationResourcesRow(0L, gc, bytes, rType)
      }).map(_ => ())
    }

  def updateProfileData(
    globalCode: SampleCode,
    newProfile: ProfileData,
    imageList: Option[List[java.io.File]] = None,
    picturesList: Option[List[java.io.File]] = None,
    signaturesList: Option[List[java.io.File]] = None
  ): Future[Boolean] =
    val action = for
      updated <- pdTable
                   .filter(_.globalCode === globalCode.text)
                   .map(pd => (pd.category, pd.attorney, pd.bioMaterialType, pd.court, pd.crimeInvolved,
                               pd.crimeType, pd.criminalCase, pd.laboratory, pd.responsibleGeneticist,
                               pd.profileExpirationDate, pd.sampleDate, pd.sampleEntryDate))
                   .update((
                     newProfile.category.text, newProfile.attorney, newProfile.bioMaterialType,
                     newProfile.court, newProfile.crimeInvolved, newProfile.crimeType, newProfile.criminalCase,
                     newProfile.laboratory, newProfile.responsibleGeneticist,
                     newProfile.profileExpirationDate.map(d => new java.sql.Date(d.getTime)),
                     newProfile.sampleDate.map(d => new java.sql.Date(d.getTime)),
                     newProfile.sampleEntryDate.map(d => new java.sql.Date(d.getTime))
                   ))
      _ <- newProfile.dataFiliation.fold(DBIO.successful(()))(df => upsertFiliation(globalCode.text, df, imageList, picturesList, signaturesList))
    yield updated >= 1
    db.run(action.transactionally)

  private def upsertFiliation(
    gc: String,
    df: DataFiliation,
    imageList: Option[List[java.io.File]],
    picturesList: Option[List[java.io.File]],
    signaturesList: Option[List[java.io.File]]
  ): DBIO[Unit] =
    val fData = (df.fullName, df.nickname, df.birthday.map(d => new java.sql.Date(d.getTime)),
                 df.birthPlace, df.nationality, df.identification, df.identificationIssuingAuthority, df.address)
    for
      updated <- pdfTable
                   .filter(_.profileData === gc)
                   .map(pdf => (pdf.fullName, pdf.nickname, pdf.birthday, pdf.birthPlace, pdf.nationality,
                                pdf.identification, pdf.identificationIssuingAuthority, pdf.address))
                   .update(fData)
      _ <- (if updated == 0 then
               pdfTable += ProfileDataFiliationRow(0L, gc, df.fullName, df.nickname,
                 df.birthday.map(d => new java.sql.Date(d.getTime)), df.birthPlace, df.nationality,
                 df.identification, df.identificationIssuingAuthority, df.address)
             else DBIO.successful(0))
      _ <- insertResources(gc, "I", imageList)
      _ <- insertResources(gc, "P", picturesList)
      _ <- insertResources(gc, "S", signaturesList)
    yield ()

  def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]] =
    db.run(
      pdfResTable
        .filter(r => r.id === id && r.resourceType === resourceType)
        .map(_.resource)
        .result
        .headOption
    )

  def delete(globalCode: SampleCode, motive: DeletedMotive): Future[Int] =
    val now = new Timestamp(Calendar.getInstance().getTimeInMillis)
    val action = for
      updated <- pdTable
                   .filter(_.globalCode === globalCode.text)
                   .map(pd => (pd.deleted, pd.deletedSolicitor, pd.deletedMotive))
                   .update((true, Some(motive.solicitor), Some(motive.motive)))
      idOpt   <- pdTable.filter(_.globalCode === globalCode.text).map(_.id).result.headOption
      _       <- idOpt.fold(DBIO.successful(0L)) { id =>
                   pdMotiveTable += ProfileDataMotiveRow(0L, id, now, motive.selectedMotive)
                 }
    yield updated
    db.run(action.transactionally)

  def removeProfile(globalCode: SampleCode): Future[Either[String, SampleCode]] =
    db.run(pdTable.filter(_.globalCode === globalCode.text).delete.transactionally).map {
      case 0 => Left(msg("error.E0940"))
      case _ => Right(globalCode)
    }

  def getTotalProfilesByUser(search: ProfileDataSearch): Future[Int] =
    val base = profilesByUserBaseQuery(search.userId, search.isSuperUser, search.active, search.inactive, search.category)
    db.run(
      if search.notUploaded.contains(true) then
        base.joinLeft(profileUplTable).on(_._1.id === _.id).filter(_._2.isEmpty).length.result
      else
        base.length.result
    )

  def getTotalProfilesByUser(userId: String, isSuperUser: Boolean, category: String = ""): Future[Int] =
    val q = if category.isEmpty then
      pdTable.filter(pd => isSuperUser.bind || pd.assignee === userId)
    else
      pdTable.filter(pd => (isSuperUser.bind || pd.assignee === userId) && pd.category === category)
    db.run(q.length.result)

  private def profilesByUserBaseQuery(userId: String, isSuperUser: Boolean, active: Boolean, inactive: Boolean, category: String) =
    (pdTable
      .filter(pd =>
        (isSuperUser.bind || pd.assignee === userId) &&
        ((pd.deleted && inactive.bind) || (!pd.deleted && active.bind)) &&
        (category.isEmpty.bind || pd.category === category)
      )
      .joinLeft(extPdTable).on(_.id === _.id))
      .sortBy(_._1.globalCode.desc)

  def getProfilesByUser(search: ProfileDataSearch): Future[Seq[ProfileDataFull]] =
    val base = profilesByUserBaseQuery(search.userId, search.isSuperUser, search.active, search.inactive, search.category)
    val withUploaded = base.joinLeft(profileUplTable).on(_._1._1.id === _.id)
    db.run(
      (if search.notUploaded.contains(true) then
        withUploaded.filter(_._2.isEmpty)
      else
        withUploaded)
        .drop(search.page * search.pageSize)
        .take(search.pageSize)
        .result
        .map(_.map { case ((pd, epd), pdu) =>
          val replicated = pdu.exists(u => u.status != 20L && u.status != 3L)
          ProfileDataFull(
            category = AlphanumericId(pd.category),
            globalCode = SampleCode(pd.globalCode),
            attorney = pd.attorney,
            bioMaterialType = pd.bioMaterialType,
            court = pd.court,
            crimeInvolved = pd.crimeInvolved,
            crimeType = pd.crimeType,
            criminalCase = pd.criminalCase,
            internalSampleCode = pd.internalSampleCode,
            assignee = pd.assignee,
            laboratory = pd.laboratory,
            deleted = pd.deleted,
            deletedMotive = None,
            responsibleGeneticist = pd.responsibleGeneticist,
            profileExpirationDate = pd.profileExpirationDate.map(d => new java.util.Date(d.getTime)),
            sampleDate = pd.sampleDate.map(d => new java.util.Date(d.getTime)),
            sampleEntryDate = pd.sampleEntryDate.map(d => new java.util.Date(d.getTime)),
            dataFiliation = None,
            readOnly = replicated,
            isExternal = epd.isDefined
          )
        })
    )

  def giveGlobalCode(labCode: String): Future[String] =
    db.run(
      labTable.filter(_.codeName === labCode).result.head.map { lab =>
        s"${lab.country}-${lab.province}-${lab.codeName}"
      }
    )

  def isDesktopProfile(globalCode: SampleCode): Future[Option[Boolean]] =
    db.run(pdTable.filter(_.globalCode === globalCode.text).map(_.fromDesktopSearch).result.headOption)

  def getDeletedMotive(globalCode: SampleCode): Future[Option[DeletedMotive]] =
    db.run(
      pdTable
        .filter(_.globalCode === globalCode.text)
        .map(pd => (pd.deletedSolicitor, pd.deletedMotive))
        .result
        .headOption
        .map(_.flatMap {
          case (Some(sol), Some(mot)) => Some(DeletedMotive(sol, mot))
          case _ => None
        })
    )

  def findByCodes(globalCodes: List[SampleCode]): Future[Seq[ProfileData]] =
    val codes = globalCodes.map(_.text)
    db.run(
      (pdTable.filter(_.globalCode inSet codes)
        .joinLeft(pdfTable).on(_.globalCode === _.profileData)
        .joinLeft(extPdTable).on(_._1.id === _.id))
        .result
        .flatMap { rows =>
          DBIO.sequence(rows.map { case ((pd, pdf), epd) =>
            rowToProfileData(pd, pdf, epd)
          })
        }
    )

  def getDesktopProfiles(): Future[Seq[SampleCode]] =
    db.run(pdTable.filter(_.fromDesktopSearch === true).map(_.globalCode).result.map(_.map(SampleCode(_))))

  def addExternalProfile(profileData: ProfileData, labOrigin: String, labImmediate: String): Future[SampleCode] =
    val gc = profileData.globalCode.text
    val action = for
      pdRow <- DBIO.successful(ProfileDataRow(
                 id = 0L, category = profileData.category.text, globalCode = gc,
                 internalCode = profileData.internalSampleCode, attorney = profileData.attorney,
                 bioMaterialType = profileData.bioMaterialType, court = profileData.court,
                 crimeInvolved = profileData.crimeInvolved, crimeType = profileData.crimeType,
                 criminalCase = profileData.criminalCase, internalSampleCode = profileData.internalSampleCode,
                 assignee = profileData.assignee, laboratory = profileData.laboratory,
                 profileExpirationDate = profileData.profileExpirationDate.map(d => new java.sql.Date(d.getTime)),
                 responsibleGeneticist = profileData.responsibleGeneticist,
                 sampleDate = profileData.sampleDate.map(d => new java.sql.Date(d.getTime)),
                 sampleEntryDate = profileData.sampleEntryDate.map(d => new java.sql.Date(d.getTime))
               ))
      id    <- (pdTable returning pdTable.map(_.id)) += pdRow
      _     <- extPdTable += ExternalProfileDataRow(id, labOrigin, labImmediate)
    yield SampleCode(gc)
    db.run(action.transactionally)

  def updateUploadStatus(
    globalCode: String,
    status: Long,
    motive: Option[String],
    interconnectionError: Option[String],
    userName: Option[String],
    operationOriginatedInInstance: String
  ): Future[Either[String, Unit]] =
    val action = for
      pdOpt <- pdTable.filter(_.globalCode === globalCode).result.headOption
      result <- pdOpt match
        case None => DBIO.successful(Left(msg("error.E0940")))
        case Some(pd) =>
          for
            existingDateOpt <- profileUplTable.filter(_.globalCode === globalCode).map(_.dateUploaded).result.headOption.map(_.flatten)
            newDate          = if status == 2L then Some(new Timestamp(System.currentTimeMillis())) else existingDateOpt
            row              = ProfileUploadedRow(pd.id, pd.globalCode, status, motive, interconnectionError, userName, Some(operationOriginatedInInstance), newDate)
            _               <- profileUplTable.insertOrUpdate(row)
          yield Right(())
    yield result
    db.run(action.transactionally).recover { case e =>
      logger.error(s"updateUploadStatus failed for $globalCode", e)
      Left(msg("error.E0941"))
    }

  def findUploadedProfilesByCodes(globalCodes: Seq[SampleCode]): Future[Seq[SampleCode]] =
    db.run(
      profileUplTable
        .filter(_.globalCode inSet globalCodes.map(_.text))
        .map(_.globalCode)
        .result
        .map(_.map(SampleCode(_)))
    )

  def getExternalProfileDataByGlobalCode(globalCode: String): Future[Option[ExternalProfileDataRow]] =
    db.run(
      (extPdTable join pdTable on (_.id === _.id))
        .filter(_._2.globalCode === globalCode)
        .map(_._1)
        .result
        .headOption
    )

  def gefFailedProfilesUploaded(): Future[List[ProfileUploadedRow]] =
    db.run(profileUplTable.filter(_.status === 1L).result.map(_.toList))
      .recover { case e => logger.error("gefFailedProfilesUploaded failed", e); Nil }

  def gefFailedProfilesUploadedDeleted(): Future[List[ProfileUploadedRow]] =
    db.run(profileUplTable.filter(_.status === 5L).result.map(_.toList))
      .recover { case e => logger.error("gefFailedProfilesUploadedDeleted failed", e); Nil }

  def gefFailedProfilesSentDeleted(labCode: String): Future[List[ProfileSentRow]] =
    db.run(profileSentTable.filter(r => r.status === 5L && r.labCode === labCode).result.map(_.toList))
      .recover { case e => logger.error("gefFailedProfilesSentDeleted failed", e); Nil }

  def updateProfileSentStatus(
    globalCode: String,
    status: Long,
    motive: Option[String],
    labCode: String,
    interconnectionError: Option[String]
  ): Future[Either[String, Unit]] =
    val action = for
      pdOpt <- pdTable.filter(_.globalCode === globalCode).result.headOption
      result <- pdOpt match
        case None => DBIO.successful(Left(msg("error.E0940")))
        case Some(pd) =>
          profileSentTable
            .insertOrUpdate(ProfileSentRow(pd.id, labCode, pd.globalCode, status, motive, interconnectionError))
            .map(_ => Right(()))
    yield result
    db.run(action.transactionally).recover { case e =>
      logger.error(s"updateProfileSentStatus failed for $globalCode", e)
      Left(msg("error.E0941"))
    }

  def getMtRcrs(): Future[MtRCRS] =
    db.run(mtRcrsTable.result.map(rows => MtRCRS(rows.map(r => r.position -> r.base).toMap)))
      .recover { case _ => MtRCRS(Map.empty) }

  def addProfileReceivedApproved(
    labCode: String, globalCode: String, status: Long, userName: String, isCategoryModification: Boolean
  ): Future[Either[String, Unit]] =
    val row = ProfileReceivedRow(globalCode, labCode, status, None, Some(userName),
                isCategoryModification, None, labCode, Some(new Timestamp(System.currentTimeMillis())))
    db.run(profileRecTable.insertOrUpdate(row).map(_ => Right(())).transactionally)
      .recover { case e =>
        logger.error("addProfileReceivedApproved error", e)
        Left(msg("error.E0941"))
      }

  def addProfileReceivedRejected(
    labCode: String, globalCode: String, status: Long, motive: String, userName: String, isCategoryModification: Boolean
  ): Future[Either[String, Unit]] =
    val row = ProfileReceivedRow(globalCode, labCode, status,
                Some("Rechazado por el motivo: " + motive), Some(userName),
                isCategoryModification, None, labCode, Some(new Timestamp(System.currentTimeMillis())))
    db.run(profileRecTable.insertOrUpdate(row).map(_ => Right(())).transactionally)
      .recover { case e =>
        logger.error("addProfileReceivedRejected error", e)
        Left(msg("error.E0941"))
      }

  def updateInterconnectionError(
    globalCode: String, status: Long, interconnectionError: String
  ): Future[Either[String, Unit]] =
    val action = for
      rowOpt <- profileRecTable.filter(_.globalCode === globalCode).result.headOption
      result <- rowOpt match
        case None => DBIO.successful(Left(msg("error.E0940")))
        case Some(row) =>
          profileRecTable
            .insertOrUpdate(row.copy(status = status, interconnectionError = Some(interconnectionError),
              operationOriginatedInInstance = ""))
            .map(_ => Right(()))
    yield result
    db.run(action.transactionally).recover { case e =>
      logger.error(s"updateInterconnectionError failed for $globalCode", e)
      Left(msg("error.E0941"))
    }

  def updateProfileReceivedStatus(
    labCode: String, globalCode: String, status: Long, motive: Option[String],
    isCategoryModification: Boolean, interconnectionError: Option[String],
    userName: Option[String], operationOriginatedInInstance: String
  ): Future[Either[String, Unit]] =
    val action = for
      existingOpt <- profileRecTable.filter(_.globalCode === globalCode).result.headOption
      dateToSave   = existingOpt.flatMap(_.dateReceived)
                       .orElse(Some(new Timestamp(System.currentTimeMillis())))
      row          = ProfileReceivedRow(globalCode, labCode, status, motive, userName,
                       isCategoryModification, interconnectionError, operationOriginatedInInstance, dateToSave)
      _           <- profileRecTable.insertOrUpdate(row)
    yield Right(())
    db.run(action.transactionally).recover { case e =>
      logger.error(s"updateProfileReceivedStatus failed for $globalCode", e)
      Left(msg("error.E0941"))
    }

  def getPendingApprovalNotification(labCode: String): Future[Seq[ProfileReceivedRow]] =
    db.run(profileRecTable.filter(r => r.status === 22L && r.labCode === labCode).result)

  def getPendingRejectionNotification(labCode: String): Future[Seq[ProfileReceivedRow]] =
    db.run(profileRecTable.filter(r => r.status === 21L && r.labCode === labCode).result)

  def getFailedProfilesReceivedDeleted(labCode: String): Future[Seq[ProfileReceivedRow]] =
    db.run(profileRecTable.filter(r => r.status === 19L && r.labCode === labCode).result)

  def getProfileReceivedStatusByGlobalCode(gc: SampleCode): Future[Option[Long]] =
    db.run(profileRecTable.filter(_.globalCode === gc.text).map(_.status).result.headOption)
      .recover { case _ => None }

  def getIsProfileReplicated(globalCode: SampleCode): Future[Boolean] =
    db.run(
      profileUplTable.filter(_.globalCode === globalCode.text).map(_.status).result.headOption.map {
        case Some(status) => status != 20L && status != 3L
        case None => false
      }
    )

  def getIsProfileReplicatedInternalCode(internalCode: String): Future[Boolean] =
    getGlobalCode(internalCode).flatMap {
      case Some(gc) => getIsProfileReplicated(gc)
      case None => Future.successful(false)
    }

  def getImmediateInferiorInstanceLabCode(globalCode: String): Future[String] =
    db.run(
      supApprTable.filter(_.globalCode === globalCode).map(_.laboratoryImmediateInstance).result.headOption.map(_.getOrElse(""))
    )

  def getProfileReceivedLabInferior(globalCode: String): Future[String] =
    db.run(profileRecTable.filter(_.globalCode === globalCode).map(_.labCode).result.headOption.map(_.getOrElse("")))

  def getProfileReceiveOperationOriginatedInInstance(globalCode: SampleCode): Future[Option[String]] =
    db.run(profileRecTable.filter(_.globalCode === globalCode.text).map(_.operationOriginatedInInstance).result.headOption)

  def countProfiles(): Future[Int] =
    db.run(pdTable.filter(_.deleted === false).length.result)
