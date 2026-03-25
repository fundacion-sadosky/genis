package user

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.unboundid.ldap.sdk.{LDAPConnection, LDAPConnectionPool}
import play.api.{Configuration, Environment}

import security.UserRepository

class UsersModule(environment: Environment, conf: Configuration) extends AbstractModule:
  override protected def configure(): Unit =
    val logger = org.slf4j.LoggerFactory.getLogger("user.UsersModule")
    try {
      logger.info("[UsersModule] Starting configuration...")
      val ldapConf = conf.get[Configuration]("ldap.default")

      val factory = new LdapConnectionPoolFactory(ldapConf)
      val connectionPool = factory.createConnectionPool()
      bind(classOf[LDAPConnectionPool]).toInstance(connectionPool)
      val connection = factory.createSingleConnection()
      bind(classOf[LDAPConnection]).toInstance(connection)

      val usersDn = ldapConf.get[String]("usersDn")
      bind(classOf[String]).annotatedWith(Names.named("usersDn")).toInstance(usersDn)

      val rolesDn = ldapConf.get[String]("rolesDn")
      bind(classOf[String]).annotatedWith(Names.named("rolesDn")).toInstance(rolesDn)

      bind(classOf[UserRepository]).to(classOf[LdapUserRepository])
      bind(classOf[RoleRepository]).to(classOf[LdapRoleRepository])
      logger.info("[UsersModule] Configuration completed successfully.")
    } catch {
      case ex: Throwable =>
        logger.error("[UsersModule] Exception during configuration", ex)
        println("[UsersModule] Exception during configuration: " + ex.getMessage)
        ex.printStackTrace()
        throw ex
    }
