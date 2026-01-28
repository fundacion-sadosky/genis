package profiledata

import com.google.inject.AbstractModule
import com.google.inject.PrivateModule
import com.google.inject.name.Names
import com.google.inject.util.Modules
import play.api.Configuration

class StashModule extends PrivateModule {
  override protected def configure() = {
    bind(classOf[ProfileDataRepository]).annotatedWith(Names.named("special")).to(classOf[ProtoProfileDataRepository])
    bind(classOf[ProfileDataService]).annotatedWith(Names.named("stashed")).to(classOf[ProfileDataServiceImpl])
    expose(classOf[ProfileDataService]).annotatedWith(Names.named("stashed"))
  }
}

class ProfileDataPrivateModule extends PrivateModule {
  override protected def configure() {
    bind(classOf[ProfileDataRepository]).annotatedWith(Names.named("special")).to(classOf[SlickProfileDataRepository])
    bind(classOf[ProfileDataService]).to(classOf[ProfileDataServiceImpl])
    expose(classOf[ProfileDataService])
    ()
  }
}

class ProfileDataModule() extends AbstractModule {
  override protected def configure() {
    val module = Modules.combine(new ProfileDataPrivateModule, new StashModule)
    bind(classOf[ProfileDataRepository]).to(classOf[SlickProfileDataRepository])
    bind(classOf[ImportToProfileData]).to(classOf[SlickImportToProfileData])
    install(module)
  }
}

