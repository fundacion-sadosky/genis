package integration.user

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Tag}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

import com.unboundid.ldap.sdk._

import user.{LdapConnectionProviderImpl, LdapRoleRepository, Role}
import types.Permission

class LdapRoleRepositoryIntegrationTest
    extends AnyWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach:

  private val timeout = 10.seconds
  private val ldapHost = "localhost"
  private val ldapPort = 1389
  private val rolesDn = "ou=Roles,dc=genis,dc=local"

  private val adminDn = "cn=admin,dc=genis,dc=local"
  private val adminPassword = "adminp"
  private val testRoleId = "TEST_INT_ROLE"
  private val testRoleDn = s"cn=$testRoleId,$rolesDn"

  private var connectionPool: LDAPConnectionPool = _
  private var searchConnection: LDAPConnection = _
  private var repo: LdapRoleRepository = _

  override protected def beforeAll(): Unit =
    super.beforeAll()
    searchConnection = new LDAPConnection(ldapHost, ldapPort)
    val poolConnection = new LDAPConnection(ldapHost, ldapPort)
    connectionPool = new LDAPConnectionPool(poolConnection, 2)
    val provider = new LdapConnectionProviderImpl(connectionPool, searchConnection)
    repo = new LdapRoleRepository(provider, rolesDn, adminDn, adminPassword)

  override protected def afterAll(): Unit =
    connectionPool.close()
    searchConnection.close()
    super.afterAll()

  override protected def beforeEach(): Unit =
    super.beforeEach()
    deleteTestRoleIfExists()

  private def deleteTestRoleIfExists(): Unit =
    try
      val conn = new LDAPConnection(ldapHost, ldapPort)
      try
        conn.bind(new SimpleBindRequest(new DN(adminDn), adminPassword))
        conn.delete(new DeleteRequest(testRoleDn))
      catch case _: LDAPException => () // ignore if not found
      finally conn.close()
    catch case _: LDAPException => () // ignore connection errors

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

  "LdapRoleRepository.addRole" must {
    "add a new role and make it retrievable via getRoles" taggedAs Tag("integration") in {
      val role = Role(testRoleId, "Test Role", Set(Permission.KIT_CRUD))
      val result = Await.result(repo.addRole(role), timeout)
      result mustBe true

      val roles = Await.result(repo.getRoles, timeout)
      val found = roles.find(_.id == testRoleId)
      found mustBe defined
      found.get.roleName mustBe "Test Role"
      found.get.permissions must contain(Permission.KIT_CRUD)
    }

    "delegate to updateRole when role already exists" taggedAs Tag("integration") in {
      val role = Role(testRoleId, "Test Role", Set(Permission.KIT_CRUD))
      Await.result(repo.addRole(role), timeout)

      val updatedRole = Role(testRoleId, "Updated Test Role", Set(Permission.KIT_CRUD, Permission.USER_CRUD))
      val result = Await.result(repo.addRole(updatedRole), timeout)
      result mustBe true

      val roles = Await.result(repo.getRoles, timeout)
      val found = roles.find(_.id == testRoleId)
      found mustBe defined
      found.get.roleName mustBe "Updated Test Role"
      found.get.permissions must contain(Permission.KIT_CRUD)
      found.get.permissions must contain(Permission.USER_CRUD)

      // Verify no duplicates were created
      roles.count(_.id == testRoleId) mustBe 1
    }
  }

  "LdapRoleRepository.updateRole" must {
    "update permissions and description of an existing role" taggedAs Tag("integration") in {
      val role = Role(testRoleId, "Test Role", Set(Permission.KIT_CRUD))
      Await.result(repo.addRole(role), timeout)

      val updatedRole = Role(testRoleId, "Modified Role", Set(Permission.USER_CRUD, Permission.DNA_PROFILE_CRUD))
      val result = Await.result(repo.updateRole(updatedRole), timeout)
      result mustBe true

      val roles = Await.result(repo.getRoles, timeout)
      val found = roles.find(_.id == testRoleId)
      found mustBe defined
      found.get.roleName mustBe "Modified Role"
      found.get.permissions mustBe Set(Permission.USER_CRUD, Permission.DNA_PROFILE_CRUD)
    }
  }

  "LdapRoleRepository.deleteRole" must {
    "delete an existing role so it no longer appears in getRoles" taggedAs Tag("integration") in {
      val role = Role(testRoleId, "Test Role", Set(Permission.KIT_CRUD))
      Await.result(repo.addRole(role), timeout)

      // Confirm it exists before deletion
      val rolesBefore = Await.result(repo.getRoles, timeout)
      rolesBefore.exists(_.id == testRoleId) mustBe true

      val result = Await.result(repo.deleteRole(testRoleId), timeout)
      result mustBe true

      val rolesAfter = Await.result(repo.getRoles, timeout)
      rolesAfter.exists(_.id == testRoleId) mustBe false
    }
  }
