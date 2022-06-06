package disclaimer

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.Configuration
import play.api.libs.ws._
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder

class DisclaimerModule() extends AbstractModule {
  override protected def configure() {

    bind(classOf[DisclaimerRepository]).to(classOf[SlickDisclaimerRepository])

    bind(classOf[DisclaimerService]).to(classOf[DisclaimerServiceImpl])

    ()
  }
}