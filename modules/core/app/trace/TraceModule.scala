package trace

import com.google.inject.AbstractModule
import pedigree.{PedigreeDataRepository, PedigreeDataRepositoryStub}

class TraceModule extends AbstractModule {
  override protected def configure(): Unit = {
    bind(classOf[TraceService]).to(classOf[TraceServiceImpl])
    bind(classOf[TraceRepository]).to(classOf[SlickTraceRepository])
    bind(classOf[PedigreeDataRepository]).to(classOf[PedigreeDataRepositoryStub])
    ()
  }
}
