package reporting


import play.api.libs.functional.syntax._
import play.api.libs.json.Format.GenericFormat
import play.api.{Logger, Play}
import play.api.mvc.{Result, Results}

import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration  // Important: Import Play.current
import play.api.libs.json.{Json, Writes, __}

trait ProfileReportService {
  def generatePrimerReporte(): Result
  def generateProfilesReport(fechaDesde: Date, fechaHasta: Date): Result
  def generateAllProfilesReport(): Result
  def generateProfileByUser(): Future[Result]
  def generateActivesInactiveByCategory(): Future[Result]
  def generateEnviados(): Future[Result]
  def generateRecibidos(): Future[Result]
  def generateCategoriaCambio(): Future[Result]

}

@Singleton
class ProfileReportServiceImpl @Inject() (profileReportMongoRepository: ProfileReportMongoRepository, profilePostgresReportRepository: ProfileReportPostgresRepository ,pdfGen: PdfGenerator) extends ProfileReportService {  // Remove Configuration injection
  val logger = Logger(this.getClass())

  // Access configuration directly using Play.current.configuration
  val PROTOCOL = Play.current.configuration.getString("instanceInterconnection.protocol").getOrElse("http://")
  val BASE_URL = PROTOCOL + Play.current.configuration.getString("instanceInterconnection.localUrl").getOrElse("http://localhost:9000")

  override def generatePrimerReporte(): Result = {
    pdfGen.ok(views.html.document2Pdf("Texto..."), BASE_URL)
  }

  override def generateProfilesReport(fechaDesde: Date, fechaHasta: Date): Result = {
    val result = for {
      cantAltas <- profileReportMongoRepository.countProfilesCreated(Some(fechaDesde), Some(fechaHasta))
      cantBajas <- profileReportMongoRepository.countProfilesDeleted()
      cantMatches <- profileReportMongoRepository.countMatches(Some(fechaDesde), Some(fechaHasta))
      cantHit <- profileReportMongoRepository.countHit(Some(fechaDesde), Some(fechaHasta))
      cantDescartes <- profileReportMongoRepository.countDescartes(Some(fechaDesde), Some(fechaHasta))
    } yield {
      val fechaDesdeString = fechaDesde.getDate.toString + "/" + (fechaDesde.getMonth + 1).toString + "/" + (fechaDesde.getYear + 1900).toString
      val fechaHastaString = fechaHasta.getDate.toString + "/" + (fechaHasta.getMonth + 1).toString + "/" + (fechaHasta.getYear + 1900).toString
      val cantPendientes = cantMatches - cantHit - cantDescartes
      pdfGen.ok(views.html.profilesReport("Resumen de perfiles", cantAltas, cantBajas, cantMatches, cantHit, cantDescartes, cantPendientes, fechaDesdeString, fechaHastaString), BASE_URL)
    }

    Await.result(result, Duration.Inf)
  }

  def generateAllProfilesReport(): Result = {
    val result = for {
      cantAltas <- profileReportMongoRepository.countProfilesCreated()
      cantBajas <- profileReportMongoRepository.countProfilesDeleted()
      cantMatches <- profileReportMongoRepository.countMatches()
      cantHit <- profileReportMongoRepository.countHit()
      cantDescartes <- profileReportMongoRepository.countDescartes()
    } yield {
      val cantPendientes = cantMatches - cantHit - cantDescartes
      pdfGen.ok(views.html.profilesReport("Resumen de perfiles", cantAltas, cantBajas, cantMatches, cantHit, cantDescartes, cantPendientes,"",""), BASE_URL)
    }

    Await.result(result, Duration.Inf)
  }

  def generateProfileByUser(): Future[Result] = {
    profilePostgresReportRepository.cantidadPerfilesPorUsuarioyCategoriaActivosyEliminados().map { profiles =>
      implicit val tupleWrites: Writes[(String, String, Boolean, Boolean, Int)] = (
        (__ \ "username").write[String] and
          (__ \ "category").write[String] and
          (__ \ "isReference").write[Boolean] and
          (__ \ "isDeleted").write[Boolean] and
          (__ \ "int").write[Int]
        ).tupled

      val reportData = Json.obj("users" -> Json.toJson(profiles)(Writes.seq(tupleWrites)))

      pdfGen.ok(views.html.profilesReportByUser("Perfiles por usuario", reportData), BASE_URL)
    }
  }

  def generateActivesInactiveByCategory(): Future[Result] = {
    profilePostgresReportRepository.cantidadPerfilesPorCategoriaActivosyEliminados().map { profiles =>
      implicit val tupleWrites: Writes[(String, Boolean, Boolean, Int)] = (
        (__ \ "category").write[String] and
          (__ \ "isReference").write[Boolean] and
          (__ \ "isDeleted").write[Boolean] and
          (__ \ "int").write[Int]
        ).tupled

      val reportData = Json.obj("categories" -> Json.toJson(profiles)(Writes.seq(tupleWrites)))

      pdfGen.ok(views.html.profilesReportByCategory("Perfiles por categoria", reportData), BASE_URL)
    }
  }


  def generateEnviados(): Future[Result] = {
    profilePostgresReportRepository.getPerfilesEnviadosAInstanciaSuperiorPorEstado().map { profiles =>
      implicit val tupleWrites: Writes[(String, Int)] = (
        (__ \ "status").write[String] and
          (__ \ "int").write[Int]
        ).tupled

      val reportData = Json.obj("statusCounts" -> Json.toJson(profiles)(Writes.seq(tupleWrites)))

      pdfGen.ok(views.html.profilesReportUploadedByState("Perfiles enviados a instancia superior", reportData), BASE_URL)
    }
  }


  def generateRecibidos(): Future[Result] = {
    profilePostgresReportRepository.getPerfilesRecibidosDeInstanciasInferioresPorEstado().map { profiles =>
      implicit val tupleWrites: Writes[(String, String, Option[String],Option[String], Boolean, Option[String], String, String, Boolean)] = (
        (__ \ "globalCode").write[String] and
          (__ \ "labCode").write[String] and
          (__ \ "motive").write[Option[String]] and
          (__ \ "userName").write[Option[String]] and
          (__ \ "isCategoryModification").write[Boolean] and
          (__ \ "interconnectionError").write[Option[String]] and
          (__ \ "status").write[String] and
          (__ \ "category").write[String] and
          (__ \ "isReference").write[Boolean]
        ).tupled

      val reportData = Json.obj("profiles" -> Json.toJson(profiles)(Writes.seq(tupleWrites)))

      pdfGen.ok(views.html.profilesPerLabReport("Perfiles recibidos por instancia y categoría y estado", reportData), BASE_URL)
    }
  }


  def generateCategoriaCambio(): Future[Result] = {
    profilePostgresReportRepository.cantidadPerfilesPorUsuarioyCategoriaActivosyEliminados().map { profiles =>
      implicit val tupleWrites: Writes[(String, String, Boolean, Boolean, Int)] = (
        (__ \ "username").write[String] and
          (__ \ "category").write[String] and
          (__ \ "isReference").write[Boolean] and
          (__ \ "isDeleted").write[Boolean] and
          (__ \ "int").write[Int]
        ).tupled

      val reportData = Json.obj("users" -> Json.toJson(profiles)(Writes.seq(tupleWrites)))

      pdfGen.ok(views.html.profilesReportByUser("Perfiles por usuario", reportData), BASE_URL)
    }
  }

}
