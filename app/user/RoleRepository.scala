package user

import scala.collection.JavaConversions.seqAsJavaList
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import scala.util.Try

import com.unboundid.ldap.sdk.AddRequest
import com.unboundid.ldap.sdk.AsyncRequestID
import com.unboundid.ldap.sdk.AsyncResultListener
import com.unboundid.ldap.sdk.Attribute
import com.unboundid.ldap.sdk.BindResult
import com.unboundid.ldap.sdk.DN
import com.unboundid.ldap.sdk.DeleteRequest
import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPConnectionPool
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.LDAPResult
import com.unboundid.ldap.sdk.Modification
import com.unboundid.ldap.sdk.ModificationType
import com.unboundid.ldap.sdk.ModifyRequest
import com.unboundid.ldap.sdk.RDN
import com.unboundid.ldap.sdk.ResultCode
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.SimpleBindRequest

import akka.actor.ActorSystem
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import play.api.Logger
import types.Permission

abstract class RoleRepository {
  def getRoles: Future[Seq[Role]]
  def rolePermissionMap: Map[String, Set[Permission]]
  def addRole(role: Role): Future[Boolean]
  def updateRole(role: Role): Future[Boolean]
  def deleteRole(id: String): Future[Boolean]
}

@Singleton
class LdapRoleRepository @Inject() (
    val akkaSystem: ActorSystem,
    bindConnectionPool: LDAPConnectionPool,
    searchConnection: LDAPConnection,
    @Named("rolesDn") rolesDn: String,
    @Named("adminDn") adminDn: String,
    @Named("adminPassword") adminPassword: String) extends RoleRepository with LdapRepository {

  val ldapContextR = akkaSystem.dispatchers.lookup("play.akka.actor.ldap-context")

  val baseDn: DN = new DN(rolesDn)
  val baseSearchConnection = searchConnection
  val baseBindConnectionPool = bindConnectionPool

  override def getRoles: Future[Seq[Role]] = {
    val filter: Filter = Filter.createEqualityFilter("objectClass", "organizationalRole")
    findLdapObjects(filter, entryToLdapRole)
  }

  private val entryToLdapRole = (entry: SearchResultEntry) => Try {
    val p: Set[Permission] = entry.getAttributeValues("street") match {
      case null => Set()
      case l    => l.flatMap { Permission.fromString(_) }.toSet
    }
    Role(entry.getAttribute("cn").getValue(), entry.getAttribute("description").getValue(), p)
  }

  override def rolePermissionMap: Map[String, Set[Permission]] = {
    val l = Await.result(this.getRoles, Duration(10, SECONDS))
    logger.debug(l.toString())
    l.map { r => r.id -> r.permissions }.toMap.withDefault { _ => Set.empty }
  }

  override def addRole(role: Role): Future[Boolean] = {
    val future = Promise[Boolean]
    withConnection { connection =>
      val bindRequest = new SimpleBindRequest(new DN(adminDn), adminPassword)

      val bindResult: BindResult = connection.bind(bindRequest)

      if (bindResult.getResultCode() == ResultCode.SUCCESS) {

        val attributes = Seq(
          new Attribute("cn", role.id),
          new Attribute("description", role.roleName),
          new Attribute("ou", "Roles"),
          new Attribute("objectclass", Seq("organizationalRole", "top")))

        val rDn = new RDN("cn", role.id)
        val dn = new DN(rDn, baseDn)
        val addRequest = new AddRequest(dn, attributes)

        connection.add(addRequest)
        future.success(true)
      } else {
        future.failure(new LDAPException(bindResult.getResultCode()))
      }
    }
    future.future
  }

  override def updateRole(role: Role): Future[Boolean] = {

    val promise = Promise[Boolean]

    withConnection { connection =>
      val bindRequest = new SimpleBindRequest(new DN(adminDn), adminPassword)

      val bindResult: BindResult = connection.bind(bindRequest)

      if (bindResult.getResultCode() == ResultCode.SUCCESS) {

        val modRs = new Modification(ModificationType.REPLACE, "street", role.permissions.map(_.toString()).toArray: _*)
        val modDesc = new Modification(ModificationType.REPLACE, "description", role.roleName)
        val mods = Seq(modRs, modDesc)

        val rDn = new RDN("cn", role.id)
        val dn = new DN(rDn, baseDn)
        val modReq = new ModifyRequest(dn, mods)

        val resultListener = new AsyncResultListener {
          def ldapResultReceived(requestID: AsyncRequestID, ldapResult: LDAPResult) = {
            if (ldapResult.getResultCode == ResultCode.SUCCESS) {
              promise.success(true)
            } else {
              promise.failure(new LDAPException(ldapResult.getResultCode))
            }
            ()
          }
        }
        connection.asyncModify(modReq, resultListener)
      } else {
        promise.failure(new LDAPException(bindResult.getResultCode))
      }
    }
    promise.future
  }

  override def deleteRole(id: String): Future[Boolean] = {

    val promise = Promise[Boolean]

    withConnection { connection =>
      val bindRequest = new SimpleBindRequest(new DN(adminDn), adminPassword)

      val bindResult: BindResult = connection.bind(bindRequest)

      if (bindResult.getResultCode() == ResultCode.SUCCESS) {
        val rDn = new RDN("cn", id)
        val dn = new DN(rDn, baseDn)
        val delReq = new DeleteRequest(dn)

        val resultListener = new AsyncResultListener {
          def ldapResultReceived(requestID: AsyncRequestID, ldapResult: LDAPResult) = {
            if (ldapResult.getResultCode == ResultCode.SUCCESS) {
              promise.success(true)
            } else {
              promise.failure(new LDAPException(ldapResult.getResultCode))
            }
            ()
          }
        }
        connection.asyncDelete(delReq, resultListener)
      } else {
        promise.failure(new LDAPException(bindResult.getResultCode))
      }
    }
    promise.future
  }

}