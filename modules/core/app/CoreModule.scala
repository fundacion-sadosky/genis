package modules.core

import com.google.inject.AbstractModule
import services.{CountryService, LaboratoryService, GeneticistService, UserService}
import services.{LaboratoryServiceImpl, GeneticistServiceImpl, CountryServiceImpl, UserServiceImpl}

class CoreModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[LaboratoryService]).to(classOf[LaboratoryServiceImpl])
    bind(classOf[GeneticistService]).to(classOf[GeneticistServiceImpl])
    bind(classOf[CountryService]).to(classOf[CountryServiceImpl])
    bind(classOf[UserService]).to(classOf[UserServiceImpl])
  }
}