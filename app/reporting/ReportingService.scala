package reporting

import java.util.Date
import javax.inject.{Inject, Singleton}

import play.api.mvc.Result
import reporting.profileReports.ProfileReportService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ReportingService {
  def generatePrimerReporte() : Result
  def generateProfilesReport(fechaDesde: Date, fechaHasta: Date ) : Result

}

@Singleton
class ReportingServiceImpl @Inject() (profileReportService : ProfileReportService) extends ReportingService {

  override def generatePrimerReporte(): Result = {
    profileReportService.generatePrimerReporte()
  }

  override def generateProfilesReport(fechaDesde: Date, fechaHasta: Date ): Result = {
    profileReportService.generateProfilesReport(fechaDesde, fechaHasta)
  }
}
