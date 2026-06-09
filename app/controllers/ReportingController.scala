package controllers

import java.util.{Date, GregorianCalendar}
import play.api.libs.json.{JsError, Json, Writes, __}
import play.api.libs.functional.syntax._
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
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
  def getAllProfilesByUser() = Action.async {
    reportingService.generateProfileByUser()
  }

  def getActivesInactiveByCategory()= Action.async {
    reportingService.generateActivesInactiveByCategory()
  }

  def getEnviados() = Action.async {
    reportingService.generateEnviados()
  }

  def getRecibidos() = Action.async {
    reportingService.generateRecibidos()
  }

  def getCategoriaCambio() = Action.async {
    reportingService.generateCategoriaCambio()
  }

  def getListadoCompleto() = Action.async {
    reportingService.generateAllProfilesList()
  }

  def getListadoCoincidencias() = Action.async {
    reportingService.generateMatchesList()
  }

  def getListadoReplicadosAInstanciaSuperior() = Action.async {
    reportingService.generateReplicatedToSuperiorList()
  }

  def getListedoRecibidosInstanciasInferiores() = Action.async {
    reportingService.generateReplicatedFromInferiorList()
  }



}
