package user

import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPConnectionPool
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryDirectoryServer
import play.api.Configuration
import com.unboundid.ldap.sdk.schema.Schema

class LdapConnectionPoolFactory(conf: Configuration) {

  private class LdapConf(conf: Configuration) {
    val url = conf.getString("url").get
    val port = conf.getInt("port").get
    val poolsize = conf.getInt("bindingPool.size").get
  }

  private val ldapConf = new LdapConf(conf)

  private lazy val inMemoryServer: InMemoryDirectoryServer = createInMemoryServer(getLdifPath(ldapConf.url))

  /**
   * Single connection is thread safe for operation like search and compare
   */
  def createSingleConnection(): LDAPConnection = {
    if (ldapConf.url.startsWith("memserver")) {
      inMemoryServer.getConnection()
    } else {
      new LDAPConnection(ldapConf.url, ldapConf.port)
    }
  }

  /**
   * Connection pool should be used for non concurrent operations as bind
   */
  def createConnectionPool(): LDAPConnectionPool = {
    if (ldapConf.url.startsWith("memserver")) {
      inMemoryServer.getConnectionPool(ldapConf.poolsize)
    } else {
      val connection: LDAPConnection = new LDAPConnection(ldapConf.url, ldapConf.port)
      new LDAPConnectionPool(connection, ldapConf.poolsize)
    }
  }

  private def getLdifPath(ldapUrl: String): Option[String] = {
    val tokens = ldapUrl.split(":")
    if (tokens.length == 2) Some(tokens(1))
    else None
  }

  private def createInMemoryServer(ldif: Option[String]): InMemoryDirectoryServer = {
    val config: InMemoryDirectoryServerConfig = new InMemoryDirectoryServerConfig("dc=pdg,dc=org");
    
    //Change the following line to use the LDAP configuration with memserver and ldif (no connection needed)
    //config.addAdditionalBindCredentials("uid=asirianni,ou=Users,dc=pdg,dc=org", "asirianni");
    config.addAdditionalBindCredentials("uid=esurijon,ou=Users,dc=pdg,dc=org", "sarasa");

    val schema = Schema.getDefaultStandardSchema // Schema.getSchema(f1, f2)
    
    config.setSchema(schema)
    val ds: InMemoryDirectoryServer = new InMemoryDirectoryServer(config);
    if (ldif.isDefined) ds.importFromLDIF(true, ldif.get);

    ds.startListening();

    ds
  }

}

