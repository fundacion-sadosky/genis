package controllers

import java.util.{Date, GregorianCalendar}

import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Action, Controller}
import javax.inject.{Inject, Singleton}

import reporting.{PdfGenerator, ProfileReportServiceImpl, ReportingService}

import scala.concurrent.Future

@Singleton
class ReportingController @Inject() (reportingService: ReportingService) extends Controller {

  def getPrimerReporte = Action {
    reportingService.generatePrimerReporte()
  }

  def getAllProfilesReporting() = Action{
    reportingService.generateAllProfilesReport()
  }
  def getProfilesReporting(fDesde: String, fHasta: String) = Action {
    val fd = fDesde.split("-")
    val fh = fHasta.split("-")
    val fechaDesde = new GregorianCalendar(fd(2).toInt, fd(1).toInt, fd(0).toInt).getTime
    val fechaHasta = new GregorianCalendar(fh(2).toInt, fh(1).toInt, fh(0).toInt).getTime
    reportingService.generateProfilesReport(fechaDesde, fechaHasta)
  }

  // TODO: Modifirar para llamar a reportingService y obtener los datos para cada reporte
  def getProfilesByUser(fechaDesde: String, fechaHasta: String) = Action {
    val fd = fechaDesde.split("-")
    val fh = fechaHasta.split("-")
    val fechaDesdeDate = new GregorianCalendar(fd(2).toInt, fd(1).toInt, fd(0).toInt).getTime
    val fechaHastaDate = new GregorianCalendar(fh(2).toInt, fh(1).toInt, fh(0).toInt).getTime
    reportingService.generateProfilesReport(fechaDesdeDate, fechaHastaDate)
  }

  def getActivesInactiveByCategory(fechaDesde: String, fechaHasta: String)= Action {
    val fd = fechaDesde.split("-")
    val fh = fechaHasta.split("-")
    val fechaDesdeDate = new GregorianCalendar(fd(2).toInt, fd(1).toInt, fd(0).toInt).getTime
    val fechaHastaDate = new GregorianCalendar(fh(2).toInt, fh(1).toInt, fh(0).toInt).getTime
    reportingService.generateProfilesReport(fechaDesdeDate, fechaHastaDate)
  }

  def getEnviados(fechaDesde: String, fechaHasta: String) = Action {
    val fd = fechaDesde.split("-")
    val fh = fechaHasta.split("-")
    val fechaDesdeDate = new GregorianCalendar(fd(2).toInt, fd(1).toInt, fd(0).toInt).getTime
    val fechaHastaDate = new GregorianCalendar(fh(2).toInt, fh(1).toInt, fh(0).toInt).getTime
    reportingService.generateProfilesReport(fechaDesdeDate, fechaHastaDate)
  }

  def getRecibidos(fechaDesde: String, fechaHasta: String) = Action {
    val fd = fechaDesde.split("-")
    val fh = fechaHasta.split("-")
    val fechaDesdeDate = new GregorianCalendar(fd(2).toInt, fd(1).toInt, fd(0).toInt).getTime
    val fechaHastaDate = new GregorianCalendar(fh(2).toInt, fh(1).toInt, fh(0).toInt).getTime
    reportingService.generateProfilesReport(fechaDesdeDate, fechaHastaDate)
  }

  def getCategoriaCambio(fechaDesde: String, fechaHasta: String) = Action {
    val fd = fechaDesde.split("-")
    val fh = fechaHasta.split("-")
    val fechaDesdeDate = new GregorianCalendar(fd(2).toInt, fd(1).toInt, fd(0).toInt).getTime
    val fechaHastaDate = new GregorianCalendar(fh(2).toInt, fh(1).toInt, fh(0).toInt).getTime
    reportingService.generateProfilesReport(fechaDesdeDate, fechaHastaDate)
  }

  def getAllProfilesByUser() = play.mvc.Results.TODO
}
