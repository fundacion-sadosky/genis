package standalone

import play.api.GlobalSettings
import play.api.test.FakeApplication

/** Minimal Play application needed to run the forensic engine outside GENis.
  *
  * `probability.LRMixCalculator` takes its thread pool from the running application
  * (key `play.akka.actor.lrmix-context`, defined in conf/application.conf). No database,
  * web layer or any other GENis service is started. The pedigree engine does not need it.
  *
  * In a ScalaTest suite, mix in `org.scalatestplus.play.OneAppPerSuite` and override `app`
  * with `EngineApp()`; see forensic.ForensicReferenceCasesTest. */
object EngineApp {
  def apply(): FakeApplication = FakeApplication(withGlobal = Some(new GlobalSettings {}))
}
