package profiledata

import profile.MtRCRS
import types.{AlphanumericId, SampleCode}
import types.AlphanumericId
import play.api.libs.json.*
import slick.jdbc.JdbcBackend.Database
import models.Tables

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class ProfileDataSearch(
  userId: String,
  isSuperUser: Boolean,
  page: Int = 0,
  pageSize: Int = 30,
  input: String = "",
  active: Boolean = true,
  inactive: Boolean = false,
  notUploaded: Option[Boolean] = None,
  category: String = ""
)
object ProfileDataSearch:
  implicit val format: Format[ProfileDataSearch] = Json.format

// View types used by PedigreeService
case class ProfileDataView(
  globalCode: SampleCode,
  category: AlphanumericId,
  internalSampleCode: String,
  assignee: String,
  associated: Boolean
)

object ProfileDataView:
  implicit val format: OFormat[ProfileDataView] = Json.format[ProfileDataView]

case class ProfileDataWithBatch(
  globalCode: SampleCode,
  category: AlphanumericId,
  internalSampleCode: String,
  assignee: String,
  idBatch: Option[Long]
)

case class DeletedMotive(userId: String, motive: String, motiveId: Int)

object DeletedMotive:
  implicit val format: OFormat[DeletedMotive] = Json.format[DeletedMotive]

case class ProfileDataViewCoincidencia(
  globalCode: SampleCode,
  category: String,
  internalSampleCode: String,
  assignee: String,
  lastMatch: java.util.Date,
  hit: Int,
  pending: Int,
  descarte: Int
)

object ProfileDataViewCoincidencia:
  implicit val format: OFormat[ProfileDataViewCoincidencia] = Json.format[ProfileDataViewCoincidencia]

trait ProfileDataRepository {
  def findByCode(globalCode: SampleCode): Future[Option[ProfileData]]
  def isDeleted(globalCode: SampleCode): Future[Option[Boolean]]
  def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]]
  def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]]
  def get(globalCode: SampleCode): Future[Option[ProfileData]]
  def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]]
  def findByCodes(codes: List[SampleCode]): Future[List[ProfileData]]
  def isProfileReplicated(internalCode: String): Future[Boolean]
  def countProfiles(): Future[Int]
}

trait ProfileDataService {
  def getMtRcrs(): Future[MtRCRS]
  def deleteProfile(globalCode: SampleCode, motive: DeletedMotive, userId: String): Future[Either[String, Unit]]
  def findByCodes(globalCodes: List[SampleCode]): Future[List[ProfileData]]
  def findByCode(globalCode: SampleCode): Future[Option[ProfileData]]
  def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]]
  def isProfileReplicated(internalCode: String): Future[Boolean]
  def countProfiles(): Future[Int]
}

