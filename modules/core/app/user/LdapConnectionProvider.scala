package user

import com.unboundid.ldap.sdk.*
import play.api.Logger

trait LdapConnectionProvider:
  def withConnection[A](handler: LDAPConnection => A): A
  def search(request: SearchRequest): SearchResult

class LdapConnectionProviderImpl(
    pool: LDAPConnectionPool,
    searchConnection: LDAPConnection
) extends LdapConnectionProvider:

  private val logger = Logger(this.getClass)

  override def withConnection[A](handler: LDAPConnection => A): A =
    val connection = pool.getConnection()
    try
      handler(connection)
    finally
      pool.releaseConnection(connection)

  override def search(request: SearchRequest): SearchResult =
    try searchConnection.search(request)
    catch case _: LDAPException =>
      logger.warn("LDAP search connection lost, reconnecting...")
      searchConnection.reconnect()
      searchConnection.search(request)
