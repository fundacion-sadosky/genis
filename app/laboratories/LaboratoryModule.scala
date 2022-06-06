package laboratories

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.Configuration

class LaboratoryModule(conf: Configuration) extends AbstractModule {
  override protected def configure {

    val labCode = conf.getString("laboratory.code").get
    bind(classOf[String]).annotatedWith(Names.named("labCode")).toInstance(labCode);

    bind(classOf[LaboratoryRepository]).to(classOf[SlickLaboratoryRepository])
    bind(classOf[LaboratoryService]).to(classOf[LaboratoryServiceImpl])

    bind(classOf[GeneticistRepository]).to(classOf[SlickGeneticistRepository])
    bind(classOf[GeneticistService]).to(classOf[GeneticistServiceImpl])

    ()
  }
  
}