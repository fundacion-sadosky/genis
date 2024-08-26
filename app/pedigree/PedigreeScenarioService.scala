package pedigree

import javax.inject.{Inject, Singleton}

import inbox.{NotificationService, PedigreeLRInfo}
import scenarios.ScenarioStatus
import types.MongoId
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api.i18n.Messages

import scala.concurrent.Future

abstract class PedigreeScenarioService {
  def createScenario(pedigreeScenario: PedigreeScenario): Future[Either[String,MongoId]]
  def updateScenario(pedigreeScenario: PedigreeScenario): Future[Either[String,MongoId]]
  def getScenarios(pedigreeId: Long): Future[Seq[PedigreeScenario]]
  def getScenario(scenarioId: MongoId): Future[Option[PedigreeScenario]]
  def changeScenarioStatus(scenario: PedigreeScenario, status: ScenarioStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String,MongoId]]
  def deleteAllScenarios(pedigreeId: Long): Future[Either[String, Long]]
}

@Singleton
class PedigreeScenarioServiceImpl @Inject() (
  pedigreeScenarioRepository: PedigreeScenarioRepository,
  pedigreeService: PedigreeService,
  notificationService: NotificationService) extends PedigreeScenarioService {

  override def createScenario(pedigreeScenario: PedigreeScenario): Future[Either[String, MongoId]] = {
    pedigreeScenarioRepository.create(pedigreeScenario)
  }

  override def deleteAllScenarios(pedigreeId: Long): Future[Either[String, Long]] = {
    pedigreeScenarioRepository.deleteAll(pedigreeId)
  }

  private def solveNotifications(scenario: PedigreeScenario) = {
    pedigreeService.getPedigree(scenario.pedigreeId) foreach { opt =>
      val pedigree = opt.get
      notificationService.solve(pedigree.pedigreeMetaData.assignee, PedigreeLRInfo(scenario.pedigreeId, pedigree.pedigreeMetaData.courtCaseId, scenario.name))
    }
  }

  override def changeScenarioStatus(scenario: PedigreeScenario, status: ScenarioStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, MongoId]] = {
    val result = (scenario.status, status) match {
      case (ScenarioStatus.Pending, _) => {
        pedigreeScenarioRepository.update(scenario).flatMap {
          case Right(_) =>
            if (status == ScenarioStatus.Validated) {
              pedigreeService.changePedigreeStatus(scenario.pedigreeId, PedigreeStatus.Validated, userId, true) flatMap {
                case Right(id) => pedigreeScenarioRepository.changeStatus(scenario._id, status)
                case Left(error) => Future.successful(Left(error))
              }
            } else {
              pedigreeScenarioRepository.changeStatus(scenario._id, status)
            }
          case Left(error) => Future.successful(Left(error))
        }
      }
      case _ => Future.successful(Left(Messages("error.E0930",scenario.status.toString, status.toString)))
    }

    result foreach {
      case Right(_) => solveNotifications(scenario)
      case Left(_) => () }
    result
  }

  override def getScenarios(pedigreeId: Long): Future[Seq[PedigreeScenario]] = {
    pedigreeScenarioRepository.getByPedigree(pedigreeId)
  }

  override def getScenario(scenarioId: MongoId): Future[Option[PedigreeScenario]] = {
    pedigreeScenarioRepository.get(scenarioId)
  }

  override def updateScenario(pedigreeScenario: PedigreeScenario): Future[Either[String, MongoId]] = {
    pedigreeScenarioRepository.update(pedigreeScenario)
  }
}