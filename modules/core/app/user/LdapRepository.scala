package user

import scala.jdk.CollectionConverters.*

import com.unboundid.ldap.sdk.*
import play.api.Logger

trait LdapRepository:
  val baseSearchConnection: LDAPConnection
  val baseBindConnectionPool: LDAPConnectionPool
  val baseDn: DN

  protected val logger: Logger = Logger(this.getClass)

  // Devuelve el atributo obligatorio o falla con un mensaje claro en vez de NPE crudo
  // cuando `getAttribute` retorna null (atributo ausente en el entry).
  protected def requiredAttribute(entry: SearchResultEntry, name: String): Attribute =
    Option(entry.getAttribute(name)).getOrElse(
      throw new NoSuchElementException(s"Missing required LDAP attribute '$name' for entry ${entry.getDN}")
    )

  protected def withConnection[A](handler: LDAPConnection => A): A =
    val connection = baseBindConnectionPool.getConnection()
    try
      handler(connection)
    finally
      baseBindConnectionPool.releaseConnection(connection)

  protected def searchOne(filter: Filter): Option[SearchResultEntry] =
    val request = new SearchRequest(baseDn.toString, SearchScope.SUB, filter)
    val result = baseSearchConnection.search(request)
    result.getSearchEntries.asScala.headOption

  protected def searchAll(filter: Filter): Seq[SearchResultEntry] =
    val request = new SearchRequest(baseDn.toString, SearchScope.SUB, filter)
    val result = baseSearchConnection.search(request)
    result.getSearchEntries.asScala.toSeq
