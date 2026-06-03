package user

import scala.concurrent.Future

import com.unboundid.ldap.sdk.{LDAPConnection, LDAPConnectionPool}
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.inject.ApplicationLifecycle

// El módulo Guice (`UsersModule`) crea el pool y la conexión LDAP sin ningún hook de
// shutdown. Este eager singleton registra un stop hook para cerrarlos al apagar la app.
@Singleton
class LdapConnectionLifecycle @Inject() (
    lifecycle: ApplicationLifecycle,
    connectionPool: LDAPConnectionPool,
    connection: LDAPConnection
) {
  private val logger: Logger = Logger(this.getClass)

  lifecycle.addStopHook { () =>
    Future.successful {
      logger.info("Closing LDAP connection pool and search connection")
      connectionPool.close()
      connection.close()
    }
  }
}
