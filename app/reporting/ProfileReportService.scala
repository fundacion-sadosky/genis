package reporting

import play.api.{Logger, Play}
import play.api.mvc.Result

import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration  // Important: Import Play.current

trait ProfileReportService {
  def generatePrimerReporte(): Result
  def generateProfilesReport(fechaDesde: Date, fechaHasta: Date): Result
  def generateAllProfilesReport(): Result
}

@Singleton
class ProfileReportServiceImpl @Inject() (profileReportRepository: ProfileReportRepository, pdfGen: PdfGenerator) extends ProfileReportService {  // Remove Configuration injection
  val logger = Logger(this.getClass())

  // Access configuration directly using Play.current.configuration
  val PROTOCOL = Play.current.configuration.getString("instanceInterconnection.protocol").getOrElse("http://")
  val BASE_URL = PROTOCOL + Play.current.configuration.getString("instanceInterconnection.localUrl").getOrElse("http://localhost:9000")

  override def generatePrimerReporte(): Result = {
    pdfGen.ok(views.html.document2Pdf("Texto..."), BASE_URL)
  }

  override def generateProfilesReport(fechaDesde: Date, fechaHasta: Date): Result = {
    val result = for {
      cantAltas <- profileReportRepository.countProfilesCreated(Some(fechaDesde), Some(fechaHasta))
      cantBajas <- profileReportRepository.countProfilesDeleted()
      cantMatches <- profileReportRepository.countMatches(Some(fechaDesde), Some(fechaHasta))
      cantHit <- profileReportRepository.countHit(Some(fechaDesde), Some(fechaHasta))
      cantDescartes <- profileReportRepository.countDescartes(Some(fechaDesde), Some(fechaHasta))
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
      cantAltas <- profileReportRepository.countProfilesCreated()
      cantBajas <- profileReportRepository.countProfilesDeleted()
      cantMatches <- profileReportRepository.countMatches()
      cantHit <- profileReportRepository.countHit()
      cantDescartes <- profileReportRepository.countDescartes()
    } yield {
      val cantPendientes = cantMatches - cantHit - cantDescartes
      pdfGen.ok(views.html.profilesReport("Resumen de perfiles", cantAltas, cantBajas, cantMatches, cantHit, cantDescartes, cantPendientes,"",""), BASE_URL)
    }

    Await.result(result, Duration.Inf)
  }
}
