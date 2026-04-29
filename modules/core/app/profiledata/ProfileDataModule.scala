package profiledata

import com.google.inject.{AbstractModule, PrivateModule}
import com.google.inject.name.Names

class ProfileDataPrivateModule extends PrivateModule:
  override protected def configure(): Unit =
    bind(classOf[ProfileDataRepository]).annotatedWith(Names.named("special")).to(classOf[SlickProfileDataRepository])
    bind(classOf[ProfileDataService]).to(classOf[ProfileDataServiceImpl])
    expose(classOf[ProfileDataService])

// Stash variant: binds ProtoProfileData tables (STASH schema) for BulkUpload/ProtoProfile workflows.
// The @Named("stashed") ProfileDataService uses the same ProfileDataServiceImpl but pointed
// at the STASH schema tables via SlickStashProfileDataRepository.
class StashModule extends PrivateModule:
  override protected def configure(): Unit =
    bind(classOf[ProfileDataRepository]).annotatedWith(Names.named("special")).to(classOf[SlickStashProfileDataRepository])
    bind(classOf[ProfileDataService]).annotatedWith(Names.named("stashed")).to(classOf[ProfileDataServiceImpl])
    expose(classOf[ProfileDataService]).annotatedWith(Names.named("stashed"))

class ProfileDataModule extends AbstractModule:
  override protected def configure(): Unit =
    bind(classOf[ProfileDataRepository]).to(classOf[SlickProfileDataRepository])
    install(new ProfileDataPrivateModule)
    install(new StashModule)
