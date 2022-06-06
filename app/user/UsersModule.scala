package user

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPConnectionPool
import play.api.Application
import play.api.Configuration
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPConnectionPool

class UsersModule(app: Application, conf: Configuration) extends AbstractModule {
  override protected def configure() {

    val factory = new LdapConnectionPoolFactory(conf)
    val connectionPool = factory.createConnectionPool
    bind(classOf[LDAPConnectionPool]).toInstance(connectionPool)
    val connection = factory.createSingleConnection
    bind(classOf[LDAPConnection]).toInstance(connection)
    val usersDn = conf.getString("usersDn").get
    bind(classOf[String]).annotatedWith(Names.named("usersDn")).toInstance(usersDn);
    val adminDn = conf.getString("adminDn").get
    bind(classOf[String]).annotatedWith(Names.named("adminDn")).toInstance(adminDn);
    val adminPassword = conf.getString("adminPassword").get
    bind(classOf[String]).annotatedWith(Names.named("adminPassword")).toInstance(adminPassword);
    val rolesDn = conf.getString("rolesDn").get
    bind(classOf[String]).annotatedWith(Names.named("rolesDn")).toInstance(rolesDn);

    bind(classOf[Application]).toInstance(app)
    bind(classOf[UserRepository]).to(classOf[LdapUserRepository])

    bind(classOf[UserService]).to(classOf[UserServiceImpl])
    
    bind(classOf[RoleService]).to(classOf[RoleServiceImpl])
    bind(classOf[RoleRepository]).to(classOf[LdapRoleRepository])

    ()
  }

}
