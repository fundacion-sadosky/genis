package modules

import com.google.inject.AbstractModule
import com.google.inject.Provides
import javax.inject.Singleton
import play.api.Configuration
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPConnectionPool
import java.net.URI

class LdapModule extends AbstractModule {

  override def configure() = {
  }

  @Provides
  @Singleton
  def provideLdapConnectionPool(configuration: Configuration): LDAPConnectionPool = {
    val enabled = configuration.getOptional[Boolean]("ldap.enabled").getOrElse(false)
    if (enabled) {
      val urlStr = configuration.get[String]("ldap.provider_url")
      val uri = new URI(urlStr)
      val host = uri.getHost
      val port = uri.getPort
      
      // Create connection
      val connection = new LDAPConnection(host, port)
      
      val adminDn = configuration.getOptional[String]("ldap.admin_dn").getOrElse("cn=admin,dc=genis,dc=local")
      val adminPassword = configuration.getOptional[String]("ldap.admin_password").getOrElse("adminp")

      connection.bind(adminDn, adminPassword)
      
      new LDAPConnectionPool(connection, 10)
    } else {
      // Return a dummy or fail?
      // For now, let's assume it's enabled.
      throw new RuntimeException("LDAP not enabled in configuration")
    }
  }
}
