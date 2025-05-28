package profiledata

import models.Tables.ProfileUploadedRow
import models.Tables.ProfileSentRow
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.{Calendar, Date}
import scala.concurrent.Future
import scala.language.postfixOps
import scala.slick.driver.PostgresDriver.simple.queryToUpdateInvoker
import scala.slick.driver.PostgresDriver.simple.runnableCompiledToUpdateInvoker
import scala.slick.jdbc.{StaticQuery => Q}
import scala.slick.jdbc.StaticQuery.staticQueryToInvoker
import configdata.Category
import configdata.Group
import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.rowset.serial.SerialBlob
import profile.MtRCRS
import models.Tables
import models.Tables.ProfileDataFiliationResourcesRow
import models.Tables.ProfileDataFiliationRow
import models.Tables.ProfileDataRow
import play.api.{Application, Logger}
import play.api.db.slick.Config.driver.simple.Column
import play.api.db.slick.Config.driver.simple.Compiled
import play.api.db.slick.Config.driver.simple.TableQuery
import play.api.db.slick.Config.driver.simple.booleanColumnExtensionMethods
import play.api.db.slick.Config.driver.simple.booleanColumnType
import play.api.db.slick.Config.driver.simple.columnExtensionMethods
import play.api.db.slick.Config.driver.simple.longColumnType
import play.api.db.slick.Config.driver.simple.queryToAppliedQueryInvoker
import play.api.db.slick.Config.driver.simple.queryToInsertInvoker
import play.api.db.slick.Config.driver.simple.
  runnableCompiledToAppliedQueryInvoker
import play.api.db.slick.Config.driver.simple.slickDriver
import play.api.db.slick.Config.driver.simple.stringColumnType
import play.api.db.slick.Config.driver.simple.valueToConstColumn
import play.api.db.slick.DB
import play.api.db.slick.DBAction
import types.AlphanumericId
import types.SampleCode
import util.{DefaultDb, Transaction}
import play.api.i18n.Messages
import models.Tables.ExternalProfileDataRow
import play.api.db.slick.Config.driver.simple._

abstract class ProfileDataRepository extends DefaultDb with Transaction  {
  /**
   * Get by dataBase Id
   *
   */
  def get(id: Long): Future[ProfileData]
  /**
   * Get by globalCode only return the information in the table
   *
   */
  def findByCode(globalCode: SampleCode): Future[Option[ProfileData]]
  /**
   * Create a new ProfileData
   *
   */
  def add(
    profileData: ProfileData,
    completeLabCode: String,
    imageList: Option[List[File]] = None,
    picturesList: Option[List[File]] = None,
    signaturesList: Option[List[File]] = None
  ): Future[SampleCode]
  /**
   * Update a ProfileData
   *
   */
  def updateProfileData(
    globalCode: SampleCode,
    newProfile: ProfileData,
    imageList: Option[List[File]] = None,
    picturesList: Option[List[File]] = None,
    signaturesList: Option[List[File]] = None
  ): Future[Boolean]
  /**
   * Get a complete ProfileData with data filiation if is present
   *
   */
  def get(globalCode: SampleCode): Future[Option[ProfileData]]
  /**
   * Get a resource by type and id
   *
   */
  def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]]
  /**
   * Set a profile data with deleted flag in true
   *
   */
  def delete(globalCode: SampleCode, motive: DeletedMotive): Future[Int]

  /**
   * remove all elements from the table
   */
  
  //def removeAll(): Future[Int]
  
  /**
   * Get all profile datas of a user
   *
   */
  def getTotalProfilesByUser(search: ProfileDataSearch): Future[Int]
  def getTotalProfilesByUser(
    userId : String,
    isSuperUser : Boolean,
    category:String=""
  ): Future[Int]
  def getProfilesByUser(search: ProfileDataSearch): Future[Seq[ProfileDataFull]]
  def giveGlobalCode(labCode: String): Future[String]
  def isDeleted(globalCode: SampleCode): Future[Option[Boolean]]
  def getDeletedMotive(globalCode: SampleCode): Future[Option[DeletedMotive]]
  def findByCodes(globalCodes: List[SampleCode]): Future[Seq[ProfileData]]
  def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]]

  def addExternalProfile(
    profileData: ProfileData,
    labOrigin:String,
    labImmediate:String
  ): Future[SampleCode]
  def updateUploadStatus(
    globalCode: String,
    status:Long,
    motive:Option[String]=None
  ): Future[Either[String,Unit]]
  def findUploadedProfilesByCodes(
    globalCodes: Seq[SampleCode]
  ): Future[Seq[SampleCode]]

  def getProfileUploadStatusByGlobalCode(
    globalCode:SampleCode
  ):Future[Option[Long]]

  def getExternalProfileDataByGlobalCode(
    globalCode:String
  ):Future[Option[ExternalProfileDataRow]]
  def gefFailedProfilesUploaded():Future[List[ProfileUploadedRow]]
  def gefFailedProfilesUploadedDeleted():Future[List[ProfileUploadedRow]]
  def gefFailedProfilesSentDeleted(labCode:String):Future[List[ProfileSentRow]]
  def updateProfileSentStatus(
    globalCode: String,
    status:Long,
    motive:Option[String]=None,
    labCode:String
  ): Future[Either[String,Unit]]
  def getMtRcrs():Future[MtRCRS]
}

