package reporting

import java.util.Date
import javax.inject.{Inject, Singleton}

import play.api.mvc.Result

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ReportingService {
  def generatePrimerReporte() : Result
  def generateProfilesReport(fechaDesde: Date, fechaHasta: Date ) : Result
  def generateAllProfilesReport(): Result

}

@Singleton
class ReportingServiceImpl @Inject() (profileReportService : ProfileReportService) extends ReportingService {

  override def generatePrimerReporte(): Result = {
    profileReportService.generatePrimerReporte()
  }

  def generateAllProfilesReport(): Result = {
    profileReportService.generateAllProfilesReport()
  }
  override def generateProfilesReport(fechaDesde: Date, fechaHasta: Date ): Result = {
    profileReportService.generateProfilesReport(fechaDesde, fechaHasta)
  }
}
