package profiledata

import profile.MtRCRS
import types.{AlphanumericId, SampleCode}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import models.Tables

trait ProfileDataRepository {
  def findByCode(globalCode: SampleCode): Future[Option[ProfileData]]
  def isDeleted(globalCode: SampleCode): Future[Option[Boolean]]
  def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]]
  def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]]

  def getProfilesByUser(search: ProfileDataSearch): Future[Seq[ProfileDataFull]]
  def getTotalProfilesByUser(search: ProfileDataSearch): Future[Int]
  def getTotalProfilesByUser(userId: String, isSuperUser: Boolean, category: String = ""): Future[Int]
  def getIsProfileReplicated(globalCode: SampleCode): Future[Boolean]
}

trait ProfileDataService {
  def getMtRcrs(): Future[MtRCRS]
}

@Singleton
class ProfileDataRepositoryStub extends ProfileDataRepository {
  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] = Future.successful(None)
  override def isDeleted(globalCode: SampleCode): Future[Option[Boolean]] = Future.successful(None)
  override def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]] = Future.successful(None)
  override def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]] = Future.successful(List.empty)
  override def getProfilesByUser(search: ProfileDataSearch): Future[Seq[ProfileDataFull]] = Future.successful(Seq.empty)
  override def getTotalProfilesByUser(search: ProfileDataSearch): Future[Int] = Future.successful(0)
  override def getTotalProfilesByUser(userId: String, isSuperUser: Boolean, category: String = ""): Future[Int] = Future.successful(0)
  override def getIsProfileReplicated(globalCode: SampleCode): Future[Boolean] = Future.successful(false)
}

@Singleton
class ProfileDataServiceStub extends ProfileDataService {
  override def getMtRcrs(): Future[MtRCRS] = Future.successful(MtRCRS(Map.empty))
}

@Singleton
class SlickProfileDataRepository @Inject()(db: Database)(implicit ec: ExecutionContext)
    extends ProfileDataRepository {

  private val profilesData    = Tables.ProfileData
  private val profileUploaded = Tables.ProfileUploaded
  private val externalPD      = Tables.ExternalProfileData

  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] = {
    val q = profilesData
      .joinLeft(externalPD).on(_.id === _.id)
      .filter(_._1.globalCode === globalCode.text)
    db.run(q.result.headOption).map(_.map { case (pd, epd) =>
      ProfileData(
        AlphanumericId(pd.category), SampleCode(pd.globalCode),
        pd.attorney, pd.bioMaterialType, pd.court, pd.crimeInvolved,
        pd.crimeType, pd.criminalCase, pd.internalSampleCode, pd.assignee,
        pd.laboratory, pd.deleted, pd.responsibleGeneticist,
        pd.profileExpirationDate, pd.sampleDate, pd.sampleEntryDate,
        epd.isDefined
      )
    })
  }

  override def isDeleted(globalCode: SampleCode): Future[Option[Boolean]] =
    db.run(profilesData.filter(_.globalCode === globalCode.text).map(_.deleted).result.headOption)

  override def getProfileUploadStatusByGlobalCode(globalCode: SampleCode): Future[Option[Long]] =
    db.run(profileUploaded.filter(_.globalCode === globalCode.text).map(_.status).result.headOption)

  override def findUploadedProfilesByCodes(codes: List[SampleCode]): Future[List[SampleCode]] = {
    val codeStrings = codes.map(_.text).toSet
    db.run(profileUploaded.filter(_.globalCode.inSet(codeStrings)).map(_.globalCode).result)
      .map(_.map(SampleCode(_)).toList)
  }

  override def getIsProfileReplicated(globalCode: SampleCode): Future[Boolean] =
    db.run(profileUploaded.filter(_.globalCode === globalCode.text).map(_.status).result.headOption)
      .map {
        case Some(20L) | Some(3L) => false
        case Some(_)              => true
        case None                 => false
      }

  override def getProfilesByUser(search: ProfileDataSearch): Future[Seq[ProfileDataFull]] = {
    val baseQuery = profilesData
      .joinLeft(profileUploaded).on(_.id === _.id)
      .joinLeft(externalPD).on(_._1.id === _.id)
      .filter { case ((pd, _), _) =>
        (search.isSuperUser: Rep[Boolean]) || pd.assignee === search.userId
      }
      .filter { case ((pd, _), _) =>
        (pd.deleted && (search.inactive: Rep[Boolean])) || (!pd.deleted && (search.active: Rep[Boolean]))
      }
      .sortBy { case ((pd, _), _) => pd.globalCode.desc }

    val withCategory =
      if (search.category.nonEmpty)
        baseQuery.filter { case ((pd, _), _) => pd.category === search.category }
      else baseQuery

    val withNotUploaded =
      if (search.notUploaded.contains(true))
        withCategory.filter { case ((_, pu), _) => pu.isEmpty }
      else withCategory

    val paged = withNotUploaded.drop(search.page * search.pageSize).take(search.pageSize)

    db.run(paged.result).flatMap { rows =>
      Future.sequence(rows.map { case ((pd, _), epd) =>
        getIsProfileReplicated(SampleCode(pd.globalCode)).map { readOnly =>
          ProfileDataFull(
            AlphanumericId(pd.category), SampleCode(pd.globalCode),
            pd.attorney, pd.bioMaterialType, pd.court, pd.crimeInvolved,
            pd.crimeType, pd.criminalCase, pd.internalSampleCode, pd.assignee,
            pd.laboratory, pd.deleted, None, pd.responsibleGeneticist,
            pd.profileExpirationDate, pd.sampleDate, pd.sampleEntryDate,
            None, readOnly, epd.isDefined
          )
        }
      })
    }
  }

  override def getTotalProfilesByUser(search: ProfileDataSearch): Future[Int] = {
    val baseQuery = profilesData
      .joinLeft(profileUploaded).on(_.id === _.id)
      .joinLeft(externalPD).on(_._1.id === _.id)
      .filter { case ((pd, _), _) =>
        (search.isSuperUser: Rep[Boolean]) || pd.assignee === search.userId
      }
      .filter { case ((pd, _), _) =>
        (pd.deleted && (search.inactive: Rep[Boolean])) || (!pd.deleted && (search.active: Rep[Boolean]))
      }

    val withCategory =
      if (search.category.nonEmpty)
        baseQuery.filter { case ((pd, _), _) => pd.category === search.category }
      else baseQuery

    val withNotUploaded =
      if (search.notUploaded.contains(true))
        withCategory.filter { case ((_, pu), _) => pu.isEmpty }
      else withCategory

    db.run(withNotUploaded.length.result)
  }

  override def getTotalProfilesByUser(userId: String, isSuperUser: Boolean, category: String = ""): Future[Int] = {
    val baseQuery = profilesData.filter { pd =>
      (isSuperUser: Rep[Boolean]) || pd.assignee === userId
    }
    val withCategory = if (category.nonEmpty) baseQuery.filter(_.category === category) else baseQuery
    db.run(withCategory.length.result)
  }
}
