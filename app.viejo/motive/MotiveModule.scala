package motive

import com.google.inject.AbstractModule

class MotiveModule extends AbstractModule {
  override protected def configure() {

    bind(classOf[MotiveRepository]).to(classOf[SlickMotiveRepository])

    bind(classOf[MotiveService]).to(classOf[MotiveServiceImpl])

    ()
  }
}