@Singleton
class SlickProfileDataRepository @Inject() (
  implicit app: Application
) extends ProfileDataRepository {
  val logger: Logger = Logger(this.getClass())

  val profilesData: TableQuery[Tables.ProfileData] =
    Tables.ProfileData // Tables.ProtoProfileData
  val externalProfileDataTable: TableQuery[Tables.ExternalProfileData] =
    Tables.ExternalProfileData // Tables.ExternalProfileData
  val profileUploaded: TableQuery[Tables.ProfileUploaded] =
    Tables.ProfileUploaded
  val profileSent: TableQuery[Tables.ProfileSent] =
    Tables.ProfileSent
  val mitochondrialRcrs: TableQuery[Tables.MitochondrialRcrs] =
    Tables.MitochondrialRcrs

  val profileMetaDataFiliations: TableQuery[Tables.ProfileDataFiliation] =
    Tables.ProfileDataFiliation // Tables.ProtoProfileDataFiliation
  val profileMetaDataFiliationResources
    : TableQuery[Tables.ProfileDataFiliationResources] =
    Tables.ProfileDataFiliationResources
    // Tables.ProtoProfileDataFiliationResources
  val profileDataMotive: TableQuery[Tables.ProfileDataMotive] =
    Tables.ProfileDataMotive
  val groups: TableQuery[Tables.Group] = Tables.Group
  val categories: TableQuery[Tables.Category] = Tables.Category
  val resources: TableQuery[Tables.ProfileDataFiliationResources] =
    Tables.ProfileDataFiliationResources

  val laboratories: TableQuery[Tables.Laboratory] = Tables.Laboratory
  val profilesDataMotive: TableQuery[Tables.ProfileDataMotive] =
    Tables.ProfileDataMotive

  val queryGetGlobalCode = Compiled {
    internalSampleCode: Column[String] =>
      for{
        pd <- profilesData if
          pd.internalSampleCode === internalSampleCode && !pd.deleted
      }
        yield (pd.globalCode)
  }
  val getByGlobalCode = Compiled(queryGetByGlobalCode _)
  val getProfileUploadedById = Compiled(queryProfileUploadedById _)
  val getProfileUploadedByGlobalCode =
    Compiled(queryProfileUploadedByGlobalCode _)
  val getExternalProfileDataByGlobalCodeCompiled =
    Compiled(queryGetExternalProfileDataByGlobalCode _)

  private def queryGetByGlobalCode(globalCode: Column[String]) =
    profilesData.filter(_.globalCode === globalCode)
  private def queryProfileUploadedById(id: Column[Long]) =
    profileUploaded.filter(_.id === id)
  private def queryProfileUploadedByGlobalCode(globalCode: Column[String]) =
    profileUploaded.filter(_.globalCode === globalCode)
  private def queryProfileUploadedByGlobalCodeInSet(globalCodes: Seq[String]) =
    profileUploaded.filter(_.globalCode inSet globalCodes)
  private def queryFailedUploadedProfiles() =
    profileUploaded.filter(_.status === 1L)
  private def queryFailedUploadedProfilesDeleted() =
    profileUploaded.filter(_.status === 5L)
  private def queryFailedSentProfilesDeleted(labCode: Column[String]) =
    profileSent.filter(_.status === 5L).filter(_.labCode === labCode)

  private def queryGetExternalProfileDataByGlobalCode(
    globalCode: Column[String]
  ) =
    externalProfileDataTable
      .innerJoin(profilesData)
      .on(_.id === _.id)
      .filter(_._2.globalCode === globalCode)
      .map(x => x._1)

  private def queryDefineDeletePd(globalCode: Column[String]) = for {
    pd <- profilesData if pd.globalCode === globalCode
  } yield (pd.deleted, pd.deletedSolicitor, pd.deletedMotive)

  private def queryUpdatePd(globalCode: Column[String]) = for {
    pd <- profilesData if pd.globalCode === globalCode
  } yield (
    pd.category,
    pd.attorney,
    pd.bioMaterialType,
    pd.court,
    pd.crimeInvolved,
    pd.crimeType,
    pd.criminalCase,
    pd.responsibleGeneticist,
    pd.profileExpirationDate,
    pd.sampleDate,
    pd.sampleEntryDate
  )

  protected def queryUpdateProtoPd(globalCode: Column[String]) = for {
    pd <- profilesData if pd.globalCode === globalCode
  } yield (
    pd.attorney,
    pd.bioMaterialType,
    pd.court,
    pd.crimeInvolved,
    pd.crimeType,
    pd.criminalCase,
    pd.responsibleGeneticist,
    pd.profileExpirationDate,
    pd.sampleDate,
    pd.sampleEntryDate,
    pd.laboratory
  )

  protected def queryUpdatePdFiliation(sampleCode: Column[String]) = for {
    pdf <- profileMetaDataFiliations if (pdf.profileData === sampleCode)
  } yield (
    pdf.fullName,
    pdf.nickname,
    pdf.birthday,
    pdf.birthPlace,
    pdf.nationality,
    pdf.identification,
    pdf.identificationIssuingAuthority,
    pdf.address
  )

  //private def queryRemoveAll() = profilesData.filter(_.deleted === true).delete

  private def queryDefineGetIdProfileData(globalCode: Column[String]) = for {
    pd <- profilesData if (pd.globalCode === globalCode)
  } yield (pd.id)

  val queryGetIdProfileData = Compiled(queryDefineGetIdProfileData _)


//  private def queryDefineGetAllProfileGlobalCodes = for {
//    pd <- profilesData
//  } yield pd.globalCode
//
//  val queryGetAllProfileGlobalCodes = Compiled(queryDefineGetAllProfileGlobalCodes)

  private def queryDefineGetProfileData(id: Column[Long]) = for (
    ((pd, pmdf),epd) <-
      profilesData
        leftJoin profileMetaDataFiliations
        on (_.globalCode === _.profileData)
        leftJoin externalProfileDataTable
        on (_._1.id === _.id)
        if pd.id === id
  ) yield (pd, pmdf.?,epd.?)

  private def queryDefineResource(id: Column[String]) = for (
    resource <- profileMetaDataFiliationResources
      if resource.profileDataFiliation === id
  ) yield (resource.resourceType, resource.id)

  private def queryDefineGetResource(
    resourceType: Column[String],
    id: Column[Long]
  ) = for {
    resource <- profileMetaDataFiliationResources
      if (resource.id === id && resource.resourceType === resourceType)
  } yield (resource.resource)

  val queryDeletePd = Compiled(queryDefineDeletePd _)

  val queryGetResource = Compiled(queryDefineGetResource _)

  val queryGetProfileData = Compiled(queryDefineGetProfileData _)

  val queryResource = Compiled(queryDefineResource _)

  val getFailedUploadedProfiles = Compiled(queryFailedUploadedProfiles)
  val getFailedUploadedProfilesDeleted =
    Compiled(queryFailedUploadedProfilesDeleted)
  val getFailedSentProfilesDeleted = Compiled(queryFailedSentProfilesDeleted _)


  private def queryDefineGetLabByCode(code: Column[String]) = for (
    lab <- laboratories if lab.codeName === code
  ) yield lab

  val queryGetLabByCode = Compiled(queryDefineGetLabByCode _)

  private def queryDefineGetProfileDataBySampleCode(
    sampleCode: Column[String]
  ) = for (
    ((pd, pmdf), epd) <-
      profilesData
        leftJoin profileMetaDataFiliations
        on (_.globalCode === _.profileData)
        leftJoin externalProfileDataTable
        on (_._1.id === _.id)
        if pd.globalCode === sampleCode
  ) yield (pd, pmdf.?,epd.?)

  val queryGetProfileDataBySampleCode =
    Compiled(queryDefineGetProfileDataBySampleCode _)

  private def queryDefineFindByCode(code: Column[String]) =
    for (
      (pd, epd) <-
        profilesData
          leftJoin externalProfileDataTable
          on (_.id === _.id)
          if (pd.globalCode === code)
    ) yield (pd,epd.?)

  val queryFindProfileDataByCode = Compiled(queryDefineFindByCode _)

  private def queryDefineGetResources(
    id: Column[String],
    resourceType: Column[String]
  ) = for {
    resource <-
      profileMetaDataFiliationResources
    if resource.profileDataFiliation === id &&
      resource.resourceType === resourceType
  } yield resource.id

  val queryGetResources = Compiled(queryDefineGetResources _)

  private def queryDefineGetProfileDataByUserAndStatus(
    userId: Column[String],
    isSuperUser: Column[Boolean],
    active: Column[Boolean],
    inactive: Column[Boolean]
  ) = (
    for {
      ((pd, pdu), epd) <- (
        profilesData
          leftJoin profileUploaded
          on (_.id === _.id)
          leftJoin externalProfileDataTable
          on (_._1.id === _.id)
        )
    if (isSuperUser || pd.assignee === userId) &&
      ((pd.deleted && inactive) || (!pd.deleted && active))

    } yield (pd,pdu.?,epd.?)
  ) sortBy(_._1.globalCode.desc)

  private def queryDefineGetProfileDataByUserAndStatusAndCategory(
    userId: Column[String],
    isSuperUser: Column[Boolean],
    active: Column[Boolean],
    inactive: Column[Boolean],
    category: Column[String]
  ) = (for {
    ((pd,pdu),epd) <- (
      profilesData
        leftJoin profileUploaded
        on(_.id === _.id)
        leftJoin externalProfileDataTable
        on(_._1.id === _.id)
      )
    if (
      (isSuperUser || pd.assignee === userId) &&
      ((pd.deleted && inactive) || (!pd.deleted && active))
      && (pd.category===category)
    )
  } yield (pd,pdu.?,epd.?)
  ) sortBy(_._1.globalCode.desc)

  private def queryDefineGetProfileDataByUser(
    userId: Column[String],
    isSuperUser: Column[Boolean]
  ) = (
    for {
    pd <- profilesData if (isSuperUser || pd.assignee === userId)
    } yield pd
  ) sortBy(_.globalCode.desc)

  private def queryDefineGetProfileDataByUserAndCategory(
    userId: Column[String],
    isSuperUser: Column[Boolean],
    category: Column[String]
  ) = (
    for {
      pd <- profilesData
        if (isSuperUser || pd.assignee === userId) && (pd.category===category)
    } yield (pd)
  ) sortBy(_.globalCode.desc)

  val queryGetProfileDataByUserAndStatus =
    Compiled(queryDefineGetProfileDataByUserAndStatus _)

  val queryGetProfileDataByUserAndStatusAndCategory =
    Compiled(queryDefineGetProfileDataByUserAndStatusAndCategory _)

  val queryGetProfileDataByUser =
    Compiled(queryDefineGetProfileDataByUser _)

  val queryGetProfileDataByUserAndCategory =
    Compiled(queryDefineGetProfileDataByUserAndCategory _)

  override def isDeleted(globalCode: SampleCode)
  : Future[Option[Boolean]] = Future {
    DB.withSession { implicit session =>
      queryDeletePd(globalCode.text).firstOption map { x => x._1 }
    }
  }

  override def getProfilesByUser(search: ProfileDataSearch)
  : Future[Seq[ProfileDataFull]] = Future {
    if(search.notUploaded.contains(true)){
      if(search.category.isEmpty) {
        DB.withSession {
          implicit session =>
            queryGetProfileDataByUserAndStatus(
              search.userId,
              search.isSuperUser,
              search.active,
              search.inactive
            )
            .list
              .filter(_._2.isEmpty)
              .drop(search.page * search.pageSize)
              .take(search.pageSize)
              .iterator
              .toVector map {
                case (pd,pdu,epd) =>
                  ProfileDataFull(
                    AlphanumericId(pd.category),
                    SampleCode(pd.globalCode),
                    pd.attorney,
                    pd.bioMaterialType,
                    pd.court,
                    pd.crimeInvolved,
                    pd.crimeType,
                    pd.criminalCase,
                    pd.internalSampleCode,
                    pd.assignee,
                    pd.laboratory,
                    pd.deleted,
                    None,
                    pd.responsibleGeneticist,
                    pd.profileExpirationDate,
                    pd.sampleDate,
                    pd.sampleEntryDate,
                    None,
                    pdu.isDefined,
                    epd.isDefined
                  )
              }
          }
      } else {
        DB.withSession { implicit session =>
          queryGetProfileDataByUserAndStatusAndCategory(
            search.userId,
            search.isSuperUser,
            search.active,
            search.inactive,
            search.category
          )
          .list
            .filter(_._2.isEmpty)
            .drop(search.page * search.pageSize)
            .take(search.pageSize)
            .iterator
            .toVector map {
              case (pd,pdu,epd) =>
                ProfileDataFull(
                  AlphanumericId(pd.category),
                  SampleCode(pd.globalCode),
                  pd.attorney,
                  pd.bioMaterialType,
                  pd.court,
                  pd.crimeInvolved,
                  pd.crimeType,
                  pd.criminalCase,
                  pd.internalSampleCode,
                  pd.assignee,
                  pd.laboratory,
                  pd.deleted,
                  None,
                  pd.responsibleGeneticist,
                  pd.profileExpirationDate,
                  pd.sampleDate,
                  pd.sampleEntryDate,
                  None,
                  pdu.isDefined,
                  epd.isDefined
                )
            }
          }
      }
    } else {
      if (search.category.isEmpty) {
        DB.withSession {
          implicit session =>
            queryGetProfileDataByUserAndStatus(
              search.userId,
              search.isSuperUser,
              search.active,
              search.inactive
            )
            .list
              .drop(search.page * search.pageSize)
              .take(search.pageSize)
              .iterator
              .toVector map {
                case (pd,pdu,epd) =>
                  ProfileDataFull(
                    AlphanumericId(pd.category),
                    SampleCode(pd.globalCode),
                    pd.attorney,
                    pd.bioMaterialType,
                    pd.court,
                    pd.crimeInvolved,
                    pd.crimeType,
                    pd.criminalCase,
                    pd.internalSampleCode,
                    pd.assignee,
                    pd.laboratory,
                    pd.deleted,
                    None,
                    pd.responsibleGeneticist,
                    pd.profileExpirationDate,
                    pd.sampleDate,
                    pd.sampleEntryDate,
                    None,
                    pdu.isDefined,
                    epd.isDefined
                  )
              }
          }
    } else {
        DB.withSession { implicit session =>
          queryGetProfileDataByUserAndStatusAndCategory(search.userId, search.isSuperUser, search.active, search.inactive,search.category)
            .list.drop(search.page * search.pageSize).take(search.pageSize).iterator.toVector map {
            case (pd,pdu,epd) =>
              ProfileDataFull(AlphanumericId(pd.category),
                SampleCode(pd.globalCode), pd.attorney, pd.bioMaterialType,
                pd.court, pd.crimeInvolved, pd.crimeType, pd.criminalCase,
                pd.internalSampleCode, pd.assignee, pd.laboratory, pd.deleted, None,
                pd.responsibleGeneticist, pd.profileExpirationDate, pd.sampleDate,
                pd.sampleEntryDate, None,pdu.isDefined,epd.isDefined)
          }
        }
      }
    }
  }

  override def getTotalProfilesByUser(search: ProfileDataSearch): Future[Int] = Future {
    if(search.notUploaded.contains(true)){
      if(search.category.isEmpty) {
        DB.withSession { implicit session =>
          queryGetProfileDataByUserAndStatus(search.userId, search.isSuperUser, search.active, search.inactive).list.filter(_._2.isEmpty).length
        }
      }else{
        DB.withSession { implicit session =>
          queryGetProfileDataByUserAndStatusAndCategory(search.userId, search.isSuperUser, search.active, search.inactive, search.category).list.filter(_._2.isEmpty).length
        }
      }
    }else{
      if(search.category.isEmpty) {
        DB.withSession { implicit session =>
          queryGetProfileDataByUserAndStatus(search.userId, search.isSuperUser, search.active, search.inactive).list.length
        }
      }else{
        DB.withSession { implicit session =>
          queryGetProfileDataByUserAndStatusAndCategory(search.userId, search.isSuperUser, search.active, search.inactive, search.category).list.length
        }
      }
    }
  }

  override def getTotalProfilesByUser(userId : String, isSuperUser : Boolean, category: String=""): Future[Int] = Future {
    if (category.isEmpty){
      DB.withSession { implicit session =>
        queryGetProfileDataByUser(userId, isSuperUser).list.length
      }
    }else{
      DB.withSession { implicit session =>
        queryGetProfileDataByUserAndCategory(userId, isSuperUser, category).list.length
      }
    }
  }

  override def delete(globalCode: SampleCode, motive: DeletedMotive): Future[Int] = {
    DB.withTransaction { implicit session =>
      val resp = queryDeletePd(globalCode.text).update((true, Option(motive.solicitor), Option(motive.motive)))
      val idProfile = queryGetIdProfileData(globalCode.text).first
      val now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime)
      val id = (profileDataMotive returning profileDataMotive.map(_.id)) += models.Tables.ProfileDataMotiveRow(0L,idProfile,now,motive.selectedMotive)

      Future.successful(resp)
    }
  }

