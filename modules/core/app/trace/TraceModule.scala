package trace

import com.google.inject.AbstractModule

class TraceModule extends AbstractModule {
  override protected def configure(): Unit = {
    bind(classOf[TraceService]).to(classOf[TraceServiceImpl])
    bind(classOf[TraceRepository]).to(classOf[SlickTraceRepository])
    // PedigreeDataRepository ahora lo bindea pedigree.PedigreeModule (SlickPedigreeDataRepository).
    ()
  }
}
