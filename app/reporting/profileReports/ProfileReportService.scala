package reporting.profileReports

import java.util.{Calendar, Date, GregorianCalendar}
import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.mvc.Result
import reactivemongo.api.collections.bson.BSONBatchCommands
import reporting.PdfGenerator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, _}

trait ProfileReportService {
  def generatePrimerReporte() : Result
  def generateProfilesReport(fechaDesde: Date, fechaHasta: Date ) : Result

}

@Singleton
class ProfileReportServiceImpl @Inject() (profileReportRepository: ProfileReportRepository,
                                          pdfGen: PdfGenerator) extends ProfileReportService {
  val logger = Logger(this.getClass())
  val BASE_URL = "http://localhost:9000"

  override def generatePrimerReporte(): Result = {
    pdfGen.ok(views.html.document2Pdf("Texto..."), BASE_URL)
  }

  override def generateProfilesReport(fechaDesde: Date, fechaHasta: Date ): Result = {

  val result =  for {
      cantAltas <- profileReportRepository.countProfilesCreated(fechaDesde, fechaHasta)
      cantBajas <- profileReportRepository.countProfilesDeleted()
      cantMatches <- profileReportRepository.countMatches(fechaDesde, fechaHasta)
      cantHit <- profileReportRepository.countHit(fechaDesde, fechaHasta)
      cantDescartes <- profileReportRepository.countDescartes(fechaDesde, fechaHasta)
    } yield {
      val fechaDesdeString = fechaDesde.getDate.toString + "/" + (fechaDesde.getMonth+1).toString + "/" + (fechaDesde.getYear+1900).toString
      val fechaHastaString = fechaHasta.getDate.toString + "/" + (fechaHasta.getMonth+1).toString + "/" + (fechaHasta.getYear+1900).toString
      val cantPendientes = cantMatches - cantHit - cantDescartes
      pdfGen.ok(views.html.profilesReport("Resumen de perfiles", cantAltas, cantBajas, cantMatches, cantHit, cantDescartes, cantPendientes, fechaDesdeString, fechaHastaString), BASE_URL)
    }

    Await.result(result, Duration.Inf)

  }

}
