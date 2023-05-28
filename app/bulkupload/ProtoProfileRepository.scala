package bulkupload

import java.util.Date

import scala.concurrent.Future
import javax.inject.Inject
import javax.inject.Singleton

import profile.{Profile, ProfileRepository}
import models.Tables
import models.Tables.BatchProtoProfileRow
import models.Tables.ProtoProfileRow
import types.SampleCode
import play.api.{Application, Logger}
import play.api.libs.json.Json

import scala.slick.jdbc.{StaticQuery => Q}
import Q.interpolation
import scala.slick.jdbc.StaticQuery.staticQueryToInvoker
import scala.slick.driver.PostgresDriver.simple._
import play.api.db.slick.Config.driver.simple.Column
import play.api.db.slick.Config.driver.simple.Compiled
import play.api.db.slick.Config.driver.simple.TableQuery
import play.api.db.slick.Config.driver.simple.booleanColumnExtensionMethods
import play.api.db.slick.Config.driver.simple.booleanColumnType
import play.api.db.slick.Config.driver.simple.columnExtensionMethods
import play.api.db.slick.Config.driver.simple.longColumnType
import play.api.db.slick.Config.driver.simple.queryToAppliedQueryInvoker
import play.api.db.slick.Config.driver.simple.queryToInsertInvoker
import play.api.db.slick.Config.driver.simple.runnableCompiledToAppliedQueryInvoker
import play.api.db.slick.Config.driver.simple.slickDriver
import play.api.db.slick.Config.driver.simple.stringColumnType
import play.api.db.slick.Config.driver.simple.valueToConstColumn
import play.api.db.slick.DB
import play.api.db.slick.DBAction
import models.Tables.ProfileDataRow
import javax.inject.Named

import play.api.i18n.Messages
import models.Tables.ProfileDataFiliationRow
import types.AlphanumericId
import profiledata.ProfileDataAttempt
import profiledata.DataFiliationAttempt
import configdata.{CategoryService, MatchingRule}
import kits.StrKitLocus
import util.{DefaultDb, Transaction}
import search.PaginationSearch

import scala.slick.driver.PostgresDriver

abstract class ProtoProfileRepository  extends DefaultDb with Transaction {

  def createBatch(user: String, protoProfileStream: Stream[ProtoProfile], laboratory: String, kits: Map[String, List[StrKitLocus]], label: Option[String], analysisType: String): Future[Long]
  def getBatch(batchId: Long): Future[Option[ProtoProfilesBatch]]
  def getBatchesStep1(userId: String, isSuperUser: Boolean): Future[Seq[ProtoProfilesBatchView]]
  def getBatchesStep2(userId: String, geneMapperId : String,  isSuperUser: Boolean): Future[Seq[ProtoProfilesBatchView]]
  def getProtoProfilesStep1(batchId: Long, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]]
  def getProtoProfilesStep2(batchId: Long, userId: String, isSuperUser: Boolean, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]]
  def getProtoProfile(id: Long): Future[Option[ProtoProfile]]
  def getProtoProfileWithBatchId(id: Long): Future[Option[(ProtoProfile, Long)]]
  def updateProtoProfileStatus(id: Long, status: ProtoProfileStatus.Value): Future[Int]
  def updateProtoProfileData(id: Long, cat: AlphanumericId): Future[Int]
  def updateProtoProfileMatchingRulesMismatch(id: Long, matchingRules: Seq[MatchingRule], mismatches: Profile.Mismatch): Future[Int]
  def exists(sampleName: String): Future[(Option[SampleCode], Option[Long])]
  def validateAssigneAndCategory(globalCode: SampleCode, assigne: String, category: Option[AlphanumericId]): Future[Option[String]]
  def hasProfileDataFiliation(id: Long): Future[Boolean]
  def setRejectMotive(id: Long, motive: String,rejectionUser:String,idMotive:Long,timeRejected:java.sql.Timestamp): Future[Int]
  def canDeleteKit(id: String): Future[Boolean]
  def deleteBatch(id: Long):Future[Either[String,Long]]
  def countImportedProfilesByBatch(idBatch: Long):Future[Either[String,Long]]
  def getSearchBachLabelID(userId: String, isSuperUser: Boolean, filter: String) : Future[Seq[ProtoProfilesBatchView]]
  def getBatchSearchModalViewByIdOrLabel(input:String,idCase:Long):Future[List[BatchModelView]]
  def mtExistente(sampleName:String ) : Future[Boolean]
}

