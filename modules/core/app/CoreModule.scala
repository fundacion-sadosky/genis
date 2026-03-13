package modules.core

import com.google.inject.AbstractModule
import services.{CountryService, LaboratoryService, GeneticistService, UserService}
import services.{LaboratoryServiceImpl, GeneticistServiceImpl, CountryServiceImpl, UserServiceImpl}
import stats.{PopulationBaseFrequencyRepository, PopulationBaseFrequencyRepositoryImpl}
import stats.{PopulationBaseFrequencyService, PopulationBaseFrequencyServiceImpl}

class CoreModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[LaboratoryService]).to(classOf[LaboratoryServiceImpl])
    bind(classOf[GeneticistService]).to(classOf[GeneticistServiceImpl])
    bind(classOf[CountryService]).to(classOf[CountryServiceImpl])
    bind(classOf[UserService]).to(classOf[UserServiceImpl])

    // Population Base Frequency (stats module)
    bind(classOf[PopulationBaseFrequencyRepository]).to(classOf[PopulationBaseFrequencyRepositoryImpl])
    bind(classOf[PopulationBaseFrequencyService]).to(classOf[PopulationBaseFrequencyServiceImpl])
  }
}