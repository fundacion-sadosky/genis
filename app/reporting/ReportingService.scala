package reporting

import java.util.Date
import javax.inject.{Inject, Singleton}
import play.api.mvc.Result

import scala.concurrent.Future

trait ReportingService {
  def generatePrimerReporte() : Result
  def generateProfilesReport(fechaDesde: Date, fechaHasta: Date ) : Result
  def generateAllProfilesReport(): Result
  def generateProfileByUser() : Future[Result]
  def generateActivesInactiveByCategory(): Future[Result]
  def generateEnviados(): Future[Result]
  def generateRecibidos(): Future[Result]
  def generateCategoriaCambio(): Future[Result]

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

  override def generateProfileByUser(): Future[Result] = {
    profileReportService.generateProfileByUser()
  }

  def generateActivesInactiveByCategory(): Future[Result] = {
    profileReportService.generateActivesInactiveByCategory()
  }

  def generateEnviados(): Future[Result] = {
    profileReportService.generateEnviados()
  }

  def generateRecibidos(): Future[Result] =  {
    profileReportService.generateRecibidos()
  }

  def generateCategoriaCambio(): Future[Result] ={
    profileReportService.generateCategoriaCambio()
  }

}
