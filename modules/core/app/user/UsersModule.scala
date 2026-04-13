package user

import com.google.inject.AbstractModule
import com.google.inject.name.Names
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
      val connection = factory.createSingleConnection()
      val provider = new LdapConnectionProviderImpl(connectionPool, connection)
      bind(classOf[LdapConnectionProvider]).toInstance(provider)

      val usersDn = ldapConf.get[String]("usersDn")
      bind(classOf[String]).annotatedWith(Names.named("usersDn")).toInstance(usersDn)

      val rolesDn = ldapConf.get[String]("rolesDn")
      bind(classOf[String]).annotatedWith(Names.named("rolesDn")).toInstance(rolesDn)

      val adminDn = ldapConf.get[String]("bindDn")
      bind(classOf[String]).annotatedWith(Names.named("adminDn")).toInstance(adminDn)
      val adminPassword = ldapConf.get[String]("bindPassword")
      bind(classOf[String]).annotatedWith(Names.named("adminPassword")).toInstance(adminPassword)

      bind(classOf[UserRepository]).to(classOf[LdapUserRepository])
      bind(classOf[RoleRepository]).to(classOf[LdapRoleRepository])
      bind(classOf[LdapHealthService]).to(classOf[LdapHealthServiceImpl])
      logger.info("[UsersModule] Configuration completed successfully.")
    } catch {
      case ex: Throwable =>
        logger.error("[UsersModule] Exception during configuration", ex)
        println("[UsersModule] Exception during configuration: " + ex.getMessage)
        ex.printStackTrace()
        throw ex
    }
