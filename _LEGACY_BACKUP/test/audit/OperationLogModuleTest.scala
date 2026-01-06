package audit

import com.google.inject.Guice
import javax.inject.Inject
import javax.inject.Singleton

import specs.PdgSpec
import javax.inject.Named

import com.google.inject.AbstractModule
import pdgconf.PdgModule

class OperationLogModuleTest extends PdgSpec {

  "OperationLogModule" must {
    "instatiate different instances of PEOSigner based on its annotated name" in {

      val module = new AbstractModule {
        override protected def configure() {
          val opModule = new PdgModule(app)
          install(opModule)
          bind(classOf[TestService])
          ()
        }
      }
      val injector = Guice.createInjector(module)
      val testService = injector.getInstance(classOf[TestService])

      testService.logSignerService must not equal (testService.profileSignerService)

    }
  }

}