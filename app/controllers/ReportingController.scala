package controllers

import java.util.{Date, GregorianCalendar}

import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, Controller}
import javax.inject.{Inject, Singleton}

import reporting.{PdfGenerator, ReportingService}
import reporting.profileReports.ProfileReportServiceImpl

import scala.concurrent.Future

@Singleton
class ReportingController @Inject() (reportingService: ReportingService) extends Controller {

  def getPrimerReporte = Action {
    reportingService.generatePrimerReporte()
  }

  def getProfilesReporting(fDesde: String, fHasta: String) = Action {
    val fd = fDesde.split("-")
    val fh = fHasta.split("-")
    val fechaDesde = new GregorianCalendar(fd(2).toInt, fd(1).toInt, fd(0).toInt).getTime
    val fechaHasta = new GregorianCalendar(fh(2).toInt, fh(1).toInt, fh(0).toInt).getTime
    reportingService.generateProfilesReport(fechaDesde, fechaHasta)
  }
}
