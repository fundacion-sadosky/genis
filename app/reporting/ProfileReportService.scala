package reporting


import models.Tables
import play.api.libs.functional.syntax._
import play.api.libs.json.Format.GenericFormat
import play.api.{Logger, Play}
import play.api.mvc.{Result, Results}

import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import play.api.libs.json.{Json, Writes, __}

import scala.util.matching.Regex.MatchData

trait ProfileReportService {
  def generatePrimerReporte(): Result
  def generateProfilesReport(fechaDesde: Date, fechaHasta: Date): Result
  def generateAllProfilesReport(): Result
  def generateProfileByUser(): Future[Result]
  def generateActivesInactiveByCategory(): Future[Result]
  def generateEnviados(): Future[Result]
  def generateRecibidos(): Future[Result]
  def generateCategoriaCambio(): Future[Result]
  def generateAllProfilesList(): Future[Result]
  def generateAllMatchesList(): Future[Result]
  def generateAllReplicatedToSuperiorList(): Future[Result]
  def generateAllReplicatedFromInferiorList(): Future[Result]
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
    profilePostgresReportRepository.getPerfilesRecibidosDeInstanciasInferioresPorEstado().map { groupedData =>
      // Define Writes for the inner tuple (category, status, count)
      implicit val innerWrites: Writes[(String, String, Int)] = (
        (__ \ "category").write[String] and
          (__ \ "status").write[String] and
          (__ \ "count").write[Int]
        ).tupled

      // Define Writes for the outer tuple (labCode, Seq[(category, status, count)])
      implicit val outerWrites: Writes[(String, Seq[(String, String, Int)])] = (
        (__ \ "labCode").write[String] and
          (__ \ "details").write[Seq[(String, String, Int)]](Writes.seq(innerWrites))
        ).tupled

      // Create the JSON object
      val reportData = Json.obj("labs" -> Json.toJson(groupedData)(Writes.seq(outerWrites)))

      // Generate the PDF report
      pdfGen.ok(views.html.profilesPerLabReport("Perfiles recibidos por instancia, categoría y estado", reportData), BASE_URL)
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


  val CsvProfileHeader: List[String] = List(
    "ID", "CATEGORY", "GLOBAL_CODE", "INTERNAL_CODE", "DESCRIPTION", "ATTORNEY",
    "BIO_MATERIAL_TYPE", "COURT", "CRIME_INVOLVED", "CRIME_TYPE", "CRIMINAL_CASE",
    "INTERNAL_SAMPLE_CODE", "ASSIGNEE", "LABORATORY", "PROFILE_EXPIRATION_DATE",
    "RESPONSIBLE_GENETICIST", "SAMPLE_DATE", "SAMPLE_ENTRY_DATE", "DELETED",
    "DELETED_SOLICITOR", "DELETED_MOTIVE",
    "ANALYSIS_DATE", "ANALYSIS_KIT"
  )


  def mapProfileToCsvRow(
    profile: Tables.ProfileData#TableElementType,
    analysisInfo: Option[(String, String)]): String = {
    val (analysisDate, analysisKit) = analysisInfo.getOrElse(("", ""))
    List(
      profile.id,
      profile.category,
      profile.globalCode,
      profile.internalCode,
      profile.description.getOrElse(""),
      profile.attorney.getOrElse(""),
      profile.bioMaterialType,
      profile.court.getOrElse(""),
      profile.crimeInvolved.getOrElse(""),
      profile.crimeType.getOrElse(""),
      profile.criminalCase.getOrElse(""),
      profile.internalSampleCode,
      profile.assignee,
      profile.laboratory,
      profile.profileExpirationDate.map(_.toString).getOrElse(""),
      profile.responsibleGeneticist.getOrElse(""),
      profile.sampleDate.map(_.toString).getOrElse(""),
      profile.sampleEntryDate.map(_.toString).getOrElse(""),
      profile.deleted,
      profile.deletedSolicitor.getOrElse(""),
      profile.deletedMotive,
      analysisDate,
      analysisKit
    ).mkString(",")
  }



  def generateAllProfilesList(): Future[Result] = {
    profilePostgresReportRepository.getAllProfilesListing().flatMap { profiles =>
      val globalCodes = profiles.map(_.globalCode)

      profileReportMongoRepository
        .getFirstAnalysisInfoByGlobalCodes(globalCodes)
        .map { analysisMap =>
          val csvRows = profiles.map { p =>
            val info = analysisMap.get(p.globalCode)
            mapProfileToCsvRow(p, info)
          }
          val csvContent = (CsvProfileHeader.mkString(",") +: csvRows).mkString("\n")
          Results.Ok(csvContent).as("text/csv")
        }
    }
  }


  val CsvMatchHeader = List("DATE","GLOBAL_CODE1", "CATEGORY1", "ASSIGNEE1", "STATUS1","GLOBAL_CODE2", "CATEGORY2", "ASSIGNEE2", "STATUS2", "STRINGENCY")

  def generateAllMatchesList(): Future[Result] = {
    profileReportMongoRepository.getAllMatches.map { matches =>
      val csvRows = matches.map { matchData =>
        List(
          matchData.date, // Date
          matchData.globalCode1,          // GLOBAL_CODE1
          matchData.category1,          // CATEGORY1
          matchData.assignee1,          // ASSIGNEE1
          matchData.status1,          // STATUS1
          matchData.globalCode2,          // GLOBAL_CODE2
          matchData.category2,          // CATEGORY2
          matchData.assignee2,          // ASSIGNEE2
          matchData.status2,          // STATUS2
          matchData.stringency         // STRINGENCY
        ).mkString(",")
      }
      val csvContent = (CsvMatchHeader.mkString(",") +: csvRows).mkString("\n")
      Results.Ok(csvContent).as("text/csv")
    }
  }

  val csvReplicatedToSuperior : List[String] = List(
     "GLOBAL_CODE", "CATEGORY","INTERNAL_CODE", "STATUS", "DELETED", "USER","DELETED_SOLICITOR", "DELETED_MOTIVE","ANALYSIS_DATE", "ANALYSIS_KIT"
  )

  def generateAllReplicatedToSuperiorList(): Future[Result] = {
    profilePostgresReportRepository.getAllReplicatedToSuperior().flatMap { profiles =>
      val globalCodes = profiles.map(_._1) // (globalCode, category, ...)

      profileReportMongoRepository
        .getFirstAnalysisInfoByGlobalCodes(globalCodes)
        .map { analysisMap =>
          val csvRows = profiles.map {
            case (globalCode, category, internalCode, status, deleted, user, deletedSolicitor, deletedMotive) =>
              val info = analysisMap.get(globalCode).getOrElse(("", ""))
              val (analysisDate, analysisKit) = info

              List(
                globalCode,
                category,
                internalCode,
                status.toString,
                deleted.toString,
                user.getOrElse(""),
                deletedSolicitor.getOrElse(""),
                deletedMotive.getOrElse(""),
                analysisDate,
                analysisKit
              ).mkString(",")
          }
          val csvContent = (csvReplicatedToSuperior.mkString(",") +: csvRows).mkString("\n")
          Results.Ok(csvContent).as("text/csv")
        }
    }
  }

  val csvReplicatedFromInferior : List[String] = List(
    "GLOBAL_CODE", "CATEGORY", "STATUS", "DELETED", "USER","DELETED_SOLICITOR", "DELETED_MOTIVE", "ANALYSIS_DATE", "ANALYSIS_KIT"
  )

  def generateAllReplicatedFromInferiorList(): Future[Result] = {
    profilePostgresReportRepository.getAllReplicatedFromInferior().flatMap { profiles =>
      val globalCodes = profiles.map(_._1)

      profileReportMongoRepository
        .getFirstAnalysisInfoByGlobalCodes(globalCodes)
        .map { analysisMap =>
          val csvRows = profiles.map {
            case (globalCode, category, status, deleted, user, deletedSolicitor, deletedMotive) =>
              val info = analysisMap.get(globalCode).getOrElse(("", ""))
              val (analysisDate, analysisKit) = info

              List(
                globalCode,
                category,
                status.toString,
                deleted.toString,
                user.getOrElse(""),
                deletedSolicitor.getOrElse(""),
                deletedMotive.getOrElse(""),
                analysisDate,
                analysisKit
              ).mkString(",")
          }
          val csvContent = (csvReplicatedFromInferior.mkString(",") +: csvRows).mkString("\n")
          Results.Ok(csvContent).as("text/csv")
        }
    }
  }


}