@javax.inject.Singleton
class ProfileDataRepositoryImpl @Inject()(db: Database)(using ec: ExecutionContext) extends ProfileDataRepository:
  import slick.jdbc.PostgresProfile.api.*
  import Tables.*

  override def findByCodes(codes: List[SampleCode]): Future[List[ProfileData]] =
    val codeStrings = codes.map(_.text).toSet
    db.run(
      profilesData
        .filter(pd => pd.globalCode.inSet(codeStrings) && !pd.deleted)
        .result
    ).map(_.map(rowToProfileData).toList)

  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] =
    db.run(profilesData.filter(pd => pd.globalCode === globalCode.text && !pd.deleted).result.headOption)
      .map(_.map(rowToProfileData))

  override def get(globalCode: SampleCode): Future[Option[ProfileData]] = findByCode(globalCode)

  override def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]] =
    db.run(profilesData.filter(_.internalSampleCode === internalSampleCode).map(_.globalCode).result.headOption)
      .map(_.map(SampleCode(_)))

  override def isDeleted(globalCode: SampleCode): Future[Option[Boolean]] =
    db.run(profilesData.filter(_.globalCode === globalCode.text).map(_.deleted).result.headOption)

  override def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]] =
    Future.successful(None)

  override def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]] =
    Future.successful(List.empty)

  // Checks APP.PROFILE_UPLOADED: status 20=deleted in superior, 3=rejected → not replicated
  override def isProfileReplicated(internalCode: String): Future[Boolean] =
    db.run(
      profilesData
        .filter(pd => pd.internalSampleCode === internalCode && !pd.deleted)
        .map(_.globalCode)
        .result.headOption
    ).flatMap {
      case None => Future.successful(false)
      case Some(gc) =>
        db.run(
          sql"""SELECT "STATUS" FROM "APP"."PROFILE_UPLOADED" WHERE "GLOBAL_CODE" = $gc"""
            .as[Long].headOption
        ).map {
          case None         => false
          case Some(20L)    => false
          case Some(3L)     => false
          case _            => true
        }
    }

  override def countProfiles(): Future[Int] =
    db.run(profilesData.filter(!_.deleted).length.result)

  private def rowToProfileData(row: ProfileDataRow): ProfileData =
    ProfileData(
      category              = AlphanumericId(row.category),
      globalCode            = SampleCode(row.globalCode),
      attorney              = row.attorney,
      bioMaterialType       = row.bioMaterialType,
      court                 = row.court,
      crimeInvolved         = row.crimeInvolved,
      crimeType             = row.crimeType,
      criminalCase          = row.criminalCase,
      internalSampleCode    = row.internalSampleCode,
      assignee              = row.assignee,
      laboratory            = row.laboratory,
      deleted               = row.deleted,
      responsibleGeneticist = row.responsibleGeneticist,
      profileExpirationDate = row.profileExpirationDate.map(d => new java.util.Date(d.getTime)),
      sampleDate            = row.sampleDate.map(d => new java.util.Date(d.getTime)),
      sampleEntryDate       = row.sampleEntryDate.map(d => new java.util.Date(d.getTime)),
      isExternal            = false
    )

@javax.inject.Singleton
class ProfileDataServiceImpl @Inject()(repo: ProfileDataRepository)(using ec: ExecutionContext) extends ProfileDataService:
  override def getMtRcrs(): Future[MtRCRS] = Future.successful(MtRCRS(Map.empty))
  override def deleteProfile(globalCode: SampleCode, motive: DeletedMotive, userId: String): Future[Either[String, Unit]] =
    Future.successful(Right(()))
  override def findByCodes(globalCodes: List[SampleCode]): Future[List[ProfileData]] = repo.findByCodes(globalCodes)
  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] = repo.findByCode(globalCode)
  override def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]] = repo.getGlobalCode(internalSampleCode)
  override def isProfileReplicated(internalCode: String): Future[Boolean] = repo.isProfileReplicated(internalCode)
  override def countProfiles(): Future[Int] = repo.countProfiles()

@javax.inject.Singleton
class ProfileDataRepositoryStub extends ProfileDataRepository {
  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def isDeleted(globalCode: SampleCode): Future[Option[Boolean]] = Future.successful(None)
  override def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]] = Future.successful(None)
  override def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]] = Future.successful(List.empty)
  override def get(globalCode: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]] = Future.successful(None)
  override def findByCodes(codes: List[SampleCode]): Future[List[ProfileData]] = Future.successful(List.empty)
  override def isProfileReplicated(internalCode: String): Future[Boolean] = Future.successful(false)
  override def countProfiles(): Future[Int] = Future.successful(0)
}

@javax.inject.Singleton
class ProfileDataServiceStub extends ProfileDataService {
  override def getMtRcrs(): Future[MtRCRS] = Future.successful(MtRCRS(Map.empty))
  override def deleteProfile(globalCode: SampleCode, motive: DeletedMotive, userId: String): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def findByCodes(globalCodes: List[SampleCode]): Future[List[ProfileData]] = Future.successful(List.empty)
  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]] = Future.successful(None)
  override def isProfileReplicated(internalCode: String): Future[Boolean] = Future.successful(false)
  override def countProfiles(): Future[Int] = Future.successful(0)
}