@Singleton
class SlickProtoProfileRepository @Inject() (
    categoryService: CategoryService,
    @Named("protoProfileGcDummy") val ppGcD: String,
    implicit val app: Application,
    profileRepository: ProfileRepository = null) extends ProtoProfileRepository with DefaultDb {

  val protoProfiles: TableQuery[Tables.ProtoProfile] = Tables.ProtoProfile
  val batchProtoProfiles: TableQuery[Tables.BatchProtoProfile] = Tables.BatchProtoProfile
  val profilesData: TableQuery[Tables.ProfileData] = Tables.ProfileData
  val protoProfileData: TableQuery[Tables.ProfileData] = new TableQuery(tag => new Tables.ProfileData(tag, Some("STASH"), "PROFILE_DATA"))
  val protoProfileDataFil: TableQuery[Tables.ProfileDataFiliation] = new TableQuery(tag => new Tables.ProfileDataFiliation(tag, Some("STASH"), "PROFILE_DATA_FILIATION"))
  val protoProfileDataApp: TableQuery[Tables.ProfileData] = new TableQuery(tag => new Tables.ProfileData(tag, Some("APP"), "PROFILE_DATA"))
  val protoProfileDataFilApp: TableQuery[Tables.ProfileDataFiliation] = new TableQuery(tag => new Tables.ProfileDataFiliation(tag, Some("APP"), "PROFILE_DATA_FILIATION"))

  val logger = Logger(this.getClass())

  private def protoProfileRow2protoProfile(row: ProtoProfileRow) = {
    val geno = Json.fromJson[ProtoProfile.Genotypification](Json.parse(row.genotypifications)).get

    val se: Seq[String] = row.errors.fold(Seq[String]())(errs => errs.split(";"))

    val mr = Json.fromJson[Seq[MatchingRule]](Json.parse(row.matchingRules)).get
    val mm = Json.fromJson[Profile.Mismatch](Json.parse(row.mismatchs)).get

    ProtoProfile(
      row.id, row.sampleName,
      row.assignee, row.category,
      ProtoProfileStatus.withName(row.status),
      row.panel, geno, mm, mr, se, row.genemapperLine, preexistence = row.preexistence.map(SampleCode(_)), rejectMotive = row.rejectMotive)
  }

  private def queryDefineCheckAssigne(globalCode: Column[String], assigne: Column[String]) = for {
    pd <- profilesData if (pd.globalCode === globalCode && pd.assignee === assigne)
  } yield (pd.category)

  val queryCheckAssigne = Compiled(queryDefineCheckAssigne _)

  private def queryDefineGetRejectMotive(id: Column[Long]) = for {
    pp <- protoProfiles if (pp.id === id)
  } yield (pp.rejectMotive,pp.rejectionUser,pp.idRejectMotive,pp.rejectionDate)

  val queryGetRejectMotive = Compiled(queryDefineGetRejectMotive _)

  private def queryDefineGetProtoProfiles(batchId: Column[Long]) = for {
    pp <- protoProfiles if (pp.idBatch === batchId)
  } yield (pp)

  val queryGetProtoProfiles = Compiled(queryDefineGetProtoProfiles _)

  private def queryDefineGetProtoProfilesStep2(batchId: Column[Long], user: Column[String], isSuperUser: Column[Boolean]) = for {
    pp <- protoProfiles if ((pp.idBatch === batchId) && (isSuperUser || pp.assignee === user) && (pp.status === "Approved" || pp.status === "Rejected" || pp.status === "Imported"))
  } yield (pp)

  val queryGetProtoProfilesStep2 = Compiled(queryDefineGetProtoProfilesStep2 _)

  private def queryDefineGetProtoProfilesBatch(batchId: Column[Long]) = for {
    bpp <- batchProtoProfiles if (bpp.id === batchId)
  } yield (bpp)

  val queryGetProtoProfilesBatch = Compiled(queryDefineGetProtoProfilesBatch _)

  private def queryDefineGetFullProtoProfile(id: Column[Long]) = for {
    ((pp, ppd), ppdf) <- protoProfiles leftJoin protoProfileData on (_.id === _.id) leftJoin protoProfileDataFil on (_._2.globalCode === _.profileData)
    if (pp.id === id)
  } yield (pp, ppd.?, ppdf.?)

  private def queryDefineGetFullProtoProfileApp(id: Column[Long]) = for {
    ((pp, ppd), ppdf) <- protoProfiles leftJoin protoProfileDataApp on (_.sampleName === _.internalCode) leftJoin protoProfileDataFilApp on (_._2.globalCode === _.profileData)
    if (pp.id === id)
  } yield (pp, ppd.?, ppdf.?)

  val queryGetFullProtoProfile = Compiled(queryDefineGetFullProtoProfile _)

  val queryGetFullProtoProfileApp = Compiled(queryDefineGetFullProtoProfileApp _)

  private def queryDefineGetProtoProfile(id: Column[Long]) = for {
    pp <- protoProfiles if (pp.id === id)
  } yield (pp)

  val queryGetProtoProfile = Compiled(queryDefineGetProtoProfile _)

  private def queryDefineUpdateStatusProtoProfile(id: Column[Long]) = for {
    pp <- protoProfiles if (pp.id === id)
  } yield (pp.status)

  val queryUpdateStatusProtoProfile = Compiled(queryDefineUpdateStatusProtoProfile _)

  private def queryDefineUpdateMrMm(id: Column[Long]) = for {
    pp <- protoProfiles if (pp.id === id)
  } yield (pp.matchingRules, pp.mismatchs)

  val queryUpdateMrMm = Compiled(queryDefineUpdateMrMm _)

  private def queryDefineUpdateDataProtoProfile(id: Column[Long]) = for {
    pp <- protoProfiles if (pp.id === id)
  } yield (pp.category)

  val queryUpdateDataProtoProfile = Compiled(queryDefineUpdateDataProtoProfile _)

  private def queryDefineUpdateDataProtoProfileData(globalCode: Column[String]) = for {
    pp <- protoProfileData if (pp.globalCode === globalCode)
  } yield (pp.category)

  val queryUpdateDataProtoProfileData = Compiled(queryDefineUpdateDataProtoProfileData _)

  val queryGetBatchesStep1 = Q.query[(String, Boolean), (Long, String, java.sql.Date, Long, Long, Long, Long, Option[String], Long, Long, String)](
                          """SELECT bpp."ID", bpp."USER", bpp."DATE", sum(1) as "TOTAL",
                          sum(CASE WHEN pp."STATUS" = 'Imported' THEN 1 ELSE 0 END) as "APPROVED",
                          sum(CASE
                            WHEN pp."STATUS" = 'Incomplete' THEN 1
                            WHEN pp."STATUS" = 'ReadyForApproval' THEN 1
                            WHEN pp."STATUS" = 'Approved' THEN 1
                            ELSE 0 END) as "PENDING",
                          sum(CASE
                            WHEN pp."STATUS" = 'Disapproved' THEN 1
                            WHEN pp."STATUS" = 'Invalid' THEN 1
                            WHEN pp."STATUS" = 'Rejected' THEN 1
                            ELSE 0 END) as "REJECTED",
                            bpp."LABEL",
                          sum(CASE
                            WHEN pp."STATUS" = 'ReadyForApproval' THEN 1
                            ELSE 0 END) as "APPROVEDSTEP1",
                          sum(CASE
                             WHEN pp."STATUS" = 'Incomplete' THEN 1
                             ELSE 0 END) as "INCOMPLETESTEP1",
                          bpp."ANALYSISTYPE"
                          FROM "APP"."BATCH_PROTO_PROFILE" bpp inner join "APP"."PROTO_PROFILE" pp
                          on (pp."ID_BATCH" = bpp."ID") WHERE (bpp."USER" = ? OR ?)
                          GROUP BY bpp."ID"
                          ORDER BY bpp."ID" DESC""")

  val queryGetBatchesStep2 = Q.query[(String, Boolean, String, Boolean), (Long, String, java.sql.Date, Long, Option[String], Long, String)](
                          """SELECT bpp."ID", bpp."USER", bpp."DATE", (
                            	SELECT COUNT(*) as "TOTAL" FROM "APP"."PROTO_PROFILE" pp
                            	WHERE pp."ID_BATCH" = bpp."ID"
                            	AND (pp."ASSIGNEE" = ? OR ?)
                            	AND pp."STATUS" IN ('Approved','Rejected','Imported')
                            ) as "TOTAL",
                            bpp."LABEL", (
                            SELECT COUNT(*) as "TOTAL_APPROVED" FROM "APP"."PROTO_PROFILE" pp
                              WHERE pp."ID_BATCH" = bpp."ID"
                              AND (pp."ASSIGNEE" = ? OR ?)
                              AND pp."STATUS" = 'Approved'
                            ) as "TOTAL_APPROVED",
                            bpp."ANALYSISTYPE"
                            FROM "APP"."BATCH_PROTO_PROFILE" bpp
                            GROUP BY bpp."ID"
                            ORDER BY bpp."ID" DESC""")

  val queryGetBatchesForModal = Q.query[(String,String), (Long, String, java.sql.Date)](
    """SELECT bpp."ID",  bpp."LABEL",bpp."DATE"
        FROM "APP"."BATCH_PROTO_PROFILE" bpp inner join "APP"."PROTO_PROFILE" pp
        on (pp."ID_BATCH" = bpp."ID")
        WHERE pp."STATUS" = 'Imported'
          AND (LOWER(bpp."LABEL") LIKE LOWER('%'||?||'%') or CAST(bpp."ID" AS varchar(50)) LIKE ?)
        GROUP BY bpp."ID" ORDER BY bpp."ID" DESC """)

  val queryGetSearchBatchesLabelID = Q.query[(String, Boolean, String, String), (Long, String, java.sql.Date, Long, Long, Long, Long, Option[String],Long, Long, String)](
    """SELECT bpp."ID", bpp."USER", bpp."DATE", sum(1) as "TOTAL",
                          sum(CASE WHEN pp."STATUS" = 'Imported' THEN 1 ELSE 0 END) as "APPROVED",
                          sum(CASE
                            WHEN pp."STATUS" = 'Incomplete' THEN 1
                            WHEN pp."STATUS" = 'ReadyForApproval' THEN 1
                            WHEN pp."STATUS" = 'Approved' THEN 1
                            ELSE 0 END) as "PENDING",
                          sum(CASE
                            WHEN pp."STATUS" = 'Disapproved' THEN 1
                            WHEN pp."STATUS" = 'Invalid' THEN 1
                            WHEN pp."STATUS" = 'Rejected' THEN 1
                            ELSE 0 END) as "REJECTED",
                            bpp."LABEL",
                          sum(CASE
                            WHEN pp."STATUS" = 'ReadyForApproval' THEN 1
                            ELSE 0 END) as "APPROVEDSTEP1",
                          sum(CASE
                              WHEN pp."STATUS" = 'Incomplete' THEN 1
                              ELSE 0 END) as "INCOMPLETESTEP1",
                          bpp."ANALYSISTYPE"
                          FROM "APP"."BATCH_PROTO_PROFILE" bpp inner join "APP"."PROTO_PROFILE" pp
                          on (pp."ID_BATCH" = bpp."ID") WHERE (bpp."USER" = ? OR ?)
                          AND (LOWER(bpp."LABEL") LIKE LOWER('%'||?||'%') or CAST(bpp."ID" AS varchar(50)) LIKE ?)
                          GROUP BY bpp."ID"
                          ORDER BY bpp."ID" DESC""")



  private def queryGetsIdsProtoProfilesByIdBatch(id: Column[Long]) = protoProfiles.filter(_.idBatch === id).map(_.id)
  private def queryGetProfileDataByIdBatch(id: Column[Long]) =  protoProfileData.filter(_.id in queryGetsIdsProtoProfilesByIdBatch(id))
  private def queryGetProfileDataFiliationByGlobalCodes(id: Column[Long]) = protoProfileDataFil.filter(_.profileData in queryGetProfileDataByIdBatch(id).map(_.globalCode))
  private def queryGetProfileDataFiliationByProfileData(id: Column[String]) = protoProfileDataFil.filter(_.profileData === id)

  val getDataFiliationByIdBatch = Compiled(queryGetProfileDataFiliationByGlobalCodes _)
  val getGetProfileDataByIdBatch = Compiled(queryGetProfileDataByIdBatch _)
  val getGetProfileDataFiliationByProfileData = Compiled(queryGetProfileDataFiliationByProfileData _)

  def countImported(idBatch: Long) = sql"""select COUNT(*) from "APP"."PROTO_PROFILE" where "ID_BATCH" = $idBatch and "STATUS" = 'Imported'""".as[Int]

  override def hasProfileDataFiliation(id: Long): Future[Boolean] = Future {
    DB.withSession { implicit session =>
      logger.debug(s"id: ${id}")
      val (pp, ppd, ppdf) = queryGetFullProtoProfile(id).first

      val unexistentProfile = ppdf.fold(false)(f =>
        f.fullName != "" && f.nickname != "" &&
          f.birthday != "" && f.birthPlace != "" &&
          f.nationality != "" && f.identification != "" &&
          f.identificationIssuingAuthority != "" &&
          f.address != "")

      val (ppApp, ppdApp, ppdfApp) = queryGetFullProtoProfileApp(id).first

      val existentProfile = ppdfApp.fold(false)(f =>
        f.fullName != "" && f.nickname != "" &&
          f.birthday != "" && f.birthPlace != "" &&
          f.nationality != "" && f.identification != "" &&
          f.identificationIssuingAuthority != "" &&
          f.address != "")

      unexistentProfile || existentProfile
    }
  }

  override def getProtoProfilesStep2(batchId: Long, user: String, isSuperUser: Boolean, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]] = Future {
    DB.withSession { implicit session =>
      var query = queryDefineGetProtoProfilesStep2(batchId, user, isSuperUser).sortBy(x=>(x.errors.isDefined.desc,(x.status==="Approved").desc,x.id.asc))
      if (paginationSearch.isDefined) {
        query = query.drop(paginationSearch.get.page * paginationSearch.get.pageSize)
          .take(paginationSearch.get.pageSize)
      }
      query.iterator.toVector map { protoProfileRow2protoProfile(_) }
    }
  }

  override def getBatch(batchId: Long): Future[Option[ProtoProfilesBatch]] = Future {
    DB.withSession { implicit session =>
      queryGetProtoProfilesBatch(batchId).firstOption map { bpp => ProtoProfilesBatch(bpp.id, bpp.user, bpp.date, bpp.label) }
    }
  }

  override def getProtoProfilesStep1(batchId: Long, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]] = Future {
    DB.withSession { implicit session =>
      var query = queryDefineGetProtoProfiles(batchId).sortBy(x=>(x.errors.isDefined.desc,x.id.asc))
        if (paginationSearch.isDefined) {
          query = query.drop(paginationSearch.get.page * paginationSearch.get.pageSize)
                       .take(paginationSearch.get.pageSize)
        }
        query.iterator.toVector map { protoProfileRow2protoProfile(_) }
    }
  }

  override def getProtoProfile(id: Long): Future[Option[ProtoProfile]] = Future {
    DB.withSession { implicit session =>
      queryGetProtoProfile(id).firstOption map {
        pp =>
          val geno = Json.fromJson[ProtoProfile.Genotypification](Json.parse(pp.genotypifications)).get

          val se: Seq[String] = pp.errors.fold(Seq[String]())(errs => errs.split(";"))

          val mr = Json.fromJson[Seq[MatchingRule]](Json.parse(pp.matchingRules)).get
          val mm = Json.fromJson[Profile.Mismatch](Json.parse(pp.mismatchs)).get

          ProtoProfile(
            pp.id, pp.sampleName,
            pp.assignee, pp.category,
            ProtoProfileStatus.withName(pp.status),
            pp.panel, geno, mm, mr, se, pp.genemapperLine, pp.preexistence.map(SampleCode(_)), None)

      }
    }
  }

  override def getProtoProfileWithBatchId(id: Long): Future[Option[(ProtoProfile, Long)]] = Future {
    DB.withSession { implicit session =>
      queryGetProtoProfile(id).firstOption map { row =>
        (protoProfileRow2protoProfile(row), row.idBatch)
      }
    }
  }

  override def updateProtoProfileStatus(id: Long, status: ProtoProfileStatus.Value): Future[Int] = Future {
    DB.withTransaction { implicit session =>
      queryUpdateStatusProtoProfile(id).update(status.toString())
    }
  }

  override def updateProtoProfileData(id: Long, cat: AlphanumericId): Future[Int] = {
    this.getProtoProfile(id) map { pp =>
      DB.withTransaction { implicit session =>
        val ret = queryUpdateDataProtoProfile(id).update(cat.text)
        val gc = ppGcD + pp.get.id
        queryUpdateDataProtoProfileData(gc).update(cat.text)

        categoryService.getCategory(cat).foreach { catres =>
          if (catres.filiationDataRequired) {
            getGetProfileDataFiliationByProfileData(gc).delete
            protoProfileDataFil += ProfileDataFiliationRow(0, gc, Some(""), Some(""), Some(new java.sql.Date(-62135758800000L)), Some(""), Some(""), Some(""), Some(""), Some(""))
          }
        }
        ret
      }
    }
  }

  override def getBatchesStep1(userId: String, isSuperUser: Boolean): Future[Seq[ProtoProfilesBatchView]] = Future {
    DB.withSession { implicit session =>
      queryGetBatchesStep1(userId, isSuperUser).list map {
        case (id, user, date, total, appr, pend, rejected, label, approvedstep1, incompletestep1,analysisType) =>
          ProtoProfilesBatchView(id, user, date, total.toInt, appr.toInt, pend.toInt, rejected.toInt, label, approvedstep1.toInt, incompletestep1.toInt,analysisType)
      }
    }
  }

  override def getBatchesStep2(userId: String, geneMapperId : String, isSuperUser: Boolean): Future[Seq[ProtoProfilesBatchView]] = Future {
    DB.withSession { implicit session =>
      queryGetBatchesStep2(geneMapperId, isSuperUser, geneMapperId, isSuperUser).list.filter(_._4 > 0). map {
        case (id, user, date, total, label, totalApproved, analysisType) =>
          ProtoProfilesBatchView(id, user, date, total.toInt, 0, 0, 0, label, totalApproved.toInt,0,analysisType)
      }
    }
  }

  override def exists(sampleName: String): Future[(Option[SampleCode], Option[Long])] = Future {
    DB.withSession { implicit session =>

      val toChar = SimpleFunction.binary[Long, String, String]("to_char")

      val profileIdQuery = for {
        pd <- profilesData if (pd.internalSampleCode === sampleName && !pd.deleted)
      } yield ("PROFILE_ID", pd.globalCode)

      val batchIdQuery = for {
        pp <- protoProfiles if (
          pp.sampleName === sampleName &&
          (pp.status inSet List("ReadyForApproval", "Approved", "Incomplete")))
      } yield ("BATCH_ID", toChar(pp.idBatch, "999999999999"))

      val existsQuery = profileIdQuery ++ batchIdQuery

      existsQuery.list.foldLeft[(Option[SampleCode], Option[Long])]((None, None))((a, b) => b._1 match {
        case "PROFILE_ID" => (Some(SampleCode(b._2)), a._2)
        case "BATCH_ID"   => (a._1, Some(b._2.trim.toLong))
      })

    }
  }

  override def createBatch(user: String, protoProfileStream: Stream[ProtoProfile], laboratory: String, kits: Map[String, List[StrKitLocus]], label : Option[String], analysisType: String): Future[Long] = Future {

    DB.withTransaction { implicit session =>

      val batchRow = BatchProtoProfileRow(0, user, new Date(), label, analysisType)

      val batchId = (batchProtoProfiles returning batchProtoProfiles.map(_.id)) += batchRow

      protoProfileStream.foreach { protoProfile =>
        val joinLociGen = for {
          locus <- kits(protoProfile.kit)
          g <- protoProfile.genotypification
          if (g.locus == locus.id)
        } yield (locus.order, g)

        val sortedGen = joinLociGen.sortBy(_._1).map(_._2)

        val jsonGenotypification = Json.toJson(sortedGen).toString
        val matchingRules = Json.toJson(protoProfile.matchingRules).toString
        val mismatches = Json.toJson(protoProfile.mismatches).toString

        val errors = if (!protoProfile.errors.isEmpty)
          Some(protoProfile.errors.reduce { (prev, rest) => prev.concat(";" + rest) })
        else
          None

        val pprow = ProtoProfileRow(
          0, protoProfile.sampleName,
          batchId, protoProfile.assignee,
          protoProfile.category,
          protoProfile.status.toString, protoProfile.kit,
          errors, jsonGenotypification, matchingRules, mismatches,
          None, protoProfile.preexistence.map(_.text), protoProfile.geneMapperLine)
        val pdFK = protoProfiles returning (protoProfiles.map { _.id }) += pprow

        val gcFk = ppGcD + pdFK
        val stashp = ProfileDataRow(
          0, protoProfile.category, gcFk, protoProfile.sampleName,
          None, None, None, None, None, None, None,
          protoProfile.sampleName, protoProfile.assignee, laboratory,
          None, None, None, None, false, None, None)

        val res = protoProfileData += stashp

        categoryService.getCategory(AlphanumericId(protoProfile.category)).foreach { cat =>
          if (cat.filiationDataRequired) {
            protoProfileDataFil += ProfileDataFiliationRow(0, gcFk, Some(""), Some(""), Some(new java.sql.Date(-2208988800L)), Some(""), Some(""), Some(""), Some(""), Some(""))
          }
        }
        res
      }
      batchId
    }
  }

  override def validateAssigneAndCategory(globalCode: SampleCode, assigne: String, category: Option[AlphanumericId] = None): Future[Option[String]] = Future {
    DB.withSession { implicit session =>
      queryCheckAssigne((globalCode.text, assigne)).firstOption match {
        case None => Some(Messages("error.E0662",assigne ))
        case Some(cat) => category.flatMap(cty =>
          if (cat == cty.text)
            None
          else
            Some(Messages("error.E0663" ,cat ,cty.text)))
      }
    }
  }

  override def updateProtoProfileMatchingRulesMismatch(id: Long, matchingRules: Seq[MatchingRule], mismatches: Profile.Mismatch): Future[Int] = {
    DB.withTransaction { implicit session =>
      val mRules = Json.toJson(matchingRules).toString
      val mis = Json.toJson(mismatches).toString
      Future.successful(queryUpdateMrMm(id).update((mRules, mis)))
    }
  }

  override def setRejectMotive(id: Long, motive: String,rejectionUser:String,idRejectMotive:Long,rejectionDate:java.sql.Timestamp): Future[Int] = Future {
    DB.withTransaction { implicit session =>
      queryGetRejectMotive(id).update((Option(motive),Option(rejectionUser),Option(idRejectMotive),Option(rejectionDate)))
    }
  }

  val queryGetByKit = Compiled { id: Column[String] =>
    for {
      pp <- protoProfiles
      if pp.panel === id && (pp.status inSet List("ReadyForApproval", "Approved", "Incomplete", "Imported"))
    } yield pp
  }

  override def canDeleteKit(id: String): Future[Boolean] = Future {
    DB.withSession { implicit session =>
      queryGetByKit(id).list.isEmpty
    }
  }

  private def delete(id: Long)(implicit session: PostgresDriver.simple.Session): Either[String,Long] =  {
    try{

      getDataFiliationByIdBatch(id).delete
      getGetProfileDataByIdBatch(id).delete
      queryGetProtoProfiles(id).delete
      val result = queryGetProtoProfilesBatch(id).delete
      Right(id)
    }catch{
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }
  override def deleteBatch(id: Long):Future[Either[String,Long]] = {

    this.runInTransactionAsync { implicit session => this.delete(id) };

  }

  def countImportedProfilesByBatch(idBatch: Long):Future[Either[String,Long]] = {
    this.runInTransactionAsync { implicit session => {
      try{
        val count = countImported(idBatch).first
         Right(count)
      }catch{
        case e: Exception => {
          e.printStackTrace()
          Left(e.getMessage)
        }
      }
    }};
  }

  def getBatchSearchModalViewByIdOrLabel(input:String,idCase:Long):Future[List[BatchModelView]] = Future{
    DB.withSession { implicit session =>
        queryGetBatchesForModal(input,input).list map {
          case (id, label, date) =>{
            val sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            BatchModelView(id, Some(label),sdf.format(date))
          }
        }
    };
  }

  override def getSearchBachLabelID(userId: String, isSuperUser: Boolean, search :String): Future[Seq[ProtoProfilesBatchView]] = Future {
   DB.withSession { implicit session =>
      queryGetSearchBatchesLabelID(userId,isSuperUser,search, search).list map {
        case (id, user, date, total, appr, pend, rejected, label, approvedStep1, incompleteStep1, analysisType) =>
          ProtoProfilesBatchView(id, user, date, total.toInt, appr.toInt, pend.toInt, rejected.toInt, label, approvedStep1.toInt, incompleteStep1.toInt,analysisType)
      }
    }
  }

  def mtExistente(sampleName:String ) : Future[Boolean]  = {

  this.exists(sampleName).flatMap{
      case (Some(sampleCode),_) => {
               this.profileRepository.get(sampleCode).map{
        case Some(profile) => {
                 profile.genotypification.get(4).isDefined }
        case _ => false
      } }
      case _ => Future.successful(false)
    }

  }



}