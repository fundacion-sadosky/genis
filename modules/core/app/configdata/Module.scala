package configdata

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.Environment
import play.api.Configuration

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    import slick.jdbc.JdbcBackend.Database
    val db = Database.forConfig("slick.dbs.default.db")
    bind(classOf[Database]).toInstance(db)

    // labCode = laboratory.code (instancia local). Default "" como en el legacy (genis-misc.conf).
    val labCode = configuration.getOptional[String]("laboratory.code").getOrElse("")
    bind(classOf[String]).annotatedWith(Names.named("labCode")).toInstance(labCode)

    bind(classOf[BioMaterialTypeRepository]).to(classOf[SlickBioMaterialTypeRepository])
    bind(classOf[BioMaterialTypeService]).to(classOf[BioMaterialTypeServiceImpl])
    bind(classOf[CrimeTypeRepository]).to(classOf[SlickCrimeTypeRepository])
    bind(classOf[CrimeTypeService]).to(classOf[CachedCrimeTypeService])
    bind(classOf[LaboratoryRepository]).to(classOf[SlickLaboratoryRepository])
    bind(classOf[GeneticistRepository]).to(classOf[SlickGeneticistRepository])
  }
}
