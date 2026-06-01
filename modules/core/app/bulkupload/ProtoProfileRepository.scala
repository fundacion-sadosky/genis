package bulkupload

import java.text.SimpleDateFormat

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Named, Singleton}

import configdata.{CategoryService, MatchingRule}
import kits.StrKitLocus
import models.Tables
import play.api.Logger
import play.api.libs.json.Json
import profile.{Profile, ProfileRepository}
import search.PaginationSearch
import slick.jdbc.GetResult
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api.*
import types.{AlphanumericId, SampleCode}

abstract class ProtoProfileRepository:
  def createBatch(user: String, protoProfileStream: LazyList[ProtoProfile], laboratory: String, kits: Map[String, List[StrKitLocus]], label: Option[String], analysisType: String): Future[Long]
  def getBatch(batchId: Long): Future[Option[ProtoProfilesBatch]]
  def getBatchesStep1(userId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]]
  def countBatchesStep1(userId: String, isSuperUser: Boolean): Future[Int]
  def getBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]]
  def countBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean): Future[Int]
  def getProtoProfilesStep1(batchId: Long, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]]
  def getProtoProfilesStep2(batchId: Long, userId: String, isSuperUser: Boolean, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]]
  def getProtoProfile(id: Long): Future[Option[ProtoProfile]]
  def getProtoProfileWithBatchId(id: Long): Future[Option[(ProtoProfile, Long)]]
  def updateProtoProfileStatus(id: Long, status: ProtoProfileStatus.Value): Future[Int]
  def updateProtoProfileStatus(internalCode: String, status: String): Future[Int]
  def updateProtoProfileData(id: Long, cat: AlphanumericId): Future[Int]
  def updateProtoProfileMatchingRulesMismatch(id: Long, matchingRules: Seq[MatchingRule], mismatches: Profile.Mismatch): Future[Int]
  def exists(sampleName: String): Future[(Option[SampleCode], Option[Long])]
  def validateAssigneAndCategory(globalCode: SampleCode, assigne: String, category: Option[AlphanumericId]): Future[Option[String]]
  def hasProfileDataFiliation(id: Long): Future[Boolean]
  def setRejectMotive(id: Long, motive: String, rejectionUser: String, idMotive: Long, timeRejected: java.sql.Timestamp): Future[Int]
  def canDeleteKit(id: String): Future[Boolean]
  def deleteBatch(id: Long): Future[Either[String, Long]]
  def countImportedProfilesByBatch(idBatch: Long): Future[Either[String, Long]]
  def countAllProtoProfilesInBatch(batchId: Long): Future[Int]
  def getSearchBachLabelID(userId: String, isSuperUser: Boolean, filter: String): Future[Seq[ProtoProfilesBatchView]]
  def getBatchSearchModalViewByIdOrLabel(input: String, idCase: Long): Future[List[BatchModelView]]
  def mtExistente(sampleName: String): Future[Boolean]
  def getProtoProfileStatus(internalCode: String): String

