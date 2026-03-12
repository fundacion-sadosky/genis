package motive

import com.google.inject.AbstractModule

class MotiveModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[MotiveRepository]).to(classOf[SlickMotiveRepository])
    bind(classOf[MotiveService]).to(classOf[MotiveServiceImpl])
  }
}