//  override def removeAll(): Future[Int] = {
//    DB.withTransaction { implicit session =>
//      val codes = queryGetAllProfileGlobalCodes.list
//      val counts: Seq[Int] = codes.map { code =>
//        queryGetIdProfileData(code).delete
//      }
//
//      val totalDeleted = counts.sum
//      Future.successful(totalDeleted)
//    }
//  }
//

  override def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]] = Future {
    DB.withTransaction { implicit session =>
      queryGetResource((resourceType, id)).firstOption map { blob =>
        val size = blob.length().toInt
        blob.getBytes(1, size)
      }
    }
  }

  override def get(globalCode: SampleCode): Future[Option[ProfileData]] = Future {
    DB.withSession { implicit session =>
      queryGetProfileDataBySampleCode(globalCode.text).firstOption map {
        case (profile, dataFiliation, epd) =>
          val posibleDataFiliation = dataFiliation map { df =>

            val inprints = queryGetResources((globalCode.text, "I")).list
            val pictures = queryGetResources((globalCode.text, "P")).list
            val signatures = queryGetResources((globalCode.text, "S")).list

            DataFiliation(df.fullName, df.nickname, df.birthday, df.birthPlace, df.nationality, df.identification, df.identificationIssuingAuthority, df.address, inprints, pictures, signatures)
          }
          ProfileData(AlphanumericId(profile.category),
            SampleCode(profile.globalCode), profile.attorney, profile.bioMaterialType,
            profile.court, profile.crimeInvolved, profile.crimeType, profile.criminalCase,
            profile.internalSampleCode, profile.assignee, profile.laboratory, profile.deleted, None,
            profile.responsibleGeneticist, profile.profileExpirationDate, profile.sampleDate,
            profile.sampleEntryDate, posibleDataFiliation,epd.isDefined)
      }
    }
  }

  override def get(id: Long): Future[ProfileData] = Future {
    DB.withTransaction { implicit session =>

      val (pd, pmdf, epd) = queryGetProfileData(id).first

      val filiationData = pmdf.map(pmdf => {

        val resources = queryResource(pmdf.profileData)
          .list
          .groupBy(row => row._1)
        val inprints = resources.get("I").getOrElse(Nil).map(tup => tup._2)
        val pictures = resources.get("P").getOrElse(Nil).map(tup => tup._2)
        val signatures = resources.get("S").getOrElse(Nil).map(tup => tup._2)

        DataFiliation(pmdf.fullName,
          pmdf.nickname,
          pmdf.birthday,
          pmdf.birthPlace,
          pmdf.nationality,
          pmdf.identification,
          pmdf.identificationIssuingAuthority,
          pmdf.address,
          inprints,
          pictures,
          signatures)
      })

      ProfileData(AlphanumericId(pd.category),
        SampleCode(pd.globalCode), pd.attorney, pd.bioMaterialType,
        pd.court, pd.crimeInvolved, pd.crimeType, pd.criminalCase,
        pd.internalSampleCode, pd.assignee, pd.laboratory, pd.deleted, None,
        pd.responsibleGeneticist, pd.profileExpirationDate, pd.sampleDate,
        pd.sampleEntryDate, filiationData,epd.isDefined)
    }
  }

  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] = Future {
    DB.withSession { implicit session =>

      queryFindProfileDataByCode(globalCode.text).firstOption map { case (prof,epd) =>
        val sc = SampleCode(prof.globalCode)
        ProfileData(AlphanumericId(prof.category), sc,
          prof.attorney, prof.bioMaterialType, prof.court, prof.crimeInvolved, prof.crimeType, prof.criminalCase, prof.internalSampleCode, prof.assignee,
          prof.laboratory, prof.deleted, None,
          prof.responsibleGeneticist, prof.profileExpirationDate, prof.sampleDate,
          prof.sampleEntryDate, None,epd.isDefined)
      }
    }
  }

  override def giveGlobalCode(labCode: String): Future[String] = DB.withSession { implicit session =>
    val lab = queryGetLabByCode(labCode).firstOption.get
    val s = lab.country + "-" + lab.province + "-" + lab.codeName
    Future.successful(s)
  }
  def addExternalProfile(profileData: ProfileData,labOrigin:String,labImmediate:String): Future[SampleCode] = Future {

    DB.withTransaction { implicit session =>

      val globalCode = profileData.globalCode.text
      try{

      val posibleSampleDate = profileData.sampleDate map { sd => new java.sql.Date(sd.getTime) }
      val posibleExpirationDate = profileData.profileExpirationDate map { ed => new java.sql.Date(ed.getTime) }
      val posibleEntryDate = profileData.sampleEntryDate map { end => new java.sql.Date(end.getTime) }

      val profileDataRow = new ProfileDataRow(0, profileData.category.text, globalCode, profileData.internalSampleCode,
        None, profileData.attorney, profileData.bioMaterialType, profileData.court, profileData.crimeInvolved,
        profileData.crimeType, profileData.criminalCase, profileData.internalSampleCode, profileData.assignee, profileData.laboratory,
        posibleExpirationDate, profileData.responsibleGeneticist,
        posibleSampleDate, posibleEntryDate)

      val id = profilesData returning profilesData.map(_.id) += profileDataRow
      val externalProfileDataRow = Tables.ExternalProfileDataRow(id,labOrigin,labImmediate)
      externalProfileDataTable += externalProfileDataRow

      } catch {
        case e: Exception => {
          logger.error("error addExternalProfile",e);
          throw e;
        }
      }
      SampleCode(globalCode)
    }
  }
  override def add(profileData: ProfileData, completeLabCode: String, imageList: Option[List[File]] = None, pictureList: Option[List[File]] = None, signaturesList: Option[List[File]] = None): Future[SampleCode] = Future {

    DB.withTransaction { implicit session =>
      val nextVal: Long = Q.queryNA[Long]("select nextval('\"APP\".\"PROFILE_DATA_GLOBAL_CODE_seq\"')").first
      val globalCode = completeLabCode + "-" + nextVal

      val posibleSampleDate = profileData.sampleDate map { sd => new java.sql.Date(sd.getTime) }
      val posibleExpirationDate = profileData.profileExpirationDate map { ed => new java.sql.Date(ed.getTime) }
      val posibleEntryDate = profileData.sampleEntryDate map { end => new java.sql.Date(end.getTime) }

      val profileDataRow = new ProfileDataRow(0, profileData.category.text, globalCode, profileData.internalSampleCode,
        None, profileData.attorney, profileData.bioMaterialType, profileData.court, profileData.crimeInvolved,
        profileData.crimeType, profileData.criminalCase, profileData.internalSampleCode, profileData.assignee, profileData.laboratory,
        posibleExpirationDate, profileData.responsibleGeneticist,
        posibleSampleDate, posibleEntryDate)
      profilesData += profileDataRow

      profileData.dataFiliation.map { filiationData =>

        val profileMDF = new ProfileDataFiliationRow(0, globalCode, filiationData.fullName, filiationData.nickname,
          filiationData.birthday.map { x => new java.sql.Date(x.getTime) }
          , filiationData.birthPlace, filiationData.nationality,
          filiationData.identification, filiationData.identificationIssuingAuthority, filiationData.address)
        profileMetaDataFiliations += profileMDF

        imageList.map { imageListFile =>

          imageListFile.foreach(file => {
            val is: InputStream = new FileInputStream(file)
            val byteArray = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
            val blob = new SerialBlob(byteArray)
            val resourceRow = ProfileDataFiliationResourcesRow(0, globalCode, blob, "I")
            profileMetaDataFiliationResources += resourceRow
          })
        }

        pictureList.map { picturesListFile =>

          picturesListFile.foreach(file => {
            val is: InputStream = new FileInputStream(file)
            val byteArray = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
            val blob = new SerialBlob(byteArray)
            val resourceRow = ProfileDataFiliationResourcesRow(0, globalCode, blob, "P")
            profileMetaDataFiliationResources += resourceRow
          })
        }

        signaturesList.map { signaturesListFile =>

          signaturesListFile.foreach(file => {
            val is: InputStream = new FileInputStream(file)
            val byteArray = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
            val blob = new SerialBlob(byteArray)
            val resourceRow = ProfileDataFiliationResourcesRow(0, globalCode, blob, "S")
            profileMetaDataFiliationResources += resourceRow
          })
        }

        Option(globalCode)
      }.getOrElse {
        None
      }
      SampleCode(globalCode)
    }
  }

  override def updateProfileData(
    globalCode: SampleCode,
    newProfile: ProfileData,
    imageList: Option[List[File]] = None,
    pictureList: Option[List[File]] = None,
    signatureList: Option[List[File]] = None
  ): Future[Boolean] = Future {

    DB.withTransaction {
      implicit session =>
        val posibleExpirationDate = newProfile
          .profileExpirationDate map { ed => new java.sql.Date(ed.getTime()) }
        val posiblesampleDate = newProfile
          .sampleDate map { sd => new java.sql.Date(sd.getTime()) }
        val posiblesampleEntryDate = newProfile
          .sampleEntryDate map { sed => new java.sql.Date(sed.getTime()) }
        val pd = (
          newProfile.category.text,
          newProfile.attorney,
          newProfile.bioMaterialType,
          newProfile.court,
          newProfile.crimeInvolved,
          newProfile.crimeType,
          newProfile.criminalCase,
          newProfile.responsibleGeneticist,
          posibleExpirationDate,
          posiblesampleDate,
          posiblesampleEntryDate
        )
        val firstResult = queryUpdatePd(globalCode.text).update(pd)
        val secondResult = newProfile.dataFiliation.fold(-1)(
          {
            filiationData =>
              val fd = (
                filiationData.fullName,
                filiationData.nickname,
                filiationData.birthday.map { x => new java.sql.Date(x.getTime) },
                filiationData.birthPlace,
                filiationData.nationality,
                filiationData.identification,
                filiationData.identificationIssuingAuthority,
                filiationData.address
              )
              val resultPdf = queryUpdatePdFiliation(globalCode.text)
                .update(fd) match {
                  case 0 =>
                    val profileMDF = new ProfileDataFiliationRow(
                      0,
                      globalCode.text,
                      filiationData.fullName,
                      filiationData.nickname,
                      filiationData.birthday.map { x => new java.sql.Date(x.getTime) },
                      filiationData.birthPlace,
                      filiationData.nationality,
                      filiationData.identification,
                      filiationData.identificationIssuingAuthority,
                      filiationData.address
                    )
                    profileMetaDataFiliations += profileMDF
                    1
                  case number => number
                }
              imageList.foreach {
                imageListFile =>
                  imageListFile
                    .foreach(
                      file => {
                        val is: InputStream = new FileInputStream(file)
                        val byteArray: Array[Byte] = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
                        val blob: SerialBlob = new SerialBlob(byteArray)
                        val resourceRow = ProfileDataFiliationResourcesRow(
                          0, globalCode.text, blob, "I"
                        )
                        profileMetaDataFiliationResources += resourceRow
                      }
                    )
              }
              pictureList.foreach {
                picturesListFile =>
                  picturesListFile.foreach(
                    file => {
                      val is: InputStream = new FileInputStream(file)
                      val byteArray: Array[Byte] = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
                      val blob: SerialBlob = new SerialBlob(byteArray)
                      val resourceRow = ProfileDataFiliationResourcesRow(
                        0, globalCode.text, blob, "P"
                      )
                      profileMetaDataFiliationResources += resourceRow
                    }
                  )
              }
              signatureList.foreach {
                signaturesListFile =>
                  signaturesListFile
                    .foreach(
                      file => {
                        val is: InputStream = new FileInputStream(file)
                        val byteArray: Array[Byte] = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
                        val blob: SerialBlob = new SerialBlob(byteArray)
                        val resourceRow = ProfileDataFiliationResourcesRow(
                          0, globalCode.text, blob, "S"
                        )
                        profileMetaDataFiliationResources += resourceRow
                      }
                    )
              }
              resultPdf
          }
        )
        if (firstResult < 1 && secondResult == 0) false else true
      }
  }

  override def getDeletedMotive(globalCode: SampleCode): Future[Option[DeletedMotive]] = Future {
    DB.withSession { implicit session =>
      queryDeletePd(globalCode.text).firstOption flatMap {
        case (bool, solicitorOpt, motiveOpt) =>

          (for {
            sol <- solicitorOpt
            mot <- motiveOpt
          } yield (sol, mot)) map { x => DeletedMotive(x._1, x._2) }

      }
    }
  }

  private def queryDefineGetProfilesDataBySampleCodes(globalCodes: List[String]) = for (
    ((pd, pmdf),epd) <- profilesData leftJoin profileMetaDataFiliations on (_.globalCode === _.profileData) leftJoin externalProfileDataTable on (_._1.id === _.id)
      if pd.globalCode inSetBind globalCodes
  ) yield (pd, pmdf.?,epd.?)

  override def findByCodes(globalCodes: List[SampleCode]): Future[Seq[ProfileData]] = Future {

    DB.withSession { implicit session =>
      queryDefineGetProfilesDataBySampleCodes(globalCodes.map(_.text)).list map {
        case (pd, dataFiliation, epd) =>

          val posibleDataFiliation = dataFiliation map { df =>

            val inprints = queryGetResources((pd.globalCode, "I")).list
            val pictures = queryGetResources((pd.globalCode, "P")).list
            val signatures = queryGetResources((pd.globalCode, "S")).list

            DataFiliation(df.fullName, df.nickname, df.birthday, df.birthPlace, df.nationality, df.identification, df.identificationIssuingAuthority, df.address, inprints, pictures, signatures)
          }

          ProfileData(AlphanumericId(pd.category),
            SampleCode(pd.globalCode), pd.attorney, pd.bioMaterialType,
            pd.court, pd.crimeInvolved, pd.crimeType, pd.criminalCase,
            pd.internalSampleCode, pd.assignee, pd.laboratory, pd.deleted, None,
            pd.responsibleGeneticist, pd.profileExpirationDate, pd.sampleDate,
            pd.sampleEntryDate, posibleDataFiliation,epd.isDefined)
      }
    }
  }

  override def getGlobalCode(internalSampleCode: String): Future[Option[SampleCode]] = Future{
    DB.withSession { implicit session => 
      queryGetGlobalCode(internalSampleCode).firstOption.map { SampleCode(_) }  
    }
  }

  override def getProfileUploadStatusByGlobalCode(gc:SampleCode):Future[Option[Long]] = {
    this.runInTransactionAsync {
      implicit session => {
        try {
          getProfileUploadedByGlobalCode(gc.text)
            .firstOption
            .fold[Option[Long]](Some(0))(x => Some(x.status))
        } catch {
          case e: Exception => {
            None
          }
        }
      }
    }
  }

  override def gefFailedProfilesUploaded():Future[List[ProfileUploadedRow]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getFailedUploadedProfiles.list
      } catch {
        case e: Exception => {
          Nil
        }
      }
    }
    }
  }

  override def gefFailedProfilesUploadedDeleted():Future[List[ProfileUploadedRow]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getFailedUploadedProfilesDeleted.list
      } catch {
        case e: Exception => {
          Nil
        }
      }
    }
    }
  }

  override def gefFailedProfilesSentDeleted(labCode:String):Future[List[ProfileSentRow]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getFailedSentProfilesDeleted(labCode).list
      } catch {
        case e: Exception => {
          Nil
        }
      }
    }
    }
  }
  override def updateUploadStatus(globalCode: String,status:Long,motive:Option[String] = None): Future[Either[String,Unit]]= {
    this.runInTransactionAsync { implicit session => {
      try {
      getByGlobalCode(globalCode).firstOption match {
        case None => Left(Messages("error.E0940"))
        case Some(row) => {
          profileUploaded insertOrUpdate models.Tables.ProfileUploadedRow(row.id,row.globalCode,status,motive)
          Right(())
        }
      }
      } catch {
        case e: Exception => {
          Left(e.getMessage)
        }
      }
    }
    }
    }


  override def getExternalProfileDataByGlobalCode(globalCode:String):Future[Option[ExternalProfileDataRow]] = {
    this.runInTransactionAsync { implicit session => {
      getExternalProfileDataByGlobalCodeCompiled(globalCode).firstOption
    }
    }
  }

  override def findUploadedProfilesByCodes(
    globalCodes: Seq[SampleCode]
  ): Future[Seq[SampleCode]] = {
    this.runInTransactionAsync {
      implicit session => {
        queryProfileUploadedByGlobalCodeInSet(
          globalCodes.map(x => x.text)
        )
        .list
        .map(x => SampleCode(x.globalCode))
      }
    }
  }
  override def updateProfileSentStatus(globalCode: String,status:Long,motive:Option[String]= None,labCode:String): Future[Either[String,Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getByGlobalCode(globalCode).firstOption match {
          case None => Left(Messages("error.E0940"))
          case Some(row) => {
            profileSent insertOrUpdate models.Tables.ProfileSentRow(row.id,labCode,row.globalCode,status,motive)
            Right(())
          }
        }
      } catch {
        case e: Exception => {
          Left(e.getMessage)
        }
      }
    }
    }
  }

  def getMtRcrs():Future[MtRCRS] = {
    this.runInTransactionAsync { implicit session => {
      try {
        val tabla = mitochondrialRcrs.list.map(x => x.position -> x.base)
        MtRCRS(tabla.toMap)
      } catch {
        case e: Exception => {
          MtRCRS(Map.empty)
        }
      }
    }
    }
  }

  }

