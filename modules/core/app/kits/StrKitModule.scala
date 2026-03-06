package kits

import com.google.inject.AbstractModule

class StrKitModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[StrKitRepository]).to(classOf[StrKitRepositoryImpl])
    bind(classOf[StrKitService]).to(classOf[StrKitServiceImpl])
  }
}
