package user

import com.unboundid.ldap.sdk.{LDAPConnection, LDAPConnectionPool}
import com.unboundid.ldap.listener.{InMemoryDirectoryServer, InMemoryDirectoryServerConfig}
import com.unboundid.ldap.sdk.schema.Schema
import play.api.{Configuration, Logger}

class LdapConnectionPoolFactory(conf: Configuration):

  private val logger = Logger(this.getClass)

  private class LdapConf(conf: Configuration):
    val url: String = conf.get[String]("url")
    val port: Int = conf.get[Int]("port")
    val poolSize: Int = conf.get[Int]("bindingPool.size")

  private val ldapConf = LdapConf(conf)

  private lazy val inMemoryServer: InMemoryDirectoryServer = createInMemoryServer(getLdifPath(ldapConf.url))

  def createSingleConnection(): LDAPConnection =
    if ldapConf.url.startsWith("memserver") then
      logger.info("Using in-memory LDAP server for single connection")
      inMemoryServer.getConnection()
    else
      logger.info(s"Creating single LDAP connection to ${ldapConf.url}:${ldapConf.port}")
      new LDAPConnection(ldapConf.url, ldapConf.port)

  def createConnectionPool(): LDAPConnectionPool =
    if ldapConf.url.startsWith("memserver") then
      logger.info(s"Using in-memory LDAP server connection pool (size: ${ldapConf.poolSize})")
      inMemoryServer.getConnectionPool(ldapConf.poolSize)
    else
      logger.info(s"Creating LDAP connection pool to ${ldapConf.url}:${ldapConf.port} (size: ${ldapConf.poolSize})")
      val connection = new LDAPConnection(ldapConf.url, ldapConf.port)
      new LDAPConnectionPool(connection, ldapConf.poolSize)

  private def getLdifPath(ldapUrl: String): Option[String] =
    val tokens = ldapUrl.split(":")
    if tokens.length == 2 then Some(tokens(1))
    else None

  private def createInMemoryServer(ldif: Option[String]): InMemoryDirectoryServer =
    val config = new InMemoryDirectoryServerConfig("dc=pdg,dc=org")
    config.addAdditionalBindCredentials("uid=esurijon,ou=Users,dc=pdg,dc=org", "sarasa")
    config.setSchema(Schema.getDefaultStandardSchema)

    val ds = new InMemoryDirectoryServer(config)
    ldif.foreach(path => ds.importFromLDIF(true, path))
    ds.startListening()

    logger.info(s"In-memory LDAP server started${ldif.map(p => s" with LDIF: $p").getOrElse("")}")
    ds
