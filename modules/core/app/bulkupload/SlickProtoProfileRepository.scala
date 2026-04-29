package bulkupload

import configdata.{CategoryService, MatchingRule}
import jakarta.inject.{Inject, Named, Singleton}
import kits.StrKitLocus
import models.Tables
import models.Tables._
import play.api.Logging
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.Json
import profile.{Profile, ProfileRepository}
import search.PaginationSearch
import slick.jdbc.GetResult
import slick.jdbc.PostgresProfile.api._
import types.{AlphanumericId, SampleCode}

import java.sql.Timestamp
import java.util.Date
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class SlickProtoProfileRepository @Inject()(
    db: Database,
    categoryService: CategoryService,
    messagesApi: MessagesApi,
    profileRepository: ProfileRepository,
    @Named("protoProfileGcDummy") ppGcD: String
)(implicit ec: ExecutionContext) extends ProtoProfileRepository with Logging {

  private val messages = messagesApi.preferred(Seq(Lang("es")))

  // ---------------------------------------------------------------------------
  // Row → domain conversion
  // ---------------------------------------------------------------------------

  private def row2protoProfile(row: ProtoProfileRow): ProtoProfile = {
    val geno = Json.fromJson[ProtoProfile.Genotypification](Json.parse(row.genotypifications)).get
    val se: Seq[String] = row.errors.fold(Seq.empty[String])(_.split(";").toSeq)
    val mr = Json.fromJson[Seq[MatchingRule]](Json.parse(row.matchingRules)).get
    val mm = Json.fromJson[Profile.Mismatch](Json.parse(row.mismatchs)).get
    ProtoProfile(
      row.id, row.sampleName, row.assignee, row.category,
      ProtoProfileStatus.withName(row.status),
      row.panel, geno, mm, mr, se, row.genemapperLine,
      preexistence = row.preexistence.map(SampleCode(_)),
      rejectMotive = row.rejectMotive
    )
  }

  // ---------------------------------------------------------------------------
  // Compiled queries (Slick 3.5 DSL)
  // ---------------------------------------------------------------------------

  private val queryCheckAssigne = Compiled { (globalCode: Rep[String], assigne: Rep[String]) =>
    Tables.profilesData.filter(pd => pd.globalCode === globalCode && pd.assignee === assigne).map(_.category)
  }


  private val queryGetRejectMotive = Compiled { (id: Rep[Long]) =>
    Tables.protoProfiles.filter(_.id === id).map(pp => (pp.rejectMotive, pp.rejectionUser, pp.idRejectMotive, pp.rejectionDate))
  }

  private def queryGetProtoProfilesBase(batchId: Long) =
    Tables.protoProfiles.filter(_.idBatch === batchId)

  private def queryGetProtoProfilesStep2Base(batchId: Long, user: String, isSuperUser: Boolean) = {
    val base = Tables.protoProfiles.filter { pp =>
      pp.idBatch === batchId &&
      (pp.status inSet Seq("Approved", "Rejected", "Imported", "Uploaded", "DesktopSearch", "ReplicatedMatchingProfile"))
    }
    if (isSuperUser) base else base.filter(_.assignee === user)
  }

  private val queryGetProtoProfilesBatch = Compiled { (batchId: Rep[Long]) =>
    Tables.batchProtoProfiles.filter(_.id === batchId)
  }

  private val queryGetProtoProfile = Compiled { (id: Rep[Long]) =>
    Tables.protoProfiles.filter(_.id === id)
  }

  private val queryUpdateStatusProtoProfile = Compiled { (id: Rep[Long]) =>
    Tables.protoProfiles.filter(_.id === id).map(_.status)
  }

  private val queryUpdateMrMm = Compiled { (id: Rep[Long]) =>
    Tables.protoProfiles.filter(_.id === id).map(pp => (pp.matchingRules, pp.mismatchs))
  }

  private val queryUpdateDataProtoProfile = Compiled { (id: Rep[Long]) =>
    Tables.protoProfiles.filter(_.id === id).map(_.category)
  }

  private val queryUpdateDataProtoProfileData = Compiled { (globalCode: Rep[String]) =>
    Tables.stashProfileData.filter(_.globalCode === globalCode).map(_.category)
  }

  private val queryGetByKit = Compiled { (id: Rep[String]) =>
    Tables.protoProfiles.filter { pp =>
      pp.panel === id && (pp.status inSet Seq("ReadyForApproval", "Approved", "Incomplete", "Imported"))
    }
  }

  // ---------------------------------------------------------------------------
  // GetResult implicits for plain SQL
  // ---------------------------------------------------------------------------

  private implicit val getBatchStep1: GetResult[(Long, String, java.sql.Date, Long, Long, Long, Long, Option[String], Long, Long, String)] =
    GetResult(r => (r.nextLong(), r.nextString(), r.nextDate(), r.nextLong(), r.nextLong(), r.nextLong(), r.nextLong(), r.nextStringOption(), r.nextLong(), r.nextLong(), r.nextString()))

  private implicit val getBatchStep2: GetResult[(Long, String, java.sql.Date, Long, Option[String], Long, String, Long)] =
    GetResult(r => (r.nextLong(), r.nextString(), r.nextDate(), r.nextLong(), r.nextStringOption(), r.nextLong(), r.nextString(), r.nextLong()))

  private implicit val getBatchModal: GetResult[(Long, String, java.sql.Date)] =
    GetResult(r => (r.nextLong(), r.nextString(), r.nextDate()))

  // ---------------------------------------------------------------------------
  // Trait implementation
  // ---------------------------------------------------------------------------

  override def createBatch(
    user: String,
    protoProfileStream: LazyList[ProtoProfile],
    laboratory: String,
    kits: Map[String, List[StrKitLocus]],
    label: Option[String],
    analysisType: String
  ): Future[Long] = {
    val distinctCategories = protoProfileStream.map(_.category).distinct.toList
    Future.sequence(distinctCategories.map(cat => categoryService.getCategory(AlphanumericId(cat)).map(cat -> _)))
      .flatMap { categoriesSeq =>
        val categoryMap = categoriesSeq.toMap
        val batchRow = BatchProtoProfileRow(0, user, new java.sql.Date(new Date().getTime), label, analysisType)
        val insertBatch = (Tables.batchProtoProfiles returning Tables.batchProtoProfiles.map(_.id)) += batchRow
        val profileActions = protoProfileStream.toList.map { protoProfile =>
          if (!kits.contains(protoProfile.kit)) throw KitNotExistsException(protoProfile.kit)
          val joinLociGen = for {
            locus <- kits(protoProfile.kit)
            g <- protoProfile.genotypification
            if g.locus == locus.id
          } yield (locus.order, g)
          val sortedGen = joinLociGen.sortBy(_._1).map(_._2)
          val jsonGenotypification = Json.toJson(sortedGen).toString
          val matchingRules = Json.toJson(protoProfile.matchingRules).toString
          val mismatches = Json.toJson(protoProfile.mismatches).toString
          val errors = if (protoProfile.errors.nonEmpty)
            Some(protoProfile.errors.reduce((prev, rest) => prev.concat(s";$rest")))
          else None
          (batchId: Long) => {
            val pprow = ProtoProfileRow(
              0, protoProfile.sampleName, batchId, protoProfile.assignee,
              protoProfile.category, protoProfile.status.toString,
              protoProfile.kit, errors, jsonGenotypification, matchingRules,
              mismatches, None, protoProfile.preexistence.map(_.text),
              protoProfile.geneMapperLine
            )
            val filiationDataRequired = categoryMap.get(protoProfile.category).flatten.exists(_.filiationDataRequired)
            for {
              pdFK <- (Tables.protoProfiles returning Tables.protoProfiles.map(_.id)) += pprow
              gcFk = s"$ppGcD$pdFK"
              _ <- Tables.stashProfileData += ProfileDataRow(
                id = 0,
                category = protoProfile.category,
                globalCode = gcFk,
                internalCode = protoProfile.sampleName,
                internalSampleCode = protoProfile.sampleName,
                assignee = protoProfile.assignee,
                laboratory = laboratory,
                deleted = false
              )
              _ <- if (filiationDataRequired)
                Tables.stashProfileDataFiliation += ProfileDataFiliationRow(
                  0, gcFk, Some(""), Some(""),
                  Some(new java.sql.Date(-2208988800L)),
                  Some(""), Some(""), Some(""), Some(""), Some("")
                )
              else DBIO.successful(0)
            } yield ()
          }
        }

        val action = for {
          batchId <- insertBatch
          _ <- DBIO.sequence(profileActions.map(_(batchId)))
        } yield batchId

        db.run(action.transactionally)
      }
  }

  override def getBatch(batchId: Long): Future[Option[ProtoProfilesBatch]] =
    db.run(queryGetProtoProfilesBatch(batchId).result.headOption)
      .map(_.map(bpp => ProtoProfilesBatch(bpp.id, bpp.user, bpp.date, bpp.label)))

  override def getBatchesStep1(userId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]] =
    db.run(
      sql"""SELECT bpp."ID", bpp."USER", bpp."DATE", sum(1) as "TOTAL",
            sum(CASE WHEN pp."STATUS" = 'Imported' THEN 1 ELSE 0 END) as "APPROVED",
            sum(CASE WHEN pp."STATUS" IN ('Incomplete','ReadyForApproval','Approved') THEN 1 ELSE 0 END) as "PENDING",
            sum(CASE WHEN pp."STATUS" IN ('Disapproved','Invalid','Rejected') THEN 1 ELSE 0 END) as "REJECTED",
            bpp."LABEL",
            sum(CASE WHEN pp."STATUS" = 'ReadyForApproval' THEN 1 ELSE 0 END) as "APPROVEDSTEP1",
            sum(CASE WHEN pp."STATUS" = 'Incomplete' THEN 1 ELSE 0 END) as "INCOMPLETESTEP1",
            bpp."ANALYSISTYPE"
            FROM "APP"."BATCH_PROTO_PROFILE" bpp
            INNER JOIN "APP"."PROTO_PROFILE" pp ON pp."ID_BATCH" = bpp."ID"
            WHERE (bpp."USER" = $userId OR $isSuperUser)
            GROUP BY bpp."ID"
            ORDER BY bpp."ID" DESC
            LIMIT $limit OFFSET $offset""".as[(Long, String, java.sql.Date, Long, Long, Long, Long, Option[String], Long, Long, String)]
    ).map(_.map {
      case (id, user, date, total, appr, pend, rejected, label, approvedStep1, incompleteStep1, analysisType) =>
        ProtoProfilesBatchView(id, user, date, total.toInt, appr.toInt, pend.toInt, rejected.toInt, label, approvedStep1.toInt, incompleteStep1.toInt, analysisType)
    })

  override def countBatchesStep1(userId: String, isSuperUser: Boolean): Future[Int] =
    db.run(
      sql"""SELECT COUNT(*) FROM "APP"."BATCH_PROTO_PROFILE" bpp
            WHERE (bpp."USER" = $userId OR $isSuperUser)""".as[Int].head
    )

  override def getBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]] =
    db.run(
      sql"""SELECT bpp."ID", bpp."USER", bpp."DATE",
            (SELECT COUNT(*) FROM "APP"."PROTO_PROFILE" pp
             WHERE pp."ID_BATCH" = bpp."ID"
             AND (pp."ASSIGNEE" = $geneMapperId OR $isSuperUser)
             AND pp."STATUS" IN ('Approved','Rejected','Imported','Uploaded','DesktopSearch','ReplicatedMatchingProfile')
            ) as "TOTAL",
            bpp."LABEL",
            (SELECT COUNT(*) FROM "APP"."PROTO_PROFILE" pp
             WHERE pp."ID_BATCH" = bpp."ID"
             AND (pp."ASSIGNEE" = $geneMapperId OR $isSuperUser)
             AND pp."STATUS" = 'Approved'
            ) as "TOTAL_APPROVED",
            bpp."ANALYSISTYPE",
            (SELECT COUNT(*) FROM "APP"."PROTO_PROFILE" pp WHERE pp."ID_BATCH" = bpp."ID") as "BATCH_TOTAL"
            FROM "APP"."BATCH_PROTO_PROFILE" bpp
            WHERE EXISTS (
              SELECT 1 FROM "APP"."PROTO_PROFILE" pp
              WHERE pp."ID_BATCH" = bpp."ID"
              AND (pp."ASSIGNEE" = $geneMapperId OR $isSuperUser)
              AND pp."STATUS" IN ('Approved','Rejected','Imported','Uploaded','DesktopSearch','ReplicatedMatchingProfile')
            )
            GROUP BY bpp."ID"
            ORDER BY bpp."ID" DESC
            LIMIT $limit OFFSET $offset""".as[(Long, String, java.sql.Date, Long, Option[String], Long, String, Long)]
    ).map(_.map {
      case (id, user, date, total, label, totalApproved, analysisType, batchTotal) =>
        ProtoProfilesBatchView(id, user, date, total.toInt, 0, 0, 0, label, totalApproved.toInt, 0, analysisType, batchTotal.toInt)
    })

  override def countBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean): Future[Int] =
    db.run(
      sql"""SELECT COUNT(DISTINCT bpp."ID")
            FROM "APP"."BATCH_PROTO_PROFILE" bpp
            WHERE EXISTS (
              SELECT 1 FROM "APP"."PROTO_PROFILE" pp
              WHERE pp."ID_BATCH" = bpp."ID"
              AND (pp."ASSIGNEE" = $geneMapperId OR $isSuperUser)
              AND pp."STATUS" IN ('Approved','Rejected','Imported','Uploaded','ReplicatedMatchingProfile')
            )""".as[Int].head
    )

  override def getProtoProfilesStep1(batchId: Long, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]] = {
    val base = queryGetProtoProfilesBase(batchId).sortBy(pp => (pp.errors.isDefined.desc, pp.id.asc))
    val action = paginationSearch match {
      case Some(ps) => base.drop(ps.page * ps.pageSize).take(ps.pageSize).result
      case None     => base.result
    }
    db.run(action).map(_.map(row2protoProfile))
  }

  override def getProtoProfilesStep2(batchId: Long, userId: String, isSuperUser: Boolean, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]] = {
    val base = queryGetProtoProfilesStep2Base(batchId, userId, isSuperUser).sortBy(pp => (pp.errors.isDefined.desc, pp.id.asc))
    val action = paginationSearch match {
      case Some(ps) => base.drop(ps.page * ps.pageSize).take(ps.pageSize).result
      case None     => base.result
    }
    db.run(action).map(_.map(row2protoProfile))
  }

  override def getProtoProfile(id: Long): Future[Option[ProtoProfile]] =
    db.run(queryGetProtoProfile(id).result.headOption).map(_.map(row2protoProfile))

  override def getProtoProfileWithBatchId(id: Long): Future[Option[(ProtoProfile, Long)]] =
    db.run(queryGetProtoProfile(id).result.headOption).map(_.map(row => (row2protoProfile(row), row.idBatch)))

  override def updateProtoProfileStatus(id: Long, status: ProtoProfileStatus.Value): Future[Int] =
    db.run(queryUpdateStatusProtoProfile(id).update(status.toString).transactionally)

  override def updateProtoProfileStatus(internalCode: String, status: String): Future[Int] =
    db.run(Tables.protoProfiles.filter(_.sampleName === internalCode).map(_.status).update(status).transactionally)

  override def updateProtoProfileData(id: Long, cat: AlphanumericId): Future[Int] =
    for {
      ppOpt    <- getProtoProfile(id)
      catOpt   <- categoryService.getCategory(cat)
      result   <- ppOpt.fold(Future.successful(0)) { pp =>
        val gc = s"$ppGcD${pp.id}"
        val filiationRequired = catOpt.exists(_.filiationDataRequired)
        val filiationAction: DBIO[Int] =
          if (filiationRequired)
            Tables.stashProfileDataFiliation.filter(_.profileData === gc).delete >>
            (Tables.stashProfileDataFiliation += ProfileDataFiliationRow(
              0, gc, Some(""), Some(""),
              Some(new java.sql.Date(-62135758800000L)),
              Some(""), Some(""), Some(""), Some(""), Some("")
            ))
          else DBIO.successful(0)
        db.run((for {
          ret <- queryUpdateDataProtoProfile(id).update(cat.text)
          _   <- queryUpdateDataProtoProfileData(gc).update(cat.text)
          _   <- filiationAction
        } yield ret).transactionally)
      }
    } yield result

  override def updateProtoProfileMatchingRulesMismatch(id: Long, matchingRules: Seq[MatchingRule], mismatches: Profile.Mismatch): Future[Int] = {
    val mRules = Json.toJson(matchingRules).toString
    val mis = Json.toJson(mismatches).toString
    db.run(queryUpdateMrMm(id).update((mRules, mis)).transactionally)
  }

  override def exists(sampleName: String): Future[(Option[SampleCode], Option[Long])] =
    db.run(
      sql"""SELECT 'PROFILE_ID', pd."GLOBAL_CODE"
            FROM "APP"."PROFILE_DATA" pd
            WHERE pd."INTERNAL_SAMPLE_CODE" = $sampleName AND pd."DELETED" = false
            UNION ALL
            SELECT 'BATCH_ID', to_char(pp."ID_BATCH", '999999999999')
            FROM "APP"."PROTO_PROFILE" pp
            WHERE pp."SAMPLE_NAME" = $sampleName
            AND pp."STATUS" IN ('ReadyForApproval', 'Approved', 'Incomplete')
        """.as[(String, String)]
    ).map { rows =>
      rows.foldLeft[(Option[SampleCode], Option[Long])]((None, None)) { (acc, b) =>
        b._1.trim match {
          case "PROFILE_ID" => (Some(SampleCode(b._2)), acc._2)
          case "BATCH_ID"   => (acc._1, Some(b._2.trim.toLong))
          case _            => acc
        }
      }
    }

  override def validateAssigneAndCategory(globalCode: SampleCode, assigne: String, category: Option[AlphanumericId] = None): Future[Option[String]] =
    db.run(queryCheckAssigne(globalCode.text, assigne).result.headOption).map {
      case None      => Some(messages("error.E0662", assigne))
      case Some(cat) => category.flatMap { cty =>
        if (cat == cty.text) None
        else Some(messages("error.E0663", cat, cty.text))
      }
    }

  override def hasProfileDataFiliation(id: Long): Future[Boolean] = {
    def isComplete(row: ProfileDataFiliationRow): Boolean =
      row.fullName.exists(_.nonEmpty) && row.nickname.exists(_.nonEmpty) &&
      row.birthday.isDefined && row.birthPlace.exists(_.nonEmpty) &&
      row.nationality.exists(_.nonEmpty) && row.identification.exists(_.nonEmpty) &&
      row.identificationIssuingAuthority.exists(_.nonEmpty) && row.address.exists(_.nonEmpty)

    val stashFilQ = for {
      pp  <- Tables.protoProfiles if pp.id === id
      pd  <- Tables.stashProfileData if pd.id === pp.id
      pdfil <- Tables.stashProfileDataFiliation if pdfil.profileData === pd.globalCode
    } yield pdfil

    val appFilQ = for {
      pp  <- Tables.protoProfiles if pp.id === id
      pd  <- Tables.profilesData if pd.internalSampleCode === pp.sampleName
      pdfil <- Tables.profileDataFiliations if pdfil.profileData === pd.globalCode
    } yield pdfil

    for {
      stashFil <- db.run(stashFilQ.result.headOption)
      appFil   <- db.run(appFilQ.result.headOption)
    } yield stashFil.exists(isComplete) || appFil.exists(isComplete)
  }

  override def setRejectMotive(id: Long, motive: String, rejectionUser: String, idMotive: Long, timeRejected: Timestamp): Future[Int] =
    db.run(queryGetRejectMotive(id).update((Some(motive), Some(rejectionUser), Some(idMotive), Some(timeRejected))).transactionally)

  override def canDeleteKit(id: String): Future[Boolean] =
    db.run(queryGetByKit(id).result).map(_.isEmpty)

  override def deleteBatch(id: Long): Future[Either[String, Long]] = {
    val protoIds = Tables.protoProfiles.filter(_.idBatch === id).map(_.id)
    val stashPdQ = Tables.stashProfileData.filter(_.id in protoIds)
    val stashFilQ = Tables.stashProfileDataFiliation.filter(_.profileData in stashPdQ.map(_.globalCode))

    val deleteAction =
      stashFilQ.delete >>
      stashPdQ.delete >>
      Tables.protoProfiles.filter(_.idBatch === id).delete >>
      Tables.batchProtoProfiles.filter(_.id === id).delete

    db.run(deleteAction.transactionally)
      .map(_ => Right(id))
      .recover { case e: Exception =>
        logger.error(s"Error deleting batch $id", e)
        Left(messages("error.E0304"))
      }
  }

  override def countImportedProfilesByBatch(idBatch: Long): Future[Either[String, Long]] =
    db.run(
      sql"""SELECT COUNT(*) FROM "APP"."PROTO_PROFILE" WHERE "ID_BATCH" = $idBatch AND "STATUS" = 'Imported'""".as[Long].head
    ).map(Right(_)).recover { case e: Exception =>
      logger.error(s"Error counting imported profiles for batch $idBatch", e)
      Left(messages("error.E0304"))
    }

  override def countAllProtoProfilesInBatch(batchId: Long): Future[Int] =
    db.run(Tables.protoProfiles.filter(_.idBatch === batchId).length.result)

  override def getBatchSearchModalViewByIdOrLabel(input: String, idCase: Long): Future[List[BatchModelView]] =
    db.run(
      sql"""SELECT bpp."ID", bpp."LABEL", bpp."DATE"
            FROM "APP"."BATCH_PROTO_PROFILE" bpp
            INNER JOIN "APP"."PROTO_PROFILE" pp ON pp."ID_BATCH" = bpp."ID"
            WHERE pp."STATUS" = 'Imported'
            AND (LOWER(bpp."LABEL") LIKE LOWER('%'||$input||'%') OR CAST(bpp."ID" AS varchar(50)) LIKE $input)
            GROUP BY bpp."ID" ORDER BY bpp."ID" DESC""".as[(Long, String, java.sql.Date)]
    ).map { rows =>
      val sdf = new java.text.SimpleDateFormat("dd/MM/yyyy")
      rows.toList.map { case (id, label, date) => BatchModelView(id, Some(label), sdf.format(date)) }
    }

  override def getSearchBachLabelID(userId: String, isSuperUser: Boolean, filter: String): Future[Seq[ProtoProfilesBatchView]] =
    db.run(
      sql"""SELECT bpp."ID", bpp."USER", bpp."DATE", sum(1) as "TOTAL",
            sum(CASE WHEN pp."STATUS" = 'Imported' THEN 1 ELSE 0 END) as "APPROVED",
            sum(CASE WHEN pp."STATUS" IN ('Incomplete','ReadyForApproval','Approved','DesktopSearch') THEN 1 ELSE 0 END) as "PENDING",
            sum(CASE WHEN pp."STATUS" IN ('Disapproved','Invalid','Rejected') THEN 1 ELSE 0 END) as "REJECTED",
            bpp."LABEL",
            sum(CASE WHEN pp."STATUS" = 'ReadyForApproval' THEN 1 ELSE 0 END) as "APPROVEDSTEP1",
            sum(CASE WHEN pp."STATUS" = 'Incomplete' THEN 1 ELSE 0 END) as "INCOMPLETESTEP1",
            bpp."ANALYSISTYPE"
            FROM "APP"."BATCH_PROTO_PROFILE" bpp
            INNER JOIN "APP"."PROTO_PROFILE" pp ON pp."ID_BATCH" = bpp."ID"
            WHERE (bpp."USER" = $userId OR $isSuperUser)
            AND (LOWER(bpp."LABEL") LIKE LOWER('%'||$filter||'%') OR CAST(bpp."ID" AS varchar(50)) LIKE $filter)
            GROUP BY bpp."ID"
            ORDER BY bpp."ID" DESC""".as[(Long, String, java.sql.Date, Long, Long, Long, Long, Option[String], Long, Long, String)]
    ).map(_.map {
      case (id, user, date, total, appr, pend, rejected, label, approvedStep1, incompleteStep1, analysisType) =>
        ProtoProfilesBatchView(id, user, date, total.toInt, appr.toInt, pend.toInt, rejected.toInt, label, approvedStep1.toInt, incompleteStep1.toInt, analysisType)
    })

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
    Await.result(
      db.run(Tables.protoProfiles.filter(_.sampleName === internalCode).map(_.status).result.headOption),
      Duration(3, SECONDS)
    ).getOrElse("Unknown")
}
