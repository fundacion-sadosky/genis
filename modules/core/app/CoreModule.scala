package modules.core

import com.google.inject.AbstractModule
import inbox.{NotificationService, NoOpNotificationService}
import services.{CountryService, LaboratoryService, GeneticistService, UserService}
import services.{LaboratoryServiceImpl, GeneticistServiceImpl, CountryServiceImpl, UserServiceImpl}

class CoreModule extends AbstractModule {
  override def configure(): Unit = {
    val logger = org.slf4j.LoggerFactory.getLogger("modules.core.CoreModule")
    try {
      logger.info("[CoreModule] Starting configuration...")
      bind(classOf[LaboratoryService]).to(classOf[LaboratoryServiceImpl])
      bind(classOf[GeneticistService]).to(classOf[GeneticistServiceImpl])
      bind(classOf[CountryService]).to(classOf[CountryServiceImpl])
      bind(classOf[UserService]).to(classOf[UserServiceImpl])
      bind(classOf[NotificationService]).to(classOf[NoOpNotificationService])
      logger.info("[CoreModule] Configuration completed successfully.")
    } catch {
      case ex: Throwable =>
        logger.error("[CoreModule] Exception during configuration", ex)
        println("[CoreModule] Exception during configuration: " + ex.getMessage)
        ex.printStackTrace()
        throw ex
    }
  }
}