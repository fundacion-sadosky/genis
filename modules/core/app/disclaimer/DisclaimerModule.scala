package disclaimer

import com.google.inject.AbstractModule

class DisclaimerModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[DisclaimerRepository]).to(classOf[SlickDisclaimerRepository])
    bind(classOf[DisclaimerService]).to(classOf[DisclaimerServiceImpl])
  }
}
