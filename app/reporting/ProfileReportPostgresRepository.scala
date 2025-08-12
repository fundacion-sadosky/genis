package reporting

import models.Tables
import play.api.{Application, db}
import play.api.db.slick.Config.driver.simple.{Compiled, TableQuery, booleanColumnType, columnExtensionMethods, runnableCompiledToAppliedQueryInvoker, slickDriver, stringColumnType}
import play.api.db.slick.DB
import play.api.http.MediaRange.parse
import play.api.libs.json.{JsValue, Json}
import util.DefaultDb

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.language.postfixOps
import scala.slick.driver.PostgresDriver.simple._

abstract class ProfileReportPostgresRepository  {
  def cantidadPerfilesPorUsuarioyCategoriaActivosyEliminados(): Future[Seq[(String, String, Boolean, Boolean, Int)]]
  def cantidadPerfilesPorCategoriaActivosyEliminados(): Future[Seq[(String, Boolean , Boolean, Int)]]
  def getPerfilesEnviadosAInstanciaSuperiorPorEstado(): Future[Seq[(String, Int)]]
  def getPerfilesRecibidosDeInstanciasInferioresPorEstado(): Future[Seq[(String, String, Option[String], Option[String], Boolean, Option[String], String, String, Boolean)]]

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

  def getPerfilesRecibidosDeInstanciasInferioresPorEstado(): Future[Seq[(String, String, Option[String], Option[String], Boolean, Option[String], String, String, Boolean)]] = {

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
    val resultF: Future[Seq[(String, String, Option[String], Option[String], Boolean, Option[String], String, String, Boolean)]] =
      for {
        baseList <- baseProfilesListF
        rejectedSeq <- rejectedParsedF
        pendingSeq <- pendingParsedF
        catsMap <- categoriesMapF
      } yield {
        // Create a map for base profiles for easier lookup
        val baseMap: Map[String, (String, Option[String], Option[String], Boolean, Option[String], String, String, Boolean)] = baseList.map {
          case (gc, labCode, motiveOpt, userNameOpt, isCatMod, interError, status, catId, isRef) =>
            gc -> (labCode, motiveOpt, userNameOpt, isCatMod, interError, status, catId, isRef)
        }.toMap

        // Process rejected profiles and merge/add them to the result
        val processedRejected: Seq[(String, String, Option[String], Option[String], Boolean, Option[String], String, String, Boolean)] = rejectedSeq.map {
          case (gc, labCode, catId, rejectMotive) =>
            val isReference = catsMap.getOrElse(catId, false)
            // Check if the global code exists in the base map
            baseMap.get(gc) match {
              case Some((baseLabCode, motiveOpt, userNameOpt, isCatMod, interError, status, baseCatId, isRef)) =>
                // If it exists, merge the data
                (gc, baseLabCode, motiveOpt, userNameOpt, isCatMod, rejectMotive, status, catId, isReference)
              case None =>
                // If it doesn't exist, create a new entry
                (gc, labCode, None, None, false, rejectMotive, "REJECTED", catId, isReference)
            }
        }

        // Process pending profiles and add them to the result
        val processedPending: Seq[(String, String, Option[String], Option[String], Boolean, Option[String], String, String, Boolean)] = pendingSeq.map {
          case (gc, labCode, catId) =>
            val isReference = catsMap.getOrElse(catId, false)
            // Check if the global code exists in the base map
            baseMap.get(gc) match {
              case Some((baseLabCode, motiveOpt, userNameOpt, isCatMod, interError, status, baseCatId, isRef)) =>
                // If it exists, merge the data with "Pendientes de aprobación"
                (gc, baseLabCode, motiveOpt, userNameOpt, isCatMod, interError, "Pendientes de aprobación", catId, isReference)
              case None =>
                // If it doesn't exist, create a new entry with "Pendientes de aprobación"
                (gc, labCode, None, None, false, None, "Pendientes de aprobación", catId, isReference)
            }
        }

        // Combine base profiles, processed rejected profiles, and processed pending profiles
        val combinedResults = baseList ++ processedRejected ++ processedPending
        combinedResults
      }

    // Finally, return the Future result
    resultF
  }
}