@Singleton
class ProtoProfileDataRepository @Inject() (implicit app: Application) extends SlickProfileDataRepository {
  override val profilesData: TableQuery[Tables.ProfileData] = new TableQuery(tag => new Tables.ProfileData(tag, Some("STASH"), "PROFILE_DATA"))
  override val profileMetaDataFiliations: TableQuery[Tables.ProfileDataFiliation] = new TableQuery(tag => new Tables.ProfileDataFiliation(tag, Some("STASH"), "PROFILE_DATA_FILIATION"))
  override val profileMetaDataFiliationResources: TableQuery[Tables.ProfileDataFiliationResources] = new TableQuery(tag => new Tables.ProfileDataFiliationResources(tag, Some("STASH"), "PROFILE_DATA_FILIATION_RESOURCES"))
  override def updateProfileData(globalCode: SampleCode, newProfile: ProfileData, imageList: Option[List[File]] = None, pictureList: Option[List[File]] = None, signatureList: Option[List[File]] = None): Future[Boolean] = Future {

    DB.withTransaction { implicit session =>

      val posibleExpirationDate = newProfile.profileExpirationDate map { ed => new java.sql.Date(ed.getTime()) }
      val posiblesampleDate = newProfile.sampleDate map { sd => new java.sql.Date(sd.getTime()) }
      val posiblesampleEntryDate = newProfile.sampleEntryDate map { sed => new java.sql.Date(sed.getTime()) }

      val pd = (newProfile.attorney, newProfile.bioMaterialType, newProfile.court, newProfile.crimeInvolved, newProfile.crimeType, newProfile.criminalCase,
        newProfile.responsibleGeneticist, posibleExpirationDate, posiblesampleDate, posiblesampleEntryDate, newProfile.laboratory)

      val firstResult = queryUpdateProtoPd(globalCode.text).update(pd)

      val secondResult = newProfile.dataFiliation.fold(-1)({ filiationData =>

        val fd = (filiationData.fullName, filiationData.nickname, filiationData.birthday.map { x => new java.sql.Date(x.getTime) }, filiationData.birthPlace, filiationData.nationality,
          filiationData.identification, filiationData.identificationIssuingAuthority, filiationData.address)

        val resultPdf = queryUpdatePdFiliation(globalCode.text).update(fd) match {
          case 0 => {
            val profileMDF = new ProfileDataFiliationRow(0, globalCode.text, filiationData.fullName, filiationData.nickname,
              filiationData.birthday.map { x => new java.sql.Date(x.getTime) }, filiationData.birthPlace, filiationData.nationality,
              filiationData.identification, filiationData.identificationIssuingAuthority, filiationData.address)
            profileMetaDataFiliations += profileMDF
            1
          }
          case number => number
        }

        imageList.map { imageListFile =>

          imageListFile.foreach(file => {
            val is: InputStream = new FileInputStream(file)
            val byteArray = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
            val blob = new SerialBlob(byteArray)
            val resourceRow = ProfileDataFiliationResourcesRow(0, globalCode.text, blob, "I")
            profileMetaDataFiliationResources += resourceRow
          })
        }

        pictureList.map { picturesListFile =>

          picturesListFile.foreach(file => {
            val is: InputStream = new FileInputStream(file)
            val byteArray = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
            val blob = new SerialBlob(byteArray)
            val resourceRow = ProfileDataFiliationResourcesRow(0, globalCode.text, blob, "P")
            profileMetaDataFiliationResources += resourceRow
          })
        }

        signatureList.map { signaturesListFile =>
          signaturesListFile.foreach(file => {
            val is: InputStream = new FileInputStream(file)
            val byteArray = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
            val blob = new SerialBlob(byteArray)
            val resourceRow = ProfileDataFiliationResourcesRow(0, globalCode.text, blob, "S")
            profileMetaDataFiliationResources += resourceRow
          })
        }

        resultPdf
      })
      if (firstResult < 1 && secondResult == 0) false else true
    }
  }
}
