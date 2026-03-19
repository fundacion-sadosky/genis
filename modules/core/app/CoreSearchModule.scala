package modules.core

import com.google.inject.AbstractModule
import configdata.{CategoryService, CategoryServiceImpl}
import search.{FullTextSearchService, FullTextSearchServiceImpl}
import services.{UserService, UserServiceImpl}

class CoreSearchModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[CategoryService]).to(classOf[CategoryServiceImpl])
    bind(classOf[FullTextSearchService]).to(classOf[FullTextSearchServiceImpl])
    bind(classOf[UserService]).to(classOf[UserServiceImpl])
  }
}
