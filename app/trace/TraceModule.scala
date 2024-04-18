package trace

import com.google.inject.AbstractModule

class TraceModule extends AbstractModule {
  override protected def configure() {

    bind(classOf[TraceService]).to(classOf[TraceServiceImpl])
    bind(classOf[TraceRepository]).to(classOf[SlickTraceRepository])

    ()
  }

}