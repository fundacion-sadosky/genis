package integration.user

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

import com.unboundid.ldap.sdk._

import user.{LdapRoleRepository, Role}
import types.Permission

class LdapRoleRepositoryIntegrationTest
    extends AnyWordSpec with Matchers with BeforeAndAfterAll:

  private val timeout = 10.seconds
  private val ldapHost = "localhost"
  private val ldapPort = 1389
  private val rolesDn = "ou=Roles,dc=genis,dc=local"

  private var connectionPool: LDAPConnectionPool = _
  private var searchConnection: LDAPConnection = _
  private var repo: LdapRoleRepository = _

  override protected def beforeAll(): Unit =
    super.beforeAll()
    searchConnection = new LDAPConnection(ldapHost, ldapPort)
    val poolConnection = new LDAPConnection(ldapHost, ldapPort)
    connectionPool = new LDAPConnectionPool(poolConnection, 2)
    repo = new LdapRoleRepository(connectionPool, searchConnection, rolesDn)

  override protected def afterAll(): Unit =
    connectionPool.close()
    searchConnection.close()
    super.afterAll()

  "LdapRoleRepository.getRoles" must {
    "return all available roles" in {
      val roles = Await.result(repo.getRoles, timeout)
      roles must not be empty
      val roleIds = roles.map(_.id)
      roleIds must contain("admin")
    }

    "return roles with correct id, roleName and permissions" in {
      val roles = Await.result(repo.getRoles, timeout)
      val adminRole = roles.find(_.id == "admin")
      adminRole mustBe defined
      adminRole.get.roleName mustBe "Administrador"
      adminRole.get.permissions must not be empty
      adminRole.get.permissions must contain(Permission.KIT_CRUD)
    }
  }

  "LdapRoleRepository.rolePermissionMap" must {
    "return map of roleId to Set[Permission]" in {
      val permMap = repo.rolePermissionMap
      permMap must not be empty
      permMap must contain key "admin"
      permMap("admin") must contain(Permission.KIT_CRUD)
      permMap("admin") must contain(Permission.USER_CRUD)
    }

    "return Set.empty for non-existent roles via withDefaultValue" in {
      val permMap = repo.rolePermissionMap
      permMap("nonexistent_role_xyz") mustBe Set.empty
    }
  }
