package configdata

import com.google.inject.AbstractModule
import play.api.Environment
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.Future

@Singleton
class DatabaseProvider @Inject()(lifecycle: ApplicationLifecycle) extends Provider[slick.jdbc.JdbcBackend.Database] {
  private lazy val db = slick.jdbc.JdbcBackend.Database.forConfig("slick.dbs.default.db")
  lifecycle.addStopHook(() => Future.successful(db.close()))
  override def get(): slick.jdbc.JdbcBackend.Database = db
}

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    import slick.jdbc.JdbcBackend.Database
    bind(classOf[Database]).toProvider(classOf[DatabaseProvider]).asEagerSingleton()
    bind(classOf[BioMaterialTypeRepository]).to(classOf[SlickBioMaterialTypeRepository])
    bind(classOf[BioMaterialTypeService]).to(classOf[BioMaterialTypeServiceImpl])
    bind(classOf[CrimeTypeRepository]).to(classOf[SlickCrimeTypeRepository])
    bind(classOf[CrimeTypeService]).to(classOf[CachedCrimeTypeService])
    bind(classOf[LaboratoryRepository]).to(classOf[SlickLaboratoryRepository])
    bind(classOf[GeneticistRepository]).to(classOf[SlickGeneticistRepository])
    bind(classOf[CategoryRepository]).to(classOf[SlickCategoryRepository])
    bind(classOf[CategoryService]).to(classOf[CategoryServiceImpl])
  }
}
