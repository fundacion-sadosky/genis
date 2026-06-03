package user

import org.apache.pekko.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.CustomExecutionContext

// Dispatcher dedicado para el I/O bloqueante de LDAP. El legacy usaba
// `play.akka.actor.ldap-context`; en Play 3 (Pekko) se modela como un
// CustomExecutionContext sobre el dispatcher `ldap-context` de application.conf.
// Evita que las operaciones LDAP bloqueantes agoten el thread pool por defecto de Play.
@Singleton
class LdapExecutionContext @Inject() (system: ActorSystem)
  extends CustomExecutionContext(system, "ldap-context")
