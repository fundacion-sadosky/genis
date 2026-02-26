package configdata

import com.google.inject.AbstractModule
import play.api.Environment
import play.api.Configuration

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[BioMaterialTypeRepository]).to(classOf[SlickBioMaterialTypeRepository])
    bind(classOf[BioMaterialTypeService]).to(classOf[BioMaterialTypeServiceImpl])
  }
}
