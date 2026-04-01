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
  def addRole(role: Role): Future[Boolean]
  def updateRole(role: Role): Future[Boolean]
  def deleteRole(id: String): Future[Boolean]

@Singleton
class LdapRoleRepository @Inject() (
    bindConnectionPool: LDAPConnectionPool,
    searchConnection: LDAPConnection,
    @Named("rolesDn") rolesDn: String,
    @Named("adminDn") adminDn: String,
    @Named("adminPassword") adminPassword: String
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

  private def withAdminConnection[A](handler: LDAPConnection => A): A =
    withConnection { connection =>
      val bindRequest = new SimpleBindRequest(new DN(adminDn), adminPassword)
      val bindResult = connection.bind(bindRequest)
      if bindResult.getResultCode == ResultCode.SUCCESS then
        handler(connection)
      else
        throw new LDAPException(bindResult.getResultCode)
    }

  override def addRole(role: Role): Future[Boolean] = Future {
    withAdminConnection { connection =>
      val existingRoles = Await.result(getRoles, 10.seconds)
      if existingRoles.exists(_.id == role.id) then
        Await.result(updateRole(role), 10.seconds)
      else
        val baseAttributes = Seq(
          new Attribute("cn", role.id),
          new Attribute("description", role.roleName),
          new Attribute("ou", "Roles"),
          new Attribute("objectclass", Seq("organizationalRole", "top")*)
        )
        val attributes = if role.permissions.nonEmpty then
          baseAttributes :+ new Attribute("street", role.permissions.map(_.toString).toSeq*)
        else baseAttributes
        val rDn = new RDN("cn", role.id)
        val dn = new DN(rDn, baseDn)
        val addRequest = new AddRequest(dn, attributes*)
        connection.add(addRequest)
        true
    }
  }

  override def updateRole(role: Role): Future[Boolean] = Future {
    withAdminConnection { connection =>
      val modRs = new Modification(ModificationType.REPLACE, "street", role.permissions.map(_.toString).toSeq*)
      val modDesc = new Modification(ModificationType.REPLACE, "description", role.roleName)
      val mods = Seq(modRs, modDesc)

      val rDn = new RDN("cn", role.id)
      val dn = new DN(rDn, baseDn)
      val modReq = new ModifyRequest(dn, mods*)

      connection.modify(modReq)
      true
    }
  }

  override def deleteRole(id: String): Future[Boolean] = Future {
    withAdminConnection { connection =>
      val rDn = new RDN("cn", id)
      val dn = new DN(rDn, baseDn)
      val delReq = new DeleteRequest(dn)
      connection.delete(delReq)
      true
    }
  }
