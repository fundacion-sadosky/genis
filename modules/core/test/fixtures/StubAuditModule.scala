package fixtures

import audit.OperationLogService
import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}

/** Replaces audit.OperationLogModule in tests: skips the PEOSignerActor + genislogdb
 *  connection pool and binds a no-op OperationLogService. */
class StubAuditModule(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[OperationLogService]).to(classOf[StubOperationLogService])
    ()
  }
}
