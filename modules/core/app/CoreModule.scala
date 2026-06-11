package modules.core

import com.google.inject.AbstractModule
import matching.{MatchingProcessStatus, MatchingProcessStatusImpl}
import services.{CountryService, LaboratoryService, GeneticistService, UserService}
import services.{LaboratoryServiceImpl, GeneticistServiceImpl, CountryServiceImpl, UserServiceImpl}
import stats.{PopulationBaseFrequencyRepository, PopulationBaseFrequencyRepositoryImpl}
import stats.{PopulationBaseFrequencyService, PopulationBaseFrequencyServiceImpl}

class CoreModule extends AbstractModule {
  override def configure(): Unit = {
    val logger = org.slf4j.LoggerFactory.getLogger("modules.core.CoreModule")
    try {
      logger.info("[CoreModule] Starting configuration...")
      bind(classOf[LaboratoryService]).to(classOf[LaboratoryServiceImpl])
      bind(classOf[GeneticistService]).to(classOf[GeneticistServiceImpl])
      bind(classOf[CountryService]).to(classOf[CountryServiceImpl])
      bind(classOf[UserService]).to(classOf[UserServiceImpl])
      // NotificationService ahora es responsabilidad de inbox.NotificationModule
      bind(classOf[MatchingProcessStatus]).to(classOf[MatchingProcessStatusImpl])

      // Population Base Frequency (stats module)
      bind(classOf[PopulationBaseFrequencyRepository]).to(classOf[PopulationBaseFrequencyRepositoryImpl])
      bind(classOf[PopulationBaseFrequencyService]).to(classOf[PopulationBaseFrequencyServiceImpl])
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