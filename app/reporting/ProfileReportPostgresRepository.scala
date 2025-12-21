package reporting

import models.Tables
import play.api.Logger.logger
import play.api.{Application, db}
import play.api.db.slick.Config.driver.simple.{Compiled, TableQuery, booleanColumnType, columnExtensionMethods, runnableCompiledToAppliedQueryInvoker, slickDriver, stringColumnType}
import play.api.db.slick.DB
import util.DefaultDb

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.language.postfixOps
import scala.slick.driver.PostgresDriver.simple._

abstract class ProfileReportPostgresRepository  {
  def cantidadPerfilesPorUsuarioyCategoriaActivosyEliminados(): Future[Seq[(String, String, Boolean, Boolean, Int)]]
  def cantidadPerfilesPorCategoriaActivosyEliminados(): Future[Seq[(String, Boolean , Boolean, Int)]]
  def getPerfilesEnviadosAInstanciaSuperiorPorEstado(): Future[Seq[(String, Int)]]
  def getPerfilesRecibidosDeInstanciasInferioresPorEstado(): Future[Seq[(String, Seq[(String, String, Int)])]]
  def getAllProfilesListing(): Future[Seq[Tables.ProfileData#TableElementType]]
  def getAllReplicatedToSuperior(): Future[Seq[(String, String, String, String, Boolean, Option[String], Option[String],Option[String])]]
  def getAllReplicatedFromInferior(): Future[Seq[(String, String, String, Boolean, Option[String], Option[String],Option[String])]]
  def getLabCodeFromGlobalCode(globalCode: String): Option[String]
}
@Singleton
class PostgresProfileReportRepository @Inject() (implicit val app: Application) extends ProfileReportPostgresRepository with DefaultDb {

  val profileData: TableQuery[Tables.ProfileData] = Tables.ProfileData
  val profilesUploaded: TableQuery[Tables.ProfileUploaded] = Tables.ProfileUploaded
  val profilesSent: TableQuery[Tables.ProfileSent] = Tables.ProfileSent
  val profilesReceived: TableQuery[Tables.ProfileReceived] = Tables.ProfileReceived
  val profileStatus: TableQuery[Tables.InferiorInstanceProfileStatus] = Tables.InferiorInstanceProfileStatus
  val category: TableQuery[Tables.Category] = Tables.Category
  var superiorInstanceProfileApproval: TableQuery[Tables.SuperiorInstanceProfileApproval] = Tables.SuperiorInstanceProfileApproval
  val inferiorInstanceProfileStatus: TableQuery[Tables.InferiorInstanceProfileStatus] = Tables.InferiorInstanceProfileStatus


  private def queryCantidadPerfilesPorUsuarioyCategoriaActivosyEliminados(): Query[(Rep[String], Rep[String], Rep[Boolean], Rep[Boolean], Rep[Int]), (String, String, Boolean, Boolean, Int), Seq] = {
    val joinedQuery = profileData
      .join(category).on(_.category === _.id)

    // Group by assignee, category.isReference, and category.id and deleted
    val groupedQuery = joinedQuery.groupBy { case (pd, c) => (pd.assignee, c.isReference, c.id, pd.deleted) }
    // Map to compute counts
    val countQuery = groupedQuery.map {
      case ((assignee, isReference, categoryId, deleted), group) =>
        (assignee,
          categoryId,
          isReference,
          deleted,
          group.length // Count all profiles in the group (either active or deleted)
        )
    }

    val orderedQuery = countQuery
      .sortBy { case (assignee, categoryId, isRef, deleted, _) => (assignee.asc, isRef.desc, categoryId.asc, deleted.desc) }
    //group by deleted or active
    orderedQuery

  }

  val queryCantidadPerfilesPorUsuarioyCategoriaActivosyEliminadosCompiled = Compiled(queryCantidadPerfilesPorUsuarioyCategoriaActivosyEliminados())

  override def cantidadPerfilesPorUsuarioyCategoriaActivosyEliminados(): Future[Seq[(String, String, Boolean, Boolean, Int)]] = Future {
    DB(app).withSession {
      implicit session =>
        queryCantidadPerfilesPorUsuarioyCategoriaActivosyEliminadosCompiled.list
    }
  }

  private def queryCantidadPerfilesPorCategoriaActivosyEliminados(): Query[(Rep[String], Rep[Boolean], Rep[Boolean], Rep[Int]), (String, Boolean, Boolean, Int), Seq] = {
    val joinedQuery = profileData
      .join(category).on(_.category === _.id)
    // Group by  category.isReference, and category.id and deleted
    val groupedQuery = joinedQuery.groupBy { case (pd, c) => (c.isReference, c.id, pd.deleted) }

    // Map to compute counts with filters (active and deleted)
    val countQuery = groupedQuery.map {
      case ((isReference, categoryId, deleted), group) =>
        (categoryId,
          isReference,
          deleted,
          group.length // Count all profiles in the group (either active or deleted)
        )
    }

    // Order by isReference descending, then category id
    val orderedQuery = countQuery
      .sortBy { case (categoryId, isRef, _, _) => (isRef.desc, categoryId.asc) }
    orderedQuery
  }

  val queryCantidadPerfilesPorCategoriaActivosyEliminadosCompiled = Compiled(queryCantidadPerfilesPorCategoriaActivosyEliminados())

  override def cantidadPerfilesPorCategoriaActivosyEliminados(): Future[Seq[(String, Boolean, Boolean, Int)]] = Future {
    DB(app).withSession {
      implicit session =>
        queryCantidadPerfilesPorCategoriaActivosyEliminadosCompiled.list
    }
  }


  def queryProfileUploadedCountByStatus(): Query[(Rep[String], Rep[Int]), (String, Int), Seq] = {
    profilesUploaded
      .join(profileStatus).on(_.status === _.id)
      .groupBy { case (_, i) => i.status }
      .map { case (status, group) => (status, group.length) }
  }

  val queryProfileUploadedWithStatusCompiled = Compiled(queryProfileUploadedCountByStatus())

  override def getPerfilesEnviadosAInstanciaSuperiorPorEstado(): Future[Seq[(String, Int)]] = Future {
    DB(app).withSession { implicit session =>
      queryProfileUploadedWithStatusCompiled.list
    }
  }

  def getPerfilesRecibidosDeInstanciasInferioresPorEstado(): Future[Seq[(String, Seq[(String, String, Int)])]] = {

    // Step 1: Fetch all profiles received with joined statuses, categories
    val baseProfilesQuery = for {
      (((rr, s), pd), c) <- profilesReceived
        .join(profileStatus).on(_.status === _.id)
        .join(profileData).on { case ((rr, s), pd) => rr.globalCode === pd.globalCode }
        .join(category).on { case (((rr, s), pd), c) => pd.category === c.id }
    } yield (
      rr.globalCode,
      rr.labCode,
      rr.motive,
      rr.userName,
      rr.isCategoryModification,
      rr.interconnectionError,
      s.status,
      c.id,
      c.isReference
    )

    val queryBaseProfilesQuery = Compiled(baseProfilesQuery)

    val baseProfilesListF: Future[Seq[(String, String, Option[String], Option[String], Boolean, Option[String], String, String, Boolean)]] = Future {
      DB(app).withSession { implicit session =>
        queryBaseProfilesQuery.list
      }
    }

    // Step 2: Fetch rejected profiles with rejectionUser
    val rejectedProfilesQuery = superiorInstanceProfileApproval
      .filter(_.rejectionUser.isDefined)
      .map(sipa => (sipa.globalCode, sipa.laboratory, sipa.profile, sipa.rejectMotive))

    val queryRejectedProfilesQuery = Compiled(rejectedProfilesQuery)
    val rejectedProfilesF: Future[Seq[(String, String, String, Option[String])]] = Future {
      DB(app).withSession { implicit session =>
        queryRejectedProfilesQuery.list
      }
    }

    // Step 3: Fetch pending approval profiles (rejectionUser is empty)
    val pendingProfilesQuery = superiorInstanceProfileApproval
      .filter(_.rejectionUser.isEmpty)
      .map(sipa => (sipa.globalCode, sipa.laboratory, sipa.profile))

    val queryPendingProfilesQuery = Compiled(pendingProfilesQuery)
    val pendingProfilesF: Future[Seq[(String, String, String)]] = Future {
      DB(app).withSession { implicit session =>
        queryPendingProfilesQuery.list
      }
    }

    def extractCategoryId(jsonStr: String): Option[String] = {
      val key = "\"categoryId\""
      val idx = jsonStr.indexOf(key)
      if (idx >= 0) {
        val startIdx = jsonStr.indexOf("\"", idx + key.length)
        if (startIdx >= 0) {
          val endIdx = jsonStr.indexOf("\"", startIdx + 1)
          if (endIdx > startIdx) {
            val value = jsonStr.substring(startIdx + 1, endIdx)
            Some(value)
          } else None
        } else None
      } else None
    }

    // Step 4: Extract categoryId from profile JSON string (without circe)
    val rejectedParsedF: Future[Seq[(String, String, String, Option[String])]] = rejectedProfilesF.map { seq =>
      seq.map { case (globalCode, labCode, profileJsonStr, rejectMotive) =>
        val catIdOpt = extractCategoryId(profileJsonStr)
        (globalCode, labCode, catIdOpt.getOrElse(""), rejectMotive)
      }
    }

    // Step 5: Extract categoryId from pending profiles JSON string
    val pendingParsedF: Future[Seq[(String, String, String)]] = pendingProfilesF.map { seq =>
      seq.map { case (globalCode, labCode, profileJsonStr) =>
        val catIdOpt = extractCategoryId(profileJsonStr)
        (globalCode, labCode, catIdOpt.getOrElse(""))
      }
    }

    // Step 6: Load categories for isReference info
    val categoriesMapF: Future[Map[String, Boolean]] = Future {
      DB(app).withSession { implicit session =>
        category.map(c => (c.id, c.isReference)).list.toMap
      }
    }

    // Step 7: Combine all data and produce the final result
    val resultF: Future[Seq[(String, Seq[(String, String, Int)])]] =
      for {
        baseList <- baseProfilesListF
        rejectedSeq <- rejectedParsedF
        pendingSeq <- pendingParsedF
        catsMap <- categoriesMapF
      } yield {
        // CORRECTION HERE:
        // 1. Match the raw values coming from the DB (gc, dbLabCode, ...)
        // 2. Calculate the 'resolvedLabCode' inside the block using your helper method
        val baseMap: Map[String, (String, Option[String], Option[String], Boolean, Option[String], String, String, Boolean)] = baseList.map {
          case (gc, dbLabCode, motiveOpt, userNameOpt, isCatMod, interError, status, catId, isRef) =>

            // Logic: Try to extract from GlobalCode. If it fails or returns None, fallback to the DB value.
            val resolvedLabCode = getLabCodeFromGlobalCode(gc).getOrElse(dbLabCode)

            gc -> (resolvedLabCode, motiveOpt, userNameOpt, isCatMod, interError, status, catId, isRef)
        }.toMap

        // Process rejected profiles (No changes needed here, just ensuring it uses baseMap correctly)
        val processedRejected: Seq[(String, String, Option[String], Option[String], Boolean, Option[String], String, String, Boolean)] = rejectedSeq.map {
          case (gc, labCode, catId, rejectMotive) =>
            val isReference = catsMap.getOrElse(catId, false)
            baseMap.get(gc) match {
              case Some((baseLabCode, motiveOpt, userNameOpt, isCatMod, interError, status, baseCatId, isRef)) =>
                (gc, baseLabCode, motiveOpt, userNameOpt, isCatMod, rejectMotive, status, catId, isReference)
              case None =>
                // Resolve lab code here as well for consistency
                val resolvedLabCode = getLabCodeFromGlobalCode(gc).getOrElse(labCode)
                (gc, resolvedLabCode, None, None, false, rejectMotive, "Rechazado en esta instancia", catId, isReference)
            }
        }

        // Process pending profiles (No changes needed here)
        val processedPending: Seq[(String, String, Option[String], Option[String], Boolean, Option[String], String, String, Boolean)] = pendingSeq.map {
          case (gc, labCode, catId) =>
            val isReference = catsMap.getOrElse(catId, false)
            baseMap.get(gc) match {
              case Some((baseLabCode, motiveOpt, userNameOpt, isCatMod, interError, status, baseCatId, isRef)) =>
                (gc, baseLabCode, motiveOpt, userNameOpt, isCatMod, interError, "Pendiente de aprobación", catId, isReference)
              case None =>
                val resolvedLabCode = getLabCodeFromGlobalCode(gc).getOrElse(labCode)
                (gc, resolvedLabCode, None, None, false, None, "Pendiente de aprobación", catId, isReference)
            }
        }

        // Combine base profiles, processed rejected profiles, and processed pending profiles
        val combinedResults = baseList.map {
          // We need to remap baseList to use the resolvedLabCode as well to match the structure
          case (gc, dbLabCode, motiveOpt, userNameOpt, isCatMod, interError, status, catId, isRef) =>
            val resolvedLabCode = getLabCodeFromGlobalCode(gc).getOrElse(dbLabCode)
            (gc, resolvedLabCode, motiveOpt, userNameOpt, isCatMod, interError, status, catId, isRef)
        } ++ processedRejected ++ processedPending

        // Deduplicate: If a profile exists in multiple lists (e.g. Received AND Rejected), take the one with the most specific status
        // The logic below prioritizes the last occurrence if we simply group.
        // However, usually "Pending" or "Rejected" from the Approval table is more recent than "Received".
        // Since we check `baseMap.get` inside processedRejected/Pending, those lists contain the merged info.

        // Strategy: Group by GlobalCode and pick the "best" one.
        // Here we assume processedRejected/Pending have precedence.
        val uniqueMap = scala.collection.mutable.Map[String, (String, String, Option[String], Option[String], Boolean, Option[String], String, String, Boolean)]()

        combinedResults.foreach { item =>
          // Simple overwrite logic: Later items in the list overwrite earlier ones.
          // Order in combinedResults: Base ++ Rejected ++ Pending.
          // This means Pending overwrites Rejected which overwrites Base.
          uniqueMap.put(item._1, item)
        }

        val deduplicatedResults = uniqueMap.values.toSeq

        deduplicatedResults
          .groupBy(_._2) // Group by labCode (Element 2)
          .map { case (labCode, seq) =>
            // Group by Category (Element 8) and Status (Element 7)
            val subGrouped = seq.groupBy(t => (t._8, t._7))

            val subSeq = subGrouped.map { case ((category, status), innerSeq) =>
              (category, status, innerSeq.size)
            }.toSeq

            (labCode, subSeq.sortBy(_._1))
          }
          .toSeq.sortBy(_._1)
      }

    resultF
  }

  def getLabCodeFromGlobalCode(globalCode: String): Option[String] = {
    val parts = globalCode.split("-")
    if (parts.length >= 4) {
      Some(parts(2))
    } else {
      None  // Or handle the case where the globalCode doesn't have the expected format
    }
  }

  def getAllProfilesListing(): Future[Seq[Tables.ProfileData#TableElementType]] = Future {
    DB(app).withSession { implicit session =>
      profileData.list
    }
  }


  def getAllReplicatedToSuperior(): Future[Seq[(String, String, String, String, Boolean, Option[String], Option[String],Option[String])]] = Future {
    DB(app).withSession { implicit session =>
      profilesUploaded
        .join(inferiorInstanceProfileStatus).on(_.status === _.id)
        .join(profileData).on { case ((pu, sip), pd) => pu.globalCode === pd.globalCode }
        .map { case (((pu, sip), pd)) =>
          (pu.globalCode, pd.category, pd.internalCode, sip.status, pd.deleted, pu.userName, pd.deletedSolicitor, pd.deletedMotive)
        }
        .list
    }
  }

  def getAllReplicatedFromInferior(): Future[Seq[(String, String, String, Boolean, Option[String], Option[String],Option[String])]] = Future {
    DB(app).withSession { implicit session =>
      profilesReceived
        .join(inferiorInstanceProfileStatus).on(_.status === _.id)
        .join(profileData).on { case ((pr, sip), pd) => pr.globalCode === pd.globalCode }
        .map { case (((pr, sip), pd)) =>
          (pr.globalCode, pd.category, sip.status, pd.deleted, pr.userName, pd.deletedSolicitor, pd.deletedMotive)
        }
        .list
    }
  }

}