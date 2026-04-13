package user

import scala.jdk.CollectionConverters.*

import com.unboundid.ldap.sdk.*
import play.api.Logger

trait LdapRepository:
  val ldap: LdapConnectionProvider
  val baseDn: DN

  protected val logger: Logger = Logger(this.getClass)

  protected def withConnection[A](handler: LDAPConnection => A): A =
    ldap.withConnection(handler)

  protected def searchOne(filter: Filter): Option[SearchResultEntry] =
    val request = new SearchRequest(baseDn.toString, SearchScope.SUB, filter)
    ldap.search(request).getSearchEntries.asScala.headOption

  protected def searchAll(filter: Filter): Seq[SearchResultEntry] =
    val request = new SearchRequest(baseDn.toString, SearchScope.SUB, filter)
    ldap.search(request).getSearchEntries.asScala.toSeq
