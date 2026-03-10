package profiledata

import com.google.inject.AbstractModule

// Simplified from legacy ProfileDataModule which had three nested modules:
// - StashModule (PrivateModule): bound ProtoProfileDataRepository for STASH schema
// - ProfileDataPrivateModule (PrivateModule): bound SlickProfileDataRepository
// - ProfileDataModule (AbstractModule): combined both via Modules.combine()
// The @Named("special") binding was used to inject different repositories
// into ProfileDataServiceImpl depending on context (stash vs main).
// TODO: revisit when ProtoProfileDataRepository and bulkupload/STASH flow are migrated.
class ProfileDataModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ProfileDataRepository]).to(classOf[ProfileDataRepositoryImpl])
    bind(classOf[ProfileDataService]).to(classOf[ProfileDataServiceImpl])
  }
}