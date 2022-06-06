package search

import com.google.inject.AbstractModule

class SearchModule extends AbstractModule {
  override protected def configure() {
    bind(classOf[FullTextSearch]).to(classOf[FullTextSearchPg])
    bind(classOf[FullTextSearchService]).to(classOf[FullTextSearchServiceImpl])
    ()
  }
}