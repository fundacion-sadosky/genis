package configdata

import com.google.inject.AbstractModule
import play.api.Environment
import play.api.Configuration

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    import slick.jdbc.JdbcBackend.Database
    val db = Database.forConfig("slick.dbs.default.db")
    bind(classOf[Database]).toInstance(db)
    bind(classOf[BioMaterialTypeRepository]).to(classOf[SlickBioMaterialTypeRepository])
    bind(classOf[BioMaterialTypeService]).to(classOf[BioMaterialTypeServiceImpl])
    bind(classOf[CrimeTypeRepository]).to(classOf[SlickCrimeTypeRepository])
    bind(classOf[CrimeTypeService]).to(classOf[CachedCrimeTypeService])
    bind(classOf[LaboratoryRepository]).to(classOf[SlickLaboratoryRepository])
    bind(classOf[GeneticistRepository]).to(classOf[SlickGeneticistRepository])
  }
}
