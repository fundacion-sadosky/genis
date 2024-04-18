package configdata

import com.google.inject.AbstractModule
import play.api.Configuration
import com.google.inject.name.Names
import kits.{SlickKitDataRepository, StrKitRepository, StrKitService, StrKitServiceImpl}

class ConfigDataModule(conf: Configuration) extends AbstractModule {
  override protected def configure() {

    val labCode = conf.getString("laboratory.code").get
    bind(classOf[String]).annotatedWith(Names.named("labCode")).toInstance(labCode);

    val country = conf.getString("laboratory.country").get
    bind(classOf[String]).annotatedWith(Names.named("country")).toInstance(country);

    val prov = conf.getString("laboratory.province").get
    bind(classOf[String]).annotatedWith(Names.named("province")).toInstance(prov);
    
    bind(classOf[CountryService]).to(classOf[CountryServiceImpl])
    bind(classOf[CountryRepository]).to(classOf[SlickCountryRepository])

    bind(classOf[CategoryRepository]).to(classOf[SlickCategoryRepository])
    bind(classOf[CategoryService]).to(classOf[CachedCategoryService])

    bind(classOf[CrimeTypeRepository]).to(classOf[SlickCrimeTypeRepository])
    bind(classOf[CrimeTypeService]).to(classOf[CachedCrimeTypeService])

    bind(classOf[BioMaterialTypeRepository]).to(classOf[SlickBioMaterialTypeRepository])
    bind(classOf[BioMaterialTypeService]).to(classOf[CachedBioMaterialTypeService])

    bind(classOf[QualityParamsProvider]).to(classOf[QualityParamsProviderImpl])

    ()
  }
}