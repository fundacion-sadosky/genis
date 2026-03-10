package profiledata

import javax.inject._
import java.io.File
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import types.SampleCode
import models.ProfileDataTables._

@Singleton
class ProfileDataRepositoryImpl @Inject()(implicit ec: ExecutionContext)
  extends ProfileDataRepository {

  private val db = Database.forConfig("slick.dbs.default.db")

  // --- mappers ---

  private def toProfileData(row: ProfileDataRow): ProfileData =
    ProfileData(
      category             = types.AlphanumericId(row.category),
      // TODO: globalCode sentinel — see ProfileData.pdAttempToPd comment.
      // This mapper is only called for rows that already exist in DB,
      // so globalCode is always valid here.
      globalCode           = SampleCode(row.globalCode),
      attorney             = row.attorney,
      bioMaterialType      = row.bioMaterialType,
      court                = row.court,
      crimeInvolved        = row.crimeInvolved,
      crimeType            = row.crimeType,
      criminalCase         = row.criminalCase,
      internalSampleCode   = row.internalSampleCode,
      assignee             = row.assignee,
      laboratory           = row.laboratory,
      deleted              = row.deleted,
      // deletedMotive is stored as flat strings in ProfileDataRow (solicitor + motive).
      // Legacy reconstructed DeletedMotive via a separate query (queryDeletePd).
      // For now we map to None; full reconstruction requires a join with PROFILE_DATA_MOTIVE.
      // TODO: implement deletedMotive reconstruction when delete flow is migrated.
      deletedMotive        = None,
      responsibleGeneticist = row.responsibleGeneticist,
      profileExpirationDate = row.profileExpirationDate,
      sampleDate           = row.sampleDate,
      sampleEntryDate      = row.sampleEntryDate,
      // dataFiliation requires a separate join with PROFILE_DATA_FILIATION.
      // Populated only in get(globalCode) and findByCodes; None in flat lookups.
      dataFiliation        = None,
      isExternal           = false
    )

  // --- implemented methods ---

  // Changed from legacy: returned Future[ProfileData] without Option.
  // Future[Option[ProfileData]] is the correct type for a nullable lookup.
  override def get(id: Long): Future[Option[ProfileData]] =
    db.run(profileData.filter(_.id === id).result.headOption)
      .map(_.map(toProfileData))

  override def get(globalCode: SampleCode): Future[Option[ProfileData]] = {
    val query = for {
      pd  <- profileData if pd.globalCode === globalCode.text
      pdf <- profileDataFiliation.filter(_.profileData === pd.globalCode).take(1)
    } yield (pd, pdf)

    val pdOnly = profileData.filter(_.globalCode === globalCode.text).result.headOption

    // Two-step: fetch profile row, then fetch filiation and resources separately.
    // Legacy did this with a left join + compiled query; here we keep it explicit
    // for clarity during migration.
    db.run(pdOnly).flatMap {
      case None => Future.successful(None)
      case Some(pdRow) =>
        val filiationQuery = profileDataFiliation
          .filter(_.profileData === globalCode.text)
          .result.headOption
        val inprintsQuery  = profileDataFiliationResources
          .filter(r => r.profileDataFiliation === globalCode.text && r.resourceType === "I")
          .map(_.id).result
        val picturesQuery  = profileDataFiliationResources
          .filter(r => r.profileDataFiliation === globalCode.text && r.resourceType === "P")
          .map(_.id).result
        val signaturesQuery = profileDataFiliationResources
          .filter(r => r.profileDataFiliation === globalCode.text && r.resourceType === "S")
          .map(_.id).result
        val externalQuery  = externalProfileData
          .filter(_.id === pdRow.id)
          .result.headOption

        for {
          filiationOpt <- db.run(filiationQuery)
          inprints     <- db.run(inprintsQuery)
          pictures     <- db.run(picturesQuery)
          signatures   <- db.run(signaturesQuery)
          externalOpt  <- db.run(externalQuery)
        } yield {
          val dataFiliation = filiationOpt.map { f =>
            DataFiliation(
              fullName                      = f.fullName,
              nickname                      = f.nickname,
              birthday                      = f.birthday,
              birthPlace                    = f.birthPlace,
              nationality                   = f.nationality,
              identification                = f.identification,
              identificationIssuingAuthority = f.identificationIssuingAuthority,
              address                       = f.address,
              inprints                      = inprints.toList,
              pictures                      = pictures.toList,
              signatures                    = signatures.toList
            )
          }
          Some(toProfileData(pdRow).copy(
            dataFiliation = dataFiliation,
            isExternal    = externalOpt.isDefined
          ))
        }
    }
  }

  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] = {
    val query = profileData
      .filter(_.globalCode === globalCode.text)
      .result.headOption
    db.run(query).map(_.map(toProfileData))
  }

  override def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]] =
    db.run(
      profileDataFiliationResources
        .filter(r => r.id === id && r.resourceType === resourceType)
        .map(_.resource)
        .result.headOption
    )

  override def isDeleted(globalCode: SampleCode): Future[Option[Boolean]] =
    db.run(
      profileData
        .filter(_.globalCode === globalCode.text)
        .map(_.deleted)
        .result.headOption
    )

  override def isDesktopProfile(globalCode: SampleCode): Future[Option[Boolean]] =
    db.run(
      profileData
        .filter(_.globalCode === globalCode.text)
        .map(_.fromDesktopSearch)
        .result.headOption
    )

  override def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]] =
    db.run(
      profileData
        .filter(pd => pd.internalSampleCode === internalSampleCode && !pd.deleted)
        .map(_.globalCode)
        .result.headOption
    ).map(_.map(SampleCode(_)))

  override def getDesktopProfiles(): Future[Seq[SampleCode]] =
    db.run(
      profileData.filter(_.fromDesktopSearch === true).map(_.globalCode).result
    ).map(_.map(SampleCode(_)))

  override def getDeletedMotive(globalCode: SampleCode): Future[Option[DeletedMotive]] =
    db.run(
      profileData
        .filter(_.globalCode === globalCode.text)
        .map(pd => (pd.deletedSolicitor, pd.deletedMotive))
        .result.headOption
    ).map {
      case Some((Some(solicitor), Some(motive))) => Some(DeletedMotive(solicitor, motive))
      case _                                     => None
    }

  override def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]] =
    db.run(
      profileUploaded
        .filter(_.globalCode === globalCode.text)
        .map(_.status)
        .result.headOption
    )

  override def getProfileReceivedStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]] =
    db.run(
      profileReceived
        .filter(_.globalCode === globalCode.text)
        .map(_.status)
        .result.headOption
    )

  override def getExternalProfileDataByGlobalCode(gc: String): Future[Option[ExternalProfileDataRow]] =
    db.run(
      externalProfileData
        .join(profileData).on(_.id === _.id)
        .filter(_._2.globalCode === gc)
        .map(_._1)
        .result.headOption
    )

  override def gefFailedProfilesUploaded(): Future[List[ProfileUploadedRow]] =
    db.run(profileUploaded.filter(_.status === 1L).result).map(_.toList)

  override def gefFailedProfilesUploadedDeleted(): Future[List[ProfileUploadedRow]] =
    db.run(profileUploaded.filter(_.status === 5L).result).map(_.toList)

  override def gefFailedProfilesSentDeleted(labCode: String): Future[List[ProfileSentRow]] =
    db.run(
      profileSent.filter(ps => ps.status === 5L && ps.labCode === labCode).result
    ).map(_.toList)

  override def getPendingApprovalNotification(labCode: String): Future[Seq[ProfileReceivedRow]] =
    db.run(profileReceived.filter(pr => pr.status === 22L && pr.labCode === labCode).result)

  override def getPendingRejectionNotification(labCode: String): Future[Seq[ProfileReceivedRow]] =
    db.run(profileReceived.filter(pr => pr.status === 21L && pr.labCode === labCode).result)

  override def getFailedProfilesReceivedDeleted(labCode: String): Future[Seq[ProfileReceivedRow]] =
    db.run(profileReceived.filter(pr => pr.status === 19L && pr.labCode === labCode).result)

  override def findUploadedProfilesByCodes(globalCodes: Seq[SampleCode]): Future[Seq[SampleCode]] = {
    val codes = globalCodes.map(_.text)
    db.run(profileUploaded.filter(_.globalCode inSet codes).map(_.globalCode).result)
      .map(_.map(SampleCode(_)))
  }

  override def getIsProfileReplicated(globalCode: SampleCode): Boolean = {
    // TODO: this method is synchronous in the legacy — it uses DB.withSession directly.
    // Keeping synchronous signature to match the trait, but this blocks the calling thread.
    // Should be refactored to Future[Boolean] once the service layer is migrated.
    import scala.concurrent.Await
    import scala.concurrent.duration._
    val query = profileUploaded
      .filter(_.globalCode === globalCode.text)
      .map(_.status)
      .result.headOption
    Await.result(db.run(query), 3.seconds) match {
      case Some(20L) | Some(3L) => false
      case Some(_)              => true
      case None                 => false
    }
  }

  override def getIsProfileReplicatedInternalCode(internalCode: String): Boolean = {
    // TODO: same blocking concern as getIsProfileReplicated above.
    import scala.concurrent.Await
    import scala.concurrent.duration._
    val result = getGlobalCode(internalCode).flatMap {
      case Some(gc) => Future.successful(getIsProfileReplicated(gc))
      case None     => Future.successful(false)
    }
    Await.result(result, 3.seconds)
  }

  // --- not implemented ---

  override def giveGlobalCode(labCode: String): Future[String] =
    Future.failed(new NotImplementedError("giveGlobalCode: requires Laboratory table query"))

  override def add(
    profileData: ProfileData,
    completeLabCode: String,
    imageList: Option[List[File]] = None,
    picturesList: Option[List[File]] = None,
    signaturesList: Option[List[File]] = None
  ): Future[SampleCode] =
    Future.failed(new NotImplementedError("add: requires DB sequence nextval and transactional insert"))

  override def updateProfileData(
    globalCode: SampleCode,
    newProfile: ProfileData,
    imageList: Option[List[File]] = None,
    picturesList: Option[List[File]] = None,
    signaturesList: Option[List[File]] = None
  ): Future[Boolean] =
    Future.failed(new NotImplementedError("updateProfileData: requires transactional update"))

  override def delete(globalCode: SampleCode, motive: DeletedMotive): Future[Int] =
    Future.failed(new NotImplementedError("delete: requires transactional update + ProfileDataMotive insert"))

  override def removeProfile(globalCode: SampleCode): Future[Either[String, SampleCode]] =
    Future.failed(new NotImplementedError("removeProfile"))

  override def getProfilesByUser(search: ProfileDataSearch): Future[Seq[ProfileDataFull]] =
    Future.failed(new NotImplementedError("getProfilesByUser: requires dynamic query composition"))

  override def getTotalProfilesByUser(search: ProfileDataSearch): Future[Int] =
    Future.failed(new NotImplementedError("getTotalProfilesByUser"))

  override def getTotalProfilesByUser(userId: String, isSuperUser: Boolean, category: String): Future[Int] =
    Future.failed(new NotImplementedError("getTotalProfilesByUser"))

  override def findByCodes(globalCodes: List[SampleCode]): Future[Seq[ProfileData]] =
    Future.failed(new NotImplementedError("findByCodes"))

  override def addExternalProfile(
    profileData: ProfileData,
    labOrigin: String,
    labImmediate: String
  ): Future[SampleCode] =
    Future.failed(new NotImplementedError("addExternalProfile"))

  override def updateUploadStatus(
    globalCode: String,
    status: Long,
    motive: Option[String],
    interconnectionError: Option[String],
    userName: Option[String]
  ): Future[Either[String, Unit]] =
    Future.failed(new NotImplementedError("updateUploadStatus"))

  override def updateProfileSentStatus(
    globalCode: String,
    status: Long,
    motive: Option[String],
    labCode: String,
    interconnectionError: Option[String]
  ): Future[Either[String, Unit]] =
    Future.failed(new NotImplementedError("updateProfileSentStatus"))

  override def addProfileReceivedApproved(
    labCode: String,
    globalCode: String,
    status: Long,
    userName: String,
    isCategoryModification: Boolean
  ): Future[Either[String, Unit]] =
    Future.failed(new NotImplementedError("addProfileReceivedApproved"))

  override def addProfileReceivedRejected(
    labCode: String,
    globalCode: String,
    status: Long,
    motive: String,
    userName: String,
    isCategoryModification: Boolean
  ): Future[Either[String, Unit]] =
    Future.failed(new NotImplementedError("addProfileReceivedRejected"))

  override def updateProfileReceivedStatus(
    labCode: String,
    globalCode: String,
    status: Long,
    motive: Option[String],
    isCategoryModification: Boolean,
    interconnectionError: Option[String],
    userName: Option[String]
  ): Future[Either[String, Unit]] =
    Future.failed(new NotImplementedError("updateProfileReceivedStatus"))

  override def updateInterconnectionError(
    globalCode: String,
    status: Long,
    interconnectionError: String
  ): Future[Either[String, Unit]] =
    Future.failed(new NotImplementedError("updateInterconnectionError"))
}