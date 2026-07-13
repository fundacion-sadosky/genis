package user

import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPConnectionOptions
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
      // If this connection dies (e.g. TCP reset under a search burst) it is
      // never re-established: every LDAP-dependent action fails with
      // resultCode=81 until the app is restarted (incident 2026-07-09,
      // CDMX deployment). setAutoReconnect covers the incident's death mode,
      // verified against unboundid-ldapsdk 2.3.1 on the asyncSearch path: if
      // the server kills this connection but stays up, the SDK re-establishes
      // it (one immediate attempt on disconnect, plus attempts on send
      // failure, throttled to one per second). It does NOT survive an LDAP
      // server restart: a reconnect attempt against an unreachable server
      // tears the connection down for good ("not established") and nothing
      // ever retries after that — surviving that needs a connection pool.
      val options = new LDAPConnectionOptions()
      options.setAutoReconnect(true)
      new LDAPConnection(options, ldapConf.url, ldapConf.port)
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

