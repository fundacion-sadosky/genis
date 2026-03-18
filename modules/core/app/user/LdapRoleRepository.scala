package user

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*
import scala.util.Try
import scala.jdk.CollectionConverters.*

import com.unboundid.ldap.sdk.*
import javax.inject.{Inject, Named, Singleton}

import types.Permission

abstract class RoleRepository:
  def getRoles: Future[Seq[Role]]
  def rolePermissionMap: Map[String, Set[Permission]]

  // Métodos agregados para compatibilidad
  def addRole(role: Role): Future[Boolean]
  def updateRole(role: Role): Future[Boolean]
  def deleteRole(id: String): Future[Either[String, Boolean]]

@Singleton
class LdapRoleRepository @Inject() (
    bindConnectionPool: LDAPConnectionPool,
    searchConnection: LDAPConnection,
    @Named("rolesDn") rolesDn: String
)(using ec: ExecutionContext) extends RoleRepository with LdapRepository:

  val baseDn: DN = new DN(rolesDn)
  val baseSearchConnection: LDAPConnection = searchConnection
  val baseBindConnectionPool: LDAPConnectionPool = bindConnectionPool

  private val entryToLdapRole = (entry: SearchResultEntry) => Try {
    val p: Set[Permission] = entry.getAttributeValues("street") match
      case null => Set.empty
      case l    => l.flatMap(Permission.fromString).toSet
    Role(entry.getAttribute("cn").getValue, entry.getAttribute("description").getValue, p)
  }

  override def getRoles: Future[Seq[Role]] = Future {
    val filter = Filter.createEqualityFilter("objectClass", "organizationalRole")
    searchAll(filter).flatMap(entry => entryToLdapRole(entry).toOption)
  }

  override def rolePermissionMap: Map[String, Set[Permission]] =
    val roles = Await.result(getRoles, 10.seconds)
    logger.debug(roles.toString)
    roles.map(r => r.id -> r.permissions).toMap.withDefaultValue(Set.empty)

  // Stubs para métodos agregados
  override def addRole(role: Role): Future[Boolean] = Future.successful(false)
  override def updateRole(role: Role): Future[Boolean] = Future.successful(false)
  override def deleteRole(id: String): Future[Either[String, Boolean]] = Future.successful(Left("Not implemented"))
