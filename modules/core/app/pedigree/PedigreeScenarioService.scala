package pedigree

import inbox.{NotificationService, PedigreeLRInfo}
import matching.MongoId
import scenarios.ScenarioStatus

import scala.concurrent.{ExecutionContext, Future}

// ---------------------------------------------------------------------------
// PedigreeScenarioService
// ---------------------------------------------------------------------------

trait PedigreeScenarioService:
  def createScenario(pedigreeScenario: PedigreeScenario): Future[Either[String, MongoId]]
  def updateScenario(pedigreeScenario: PedigreeScenario): Future[Either[String, MongoId]]
  def getScenarios(pedigreeId: Long): Future[Seq[PedigreeScenario]]
  def getScenario(scenarioId: MongoId): Future[Option[PedigreeScenario]]
  def changeScenarioStatus(scenario: PedigreeScenario, status: ScenarioStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, MongoId]]
  def deleteAllScenarios(pedigreeId: Long): Future[Either[String, Long]]

@jakarta.inject.Singleton
class PedigreeScenarioServiceStub extends PedigreeScenarioService:
  override def createScenario(s: PedigreeScenario): Future[Either[String, MongoId]]                                                               = Future.successful(Right(s._id))
  override def updateScenario(s: PedigreeScenario): Future[Either[String, MongoId]]                                                               = Future.successful(Right(s._id))
  override def getScenarios(pedigreeId: Long): Future[Seq[PedigreeScenario]]                                                                      = Future.successful(Seq.empty)
  override def getScenario(id: MongoId): Future[Option[PedigreeScenario]]                                                                         = Future.successful(None)
  override def changeScenarioStatus(s: PedigreeScenario, st: ScenarioStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, MongoId]] = Future.successful(Right(s._id))
  override def deleteAllScenarios(pedigreeId: Long): Future[Either[String, Long]]                                                                 = Future.successful(Right(pedigreeId))

// ---------------------------------------------------------------------------
// PedigreeScenarioServiceImpl
// Uses a Provider[PedigreeService] to break the circular dependency
// PedigreeService -> PedigreeScenarioRepository -> (none)
// PedigreeScenarioService -> PedigreeService (circular via BayesianNetworkService)
// ---------------------------------------------------------------------------

@jakarta.inject.Singleton
class PedigreeScenarioServiceImpl @jakarta.inject.Inject() (
  pedigreeScenarioRepository: PedigreeScenarioRepository,
  pedigreeServiceProvider: jakarta.inject.Provider[PedigreeService],
  notificationService: NotificationService
)(using ec: ExecutionContext) extends PedigreeScenarioService:

  private def pedigreeService: PedigreeService = pedigreeServiceProvider.get()

  private def solveNotifications(scenario: PedigreeScenario): Unit =
    pedigreeService.getPedigree(scenario.pedigreeId).foreach { opt =>
      val pedigree = opt.get
      notificationService.solve(pedigree.pedigreeMetaData.assignee,
        PedigreeLRInfo(scenario.pedigreeId, pedigree.pedigreeMetaData.courtCaseId, scenario.name))
    }

  override def createScenario(pedigreeScenario: PedigreeScenario): Future[Either[String, MongoId]] =
    pedigreeScenarioRepository.create(pedigreeScenario)

  override def updateScenario(pedigreeScenario: PedigreeScenario): Future[Either[String, MongoId]] =
    pedigreeScenarioRepository.update(pedigreeScenario)

  override def getScenarios(pedigreeId: Long): Future[Seq[PedigreeScenario]] =
    pedigreeScenarioRepository.getByPedigree(pedigreeId)

  override def getScenario(scenarioId: MongoId): Future[Option[PedigreeScenario]] =
    pedigreeScenarioRepository.get(scenarioId)

  override def deleteAllScenarios(pedigreeId: Long): Future[Either[String, Long]] =
    pedigreeScenarioRepository.deleteAll(pedigreeId)

  override def changeScenarioStatus(scenario: PedigreeScenario, status: ScenarioStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, MongoId]] =
    val result = (scenario.status, status) match
      case (ScenarioStatus.Pending, _) =>
        pedigreeScenarioRepository.update(scenario).flatMap {
          case Right(_) =>
            if status == ScenarioStatus.Validated then
              pedigreeService.changePedigreeStatus(scenario.pedigreeId, PedigreeStatus.Validated, userId, isSuperUser = true).flatMap {
                case Right(_)    => pedigreeScenarioRepository.changeStatus(scenario._id, status)
                case Left(error) => Future.successful(Left(error))
              }
            else
              pedigreeScenarioRepository.changeStatus(scenario._id, status)
          case Left(error) => Future.successful(Left(error))
        }
      case _ =>
        Future.successful(Left(s"error.E0930|${scenario.status}|$status"))

    result.foreach {
      case Right(_) => solveNotifications(scenario)
      case Left(_)  => ()
    }
    result
