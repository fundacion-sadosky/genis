package user

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*

import com.unboundid.ldap.sdk.*
import play.api.Logger

trait LdapRepository:
  val baseSearchConnection: LDAPConnection
  val baseBindConnectionPool: LDAPConnectionPool
  val baseDn: DN

  protected val logger: Logger = Logger(this.getClass)

  protected def withConnection[A](handler: LDAPConnection => A): A =
    val connection = baseBindConnectionPool.getConnection()
    try
      handler(connection)
    finally
      baseBindConnectionPool.releaseConnection(connection)

  private def doSearch(request: SearchRequest): SearchResult =
    try baseSearchConnection.search(request)
    catch case _: LDAPException =>
      logger.warn("LDAP search connection lost, reconnecting...")
      baseSearchConnection.reconnect()
      baseSearchConnection.search(request)

  protected def searchOne(filter: Filter): Option[SearchResultEntry] =
    val request = new SearchRequest(baseDn.toString, SearchScope.SUB, filter)
    doSearch(request).getSearchEntries.asScala.headOption

  protected def searchAll(filter: Filter): Seq[SearchResultEntry] =
    val request = new SearchRequest(baseDn.toString, SearchScope.SUB, filter)
    doSearch(request).getSearchEntries.asScala.toSeq