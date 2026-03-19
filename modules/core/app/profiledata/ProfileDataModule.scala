package profiledata

import com.google.inject.AbstractModule
import com.google.inject.name.Names

// Simplified from legacy ProfileDataModule which had three nested modules:
// - StashModule (PrivateModule): bound ProtoProfileDataRepository for STASH schema
// - ProfileDataPrivateModule (PrivateModule): bound SlickProfileDataRepository
// - ProfileDataModule (AbstractModule): combined both via Modules.combine()
// The @Named("special") binding was used to inject different repositories
// into ProfileDataServiceImpl depending on context (stash vs main).
// TODO: when ProtoProfileDataRepository (STASH schema) is migrated, create a dedicated
// ProfileDataServiceImpl binding for "stashed" backed by that repository.
class ProfileDataModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ProfileDataRepository]).to(classOf[ProfileDataRepositoryImpl])
    bind(classOf[ProfileDataService]).to(classOf[ProfileDataServiceImpl])
    // @Named("stashed") is used by ProtoProfileDataController (bulk-upload / STASH flow).
    // Points to the same impl until ProtoProfileDataRepository is implemented.
    bind(classOf[ProfileDataService])
      .annotatedWith(Names.named("stashed"))
      .to(classOf[ProfileDataServiceImpl])
  }
}