@Singleton
class SlickProtoProfileRepository @Inject() (
  categoryService: CategoryService,
  @Named("protoProfileGcDummy") ppGcD: String,
  db: Database,
  profileRepository: ProfileRepository = null
)(using ec: ExecutionContext) extends ProtoProfileRepository:

  val logger = Logger(this.getClass)

  private def rowToProtoProfile(row: Tables.ProtoProfileRow): ProtoProfile =
    val geno = Json.fromJson[ProtoProfile.Genotypification](Json.parse(row.genotypifications)).get
    val errs = row.errors.fold(Seq.empty[String])(_.split(";").toSeq)
    val mr   = Json.fromJson[Seq[MatchingRule]](Json.parse(row.matchingRules)).get
    val mm   = Json.fromJson[Profile.Mismatch](Json.parse(row.mismatchs)).get
    ProtoProfile(row.id, row.sampleName, row.assignee, row.category,
      ProtoProfileStatus.withName(row.status), row.panel, geno, mm, mr, errs,
      row.genemapperLine, row.preexistence.map(SampleCode(_)), rejectMotive = row.rejectMotive)

  // ---- Step1 queries -------------------------------------------------------

  implicit val getBatchView1: GetResult[ProtoProfilesBatchView] = GetResult(r =>
    ProtoProfilesBatchView(r.nextLong(), r.nextString(), r.nextDate(), r.nextLong().toInt,
      r.nextLong().toInt, r.nextLong().toInt, r.nextLong().toInt, r.nextStringOption(),
      r.nextLong().toInt, r.nextLong().toInt, r.nextString()))

  implicit val getBatchView2: GetResult[(Long, String, java.sql.Date, Long, Option[String], Long, String, Long)] = GetResult(r =>
    (r.nextLong(), r.nextString(), r.nextDate(), r.nextLong(), r.nextStringOption(), r.nextLong(), r.nextString(), r.nextLong()))

  implicit val getBatchModal: GetResult[(Long, String, java.sql.Date)] = GetResult(r =>
    (r.nextLong(), r.nextString(), r.nextDate()))

  override def countBatchesStep1(userId: String, isSuperUser: Boolean): Future[Int] =
    db.run(sql"""SELECT COUNT(*)
       FROM "APP"."BATCH_PROTO_PROFILE" bpp
       WHERE (bpp."USER" = $userId OR $isSuperUser)""".as[Int].head)

  override def countBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean): Future[Int] =
    db.run(sql"""SELECT COUNT(DISTINCT bpp."ID")
       FROM "APP"."BATCH_PROTO_PROFILE" bpp
       WHERE EXISTS (
         SELECT 1 FROM "APP"."PROTO_PROFILE" pp
         WHERE pp."ID_BATCH" = bpp."ID"
         AND (pp."ASSIGNEE" = $geneMapperId OR $isSuperUser)
         AND pp."STATUS" IN ('Approved','Rejected','Imported','Uploaded','ReplicatedMatchingProfile')
       )""".as[Int].head)

  override def getBatchesStep1(userId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]] =
    db.run(sql"""SELECT bpp."ID", bpp."USER", bpp."DATE",
      sum(1) as "TOTAL",
      sum(CASE WHEN pp."STATUS" = 'Imported' THEN 1 ELSE 0 END),
      sum(CASE WHEN pp."STATUS" IN ('Incomplete','ReadyForApproval','Approved') THEN 1 ELSE 0 END),
      sum(CASE WHEN pp."STATUS" IN ('Disapproved','Invalid','Rejected') THEN 1 ELSE 0 END),
      bpp."LABEL",
      sum(CASE WHEN pp."STATUS" = 'ReadyForApproval' THEN 1 ELSE 0 END),
      sum(CASE WHEN pp."STATUS" = 'Incomplete' THEN 1 ELSE 0 END),
      bpp."ANALYSISTYPE"
      FROM "APP"."BATCH_PROTO_PROFILE" bpp
      INNER JOIN "APP"."PROTO_PROFILE" pp ON pp."ID_BATCH" = bpp."ID"
      WHERE (bpp."USER" = $userId OR $isSuperUser)
      GROUP BY bpp."ID"
      ORDER BY bpp."ID" DESC
      LIMIT $limit OFFSET $offset""".as[ProtoProfilesBatchView](using getBatchView1))

  override def getBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]] =
    db.run(sql"""SELECT bpp."ID", bpp."USER", bpp."DATE",
      (SELECT COUNT(*) FROM "APP"."PROTO_PROFILE" pp
       WHERE pp."ID_BATCH" = bpp."ID"
       AND (pp."ASSIGNEE" = $geneMapperId OR $isSuperUser)
       AND pp."STATUS" IN ('Approved','Rejected','Imported','Uploaded','DesktopSearch','ReplicatedMatchingProfile')),
      bpp."LABEL",
      (SELECT COUNT(*) FROM "APP"."PROTO_PROFILE" pp
       WHERE pp."ID_BATCH" = bpp."ID"
       AND (pp."ASSIGNEE" = $geneMapperId OR $isSuperUser)
       AND pp."STATUS" = 'Approved'),
      bpp."ANALYSISTYPE",
      (SELECT COUNT(*) FROM "APP"."PROTO_PROFILE" pp WHERE pp."ID_BATCH" = bpp."ID")
      FROM "APP"."BATCH_PROTO_PROFILE" bpp
      WHERE EXISTS (
        SELECT 1 FROM "APP"."PROTO_PROFILE" pp
        WHERE pp."ID_BATCH" = bpp."ID"
        AND (pp."ASSIGNEE" = $geneMapperId OR $isSuperUser)
        AND pp."STATUS" IN ('Approved','Rejected','Imported','Uploaded','DesktopSearch','ReplicatedMatchingProfile')
      )
      GROUP BY bpp."ID"
      ORDER BY bpp."ID" DESC
      LIMIT $limit OFFSET $offset""".as[(Long, String, java.sql.Date, Long, Option[String], Long, String, Long)](using getBatchView2)).map {
      _.map { case (id, user, date, total, label, totalApproved, analysisType, batchTotal) =>
        ProtoProfilesBatchView(id, user, date, total.toInt, 0, 0, 0, label, totalApproved.toInt, 0, analysisType, batchTotal.toInt)
      }
    }

  override def getSearchBachLabelID(userId: String, isSuperUser: Boolean, filter: String): Future[Seq[ProtoProfilesBatchView]] =
    db.run(sql"""SELECT bpp."ID", bpp."USER", bpp."DATE",
      sum(1) as "TOTAL",
      sum(CASE WHEN pp."STATUS" = 'Imported' THEN 1 ELSE 0 END),
      sum(CASE WHEN pp."STATUS" IN ('Incomplete','ReadyForApproval','Approved','DesktopSearch') THEN 1 ELSE 0 END),
      sum(CASE WHEN pp."STATUS" IN ('Disapproved','Invalid','Rejected') THEN 1 ELSE 0 END),
      bpp."LABEL",
      sum(CASE WHEN pp."STATUS" = 'ReadyForApproval' THEN 1 ELSE 0 END),
      sum(CASE WHEN pp."STATUS" = 'Incomplete' THEN 1 ELSE 0 END),
      bpp."ANALYSISTYPE"
      FROM "APP"."BATCH_PROTO_PROFILE" bpp
      INNER JOIN "APP"."PROTO_PROFILE" pp ON pp."ID_BATCH" = bpp."ID"
      WHERE (bpp."USER" = $userId OR $isSuperUser)
      AND (LOWER(bpp."LABEL") LIKE LOWER('%'||$filter||'%') OR CAST(bpp."ID" AS varchar(50)) LIKE $filter)
      GROUP BY bpp."ID"
      ORDER BY bpp."ID" DESC""".as[ProtoProfilesBatchView](using getBatchView1))

  override def getBatchSearchModalViewByIdOrLabel(input: String, idCase: Long): Future[List[BatchModelView]] =
    val sdf = new SimpleDateFormat("dd/MM/yyyy")
    db.run(sql"""SELECT bpp."ID", bpp."LABEL", bpp."DATE"
      FROM "APP"."BATCH_PROTO_PROFILE" bpp
      INNER JOIN "APP"."PROTO_PROFILE" pp ON pp."ID_BATCH" = bpp."ID"
      WHERE pp."STATUS" = 'Imported'
      AND (LOWER(bpp."LABEL") LIKE LOWER('%'||$input||'%') OR CAST(bpp."ID" AS varchar(50)) LIKE $input)
      GROUP BY bpp."ID"
      ORDER BY bpp."ID" DESC""".as[(Long, String, java.sql.Date)](using getBatchModal)).map {
      _.toList.map { case (id, label, date) => BatchModelView(id, Some(label), sdf.format(date)) }
    }

  // ---- CRUD ----------------------------------------------------------------

  override def getBatch(batchId: Long): Future[Option[ProtoProfilesBatch]] =
    db.run(Tables.batchProtoProfiles.filter(_.id === batchId).result.headOption).map {
      _.map(b => ProtoProfilesBatch(b.id, b.user, b.date, b.label))
    }

  override def getProtoProfilesStep1(batchId: Long, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]] =
    val base = Tables.protoProfiles.filter(_.idBatch === batchId).sortBy(pp => (pp.errors.isDefined.desc, pp.id.asc))
    val q = paginationSearch.fold(base)(p => base.drop(p.page * p.pageSize).take(p.pageSize))
    db.run(q.result).map(_.map(rowToProtoProfile))

  override def getProtoProfilesStep2(batchId: Long, userId: String, isSuperUser: Boolean, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]] =
    val approvedStatuses = Set("Approved","Rejected","Imported","Uploaded","DesktopSearch","ReplicatedMatchingProfile")
    val baseQ = Tables.protoProfiles.filter(pp => pp.idBatch === batchId && pp.status.inSet(approvedStatuses))
    val withUser = if isSuperUser then baseQ else baseQ.filter(_.assignee === userId)
    val sorted = withUser.sortBy(pp => (pp.errors.isDefined.desc, pp.id.asc))
    val q = paginationSearch.fold(sorted)(p => sorted.drop(p.page * p.pageSize).take(p.pageSize))
    db.run(q.result).map(_.map(rowToProtoProfile))

  override def getProtoProfile(id: Long): Future[Option[ProtoProfile]] =
    db.run(Tables.protoProfiles.filter(_.id === id).result.headOption).map(_.map(rowToProtoProfile))

  override def getProtoProfileWithBatchId(id: Long): Future[Option[(ProtoProfile, Long)]] =
    db.run(Tables.protoProfiles.filter(_.id === id).result.headOption).map {
      _.map(row => (rowToProtoProfile(row), row.idBatch))
    }

  override def updateProtoProfileStatus(id: Long, status: ProtoProfileStatus.Value): Future[Int] =
    db.run(Tables.protoProfiles.filter(_.id === id).map(_.status).update(status.toString).transactionally)

  override def updateProtoProfileStatus(internalCode: String, status: String): Future[Int] =
    db.run(Tables.protoProfiles.filter(_.sampleName === internalCode).map(_.status).update(status).transactionally)

  override def updateProtoProfileData(id: Long, cat: AlphanumericId): Future[Int] =
    db.run(Tables.protoProfiles.filter(_.id === id).map(_.category).update(cat.text).transactionally)

  override def updateProtoProfileMatchingRulesMismatch(id: Long, matchingRules: Seq[MatchingRule], mismatches: Profile.Mismatch): Future[Int] =
    val mRules = Json.toJson(matchingRules).toString
    val mis    = Json.toJson(mismatches).toString
    db.run(Tables.protoProfiles.filter(_.id === id).map(pp => (pp.matchingRules, pp.mismatchs)).update((mRules, mis)).transactionally)

  override def setRejectMotive(id: Long, motive: String, rejectionUser: String, idRejectMotive: Long, rejectionDate: java.sql.Timestamp): Future[Int] =
    db.run(Tables.protoProfiles.filter(_.id === id)
      .map(pp => (pp.rejectMotive, pp.rejectionUser, pp.idRejectMotive, pp.rejectionDate))
      .update((Some(motive), Some(rejectionUser), Some(idRejectMotive), Some(rejectionDate)))
      .transactionally)

  override def canDeleteKit(id: String): Future[Boolean] =
    val activeStatuses = Set("ReadyForApproval","Approved","Incomplete","Imported")
    db.run(Tables.protoProfiles.filter(pp => pp.panel === id && pp.status.inSet(activeStatuses)).exists.result).map(!_)

  override def deleteBatch(id: Long): Future[Either[String, Long]] =
    val action = for {
      _ <- Tables.stashProfileDataFiliationResources.filter(
        _.profileDataFiliation in Tables.stashProfileDataFiliation
          .filter(_.profileData in Tables.protoProfiles.filter(_.idBatch === id).map(pp => (ppGcD + pp.id.asColumnOf[String])))
          .map(_.profileData)
      ).delete
      _ <- Tables.stashProfileDataFiliation.filter(
        _.profileData in Tables.protoProfiles.filter(_.idBatch === id).map(pp => (ppGcD + pp.id.asColumnOf[String]))
      ).delete
      _ <- Tables.stashProfileData.filter(
        _.id in Tables.protoProfiles.filter(_.idBatch === id).map(_.id)
      ).delete
      _ <- Tables.protoProfiles.filter(_.idBatch === id).delete
      _ <- Tables.batchProtoProfiles.filter(_.id === id).delete
    } yield ()
    db.run(action.transactionally).map(_ => Right(id)).recover {
      case e => Left(e.getMessage)
    }

  override def countImportedProfilesByBatch(idBatch: Long): Future[Either[String, Long]] =
    db.run(
      Tables.protoProfiles.filter(pp => pp.idBatch === idBatch && pp.status === "Imported").length.result
    ).map(n => Right(n.toLong)).recover { case e => Left(e.getMessage) }

  override def countAllProtoProfilesInBatch(batchId: Long): Future[Int] =
    db.run(Tables.protoProfiles.filter(_.idBatch === batchId).length.result)

  override def hasProfileDataFiliation(id: Long): Future[Boolean] =
    val gc   = ppGcD + id
    def filComplete(f: Tables.ProfileDataFiliationRow): Boolean =
      f.fullName.exists(_.nonEmpty) && f.nickname.exists(_.nonEmpty) &&
      f.birthday.exists(_.nonEmpty) && f.birthPlace.exists(_.nonEmpty) &&
      f.nationality.exists(_.nonEmpty) && f.identification.exists(_.nonEmpty) &&
      f.identificationIssuingAuthority.exists(_.nonEmpty) && f.address.exists(_.nonEmpty)

    // Legacy checks STASH filiation (for the dummy global code) AND APP filiation (by sampleName/internalCode)
    val stashFil = db.run(Tables.stashProfileDataFiliation.filter(_.profileData === gc).result.headOption)
    val appFil   = db.run(Tables.protoProfiles.filter(_.id === id).result.headOption).flatMap {
      case None => Future.successful(None)
      case Some(pp) =>
        db.run(Tables.profileMetaDataFiliations.filter(_.profileData in
          Tables.profilesData.filter(_.internalCode === pp.sampleName).map(_.globalCode)
        ).result.headOption)
    }
    for
      s <- stashFil
      a <- appFil
    yield s.exists(filComplete) || a.exists(filComplete)

  override def exists(sampleName: String): Future[(Option[SampleCode], Option[Long])] =
    val profileQ = Tables.profilesData.filter(pd => pd.internalSampleCode === sampleName && !pd.deleted).map(_.globalCode).result
    val batchQ   = Tables.protoProfiles
      .filter(pp => pp.sampleName === sampleName && pp.status.inSet(Set("ReadyForApproval","Approved","Incomplete")))
      .map(_.idBatch).result
    for
      profiles <- db.run(profileQ)
      batches  <- db.run(batchQ)
    yield
      val gc      = profiles.headOption.map(SampleCode(_))
      val batchId = batches.headOption
      (gc, batchId)

  override def validateAssigneAndCategory(globalCode: SampleCode, assigne: String, category: Option[AlphanumericId]): Future[Option[String]] =
    db.run(Tables.profilesData.filter(pd => pd.globalCode === globalCode.text && pd.assignee === assigne).map(_.category).result.headOption)
      .map {
        case None => Some(s"error.E0662 assignee=$assigne")
        case Some(cat) => category.flatMap { cty =>
          if cat == cty.text then None
          else Some(s"error.E0663 cat=$cat expected=${cty.text}")
        }
      }

  override def createBatch(user: String, protoProfileStream: LazyList[ProtoProfile], laboratory: String, kits: Map[String, List[StrKitLocus]], label: Option[String], analysisType: String): Future[Long] =
    val batchRow = Tables.BatchProtoProfileRow(0, user, new java.sql.Date(System.currentTimeMillis()), label, analysisType)
    val insertBatch = (Tables.batchProtoProfiles returning Tables.batchProtoProfiles.map(_.id)) += batchRow

    val profileActions = protoProfileStream.map { pp =>
      if !kits.contains(pp.kit) then throw KitNotExistsException(pp.kit)
      val joinLociGen = for {
        locus <- kits(pp.kit)
        g     <- pp.genotypification
        if g.locus == locus.id
      } yield (locus.order, g)
      val sortedGen        = joinLociGen.sortBy(_._1).map(_._2)
      val jsonGenotypification = Json.toJson(sortedGen).toString
      val matchingRules    = Json.toJson(pp.matchingRules).toString
      val mismatches       = Json.toJson(pp.mismatches).toString
      val errorsStr        = if pp.errors.nonEmpty then Some(pp.errors.mkString(";")) else None

      (batchId: Long) =>
        val ppRow = Tables.ProtoProfileRow(0, pp.sampleName, batchId, pp.assignee,
          pp.category, pp.status.toString, pp.kit, errorsStr,
          jsonGenotypification, matchingRules, mismatches, None,
          pp.preexistence.map(_.text), pp.geneMapperLine,
          None, None, None)
        for {
          ppId <- (Tables.protoProfiles returning Tables.protoProfiles.map(_.id)) += ppRow
          gcFk = s"$ppGcD$ppId"
          stashRow = Tables.ProfileDataRow(0, pp.category, gcFk, pp.sampleName,
            None, None, None, None, None, None, None,
            pp.sampleName, pp.assignee, laboratory,
            None, None, None, None, false, None, None, false)
          _ <- Tables.stashProfileData += stashRow
          _ <- DBIO.from(categoryService.getCategory(AlphanumericId(pp.category))).flatMap {
            case Some(cat) if cat.filiationDataRequired =>
              Tables.stashProfileDataFiliation += Tables.ProfileDataFiliationRow(
                0, gcFk, Some(""), Some(""), Some(""), Some(""), Some(""), Some(""), Some(""), Some(""))
            case _ => DBIO.successful(0)
          }
        } yield ()
    }.toSeq

    val action = insertBatch.flatMap { batchId =>
      DBIO.seq(profileActions.map(_(batchId))*).andThen(DBIO.successful(batchId))
    }
    db.run(action.transactionally)

  override def mtExistente(sampleName: String): Future[Boolean] =
    exists(sampleName).flatMap {
      case (Some(sampleCode), _) =>
        profileRepository.get(sampleCode).map {
          case Some(profile) => profile.genotypification.get(4).isDefined
          case _             => false
        }
      case _ => Future.successful(false)
    }

  override def getProtoProfileStatus(internalCode: String): String =
    import scala.concurrent.Await
    import scala.concurrent.duration.*
    Await.result(
      db.run(Tables.protoProfiles.filter(_.sampleName === internalCode).map(_.status).result.headOption),
      10.seconds
    ).getOrElse("Unknown")