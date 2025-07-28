import models.Tables
import play.api.Application
import scala.concurrent.Future
import scala.language.postfixOps
import javax.inject.Inject
import javax.inject.Singleton
import models.Tables
import play.api.Application
import scala.slick.driver.PostgresDriver.simple._
import play.api.db.slick.Config.driver.simple.Column
import play.api.db.slick.Config.driver.simple.Compiled
import play.api.db.slick.Config.driver.simple.TableQuery
import play.api.db.slick.Config.driver.simple.booleanColumnExtensionMethods
import play.api.db.slick.Config.driver.simple.booleanColumnType
import play.api.db.slick.Config.driver.simple.columnExtensionMethods
import play.api.db.slick.Config.driver.simple.queryToInsertInvoker
import play.api.db.slick.Config.driver.simple.runnableCompiledToAppliedQueryInvoker
import play.api.db.slick.Config.driver.simple.slickDriver
import play.api.db.slick.Config.driver.simple.stringColumnType
import play.api.db.slick.DB
import types.Email
import util.DefaultDb

abstract class ProfilePostgresReportRepository  {
  def cantidadPerfilesPorCategoriaActivosyEliminados(): Future[Seq[(String, Boolean , Int, Int)]]

}
@Singleton
class SlickProfileReportRepository @Inject() (implicit val app: Application) extends ProfilePostgresReportRepository with DefaultDb  {

  val profileData: TableQuery[Tables.ProfileData] = Tables.ProfileData
  val profilesUploaded: TableQuery[Tables.ProfileUploaded] = Tables.ProfileUploaded
  val profilesSent: TableQuery[Tables.ProfileSent] = Tables.ProfileSent
  val profilesReceived: TableQuery[Tables.ProfileReceived] = Tables.ProfileReceived
  val category: TableQuery[Tables.Category] = Tables.Category


  private def queryCantidadPerfilesPorCategoriaActivosyEliminados(): Query[(Rep[String], Rep[Boolean], Rep[Int], Rep[Int]), (String, Boolean, Int, Int), Seq] = {
    val joinedQuery = profileData
      .join(category).on(_.category === _.id)
    // Group by category.isReference and category.id
    val groupedQuery = joinedQuery
      .groupBy { case (_, c) => (c.isReference, c.id) }

    // Map to compute counts with filters (active and deleted)
    val countQuery = groupedQuery.map {
      case ((isReference, categoryId), group) =>
        (
          categoryId,
          isReference,
          group.filter(_._1.deleted === false).length,
          group.filter(_._1.deleted === true).length
        )
    }

    // Order by isReference descending, then category id
    val orderedQuery = countQuery
      .sortBy { case (categoryId, isRef, _, _) => (isRef.desc, categoryId.asc) }
    orderedQuery
  }

  val queryCantidadPerfilesPorCategoriaActivosyEliminadosCompiled = Compiled(queryCantidadPerfilesPorCategoriaActivosyEliminados())

  override def cantidadPerfilesPorCategoriaActivosyEliminados(): Future[Seq[(String, Boolean, Int, Int)]] = Future {
    DB(app).withSession {
      implicit session =>
        queryCantidadPerfilesPorCategoriaActivosyEliminadosCompiled.list
    }
  }
}