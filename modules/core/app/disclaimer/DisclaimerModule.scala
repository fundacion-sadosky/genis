package disclaimer

import com.google.inject.AbstractModule

class DisclaimerModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[DisclaimerService]).to(classOf[DisclaimerServiceImpl])
    bind(classOf[DisclaimerRepository]).to(classOf[SlickDisclaimerRepository])
  }
}
