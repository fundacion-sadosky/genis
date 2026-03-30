package integration.user

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Tag}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

import com.unboundid.ldap.sdk.*

import user.{LdapUserRepository, UserStatus}
import security.LdapUser

class LdapUserRepositoryIntegrationTest
    extends AnyWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach:

  private val timeout = 10.seconds
  private val ldapHost = "localhost"
  private val ldapPort = 1389
  private val usersDn = "ou=Users,dc=genis,dc=local"
  private val adminDn = "cn=admin,dc=genis,dc=local"
  private val adminPassword = "adminp"

  private val testUserId = "TEST_INT_USER"
  private val testUserDn = s"uid=$testUserId,$usersDn"

  private var connectionPool: LDAPConnectionPool = _
  private var searchConnection: LDAPConnection = _
  private var repo: LdapUserRepository = _

  private def testUser = LdapUser(
    userName = testUserId,
    firstName = "Test",
    lastName = "Integration",
    email = "test@genis.local",
    roles = Seq("admin"),
    geneMapperId = "tgm",
    phone1 = "1234567890",
    phone2 = Some("0987654321"),
    status = UserStatus.pending,
    encryptedPublicKey = Array.emptyByteArray,
    encryptedPrivateKey = Array.emptyByteArray,
    encryptrdTotpSecret = "TESTSECRET".getBytes,
    superuser = false
  )

  override protected def beforeAll(): Unit =
    super.beforeAll()
    searchConnection = new LDAPConnection(ldapHost, ldapPort)
    val poolConnection = new LDAPConnection(ldapHost, ldapPort)
    connectionPool = new LDAPConnectionPool(poolConnection, 2)
    repo = new LdapUserRepository(connectionPool, searchConnection, usersDn,
      adminDn, adminPassword)

  override protected def beforeEach(): Unit =
    super.beforeEach()
    deleteTestUserIfExists()

  override protected def afterAll(): Unit =
    deleteTestUserIfExists()
    connectionPool.close()
    searchConnection.close()
    super.afterAll()

  private def deleteTestUserIfExists(): Unit =
    try
      val conn = new LDAPConnection(ldapHost, ldapPort)
      try
        conn.bind(new SimpleBindRequest(new DN(adminDn), adminPassword))
        conn.delete(testUserDn)
      catch
        case _: LDAPException => // entry does not exist, ignore
      finally
        conn.close()
    catch
      case _: LDAPException => // connection issue, ignore

  "LdapUserRepository.bind" must {
    "return true for valid credentials" taggedAs Tag("integration") in {
      val result = Await.result(repo.bind("setup", "pass"), timeout)
      result mustBe true
    }

    "return false for wrong password" taggedAs Tag("integration") in {
      val result = Await.result(repo.bind("setup", "wrongpassword"), timeout)
      result mustBe false
    }

    "return false for non-existent user" taggedAs Tag("integration") in {
      val result = Await.result(repo.bind("nonexistent_user_xyz", "pass"), timeout)
      result mustBe false
    }
  }

  "LdapUserRepository.get" must {
    "return LdapUser with correct fields for existing user" taggedAs Tag("integration") in {
      val user = Await.result(repo.get("setup"), timeout)
      user.userName mustBe "setup"
      user.firstName mustBe "s"
      user.lastName mustBe "etup"
      user.email mustBe "setup@genis.local"
      user.geneMapperId mustBe "sgm"
      user.phone1 mustBe "0000000000"
      user.superuser mustBe false
    }

    "throw NoSuchElementException for non-existent user" taggedAs Tag("integration") in {
      val ex = intercept[NoSuchElementException] {
        Await.result(repo.get("nonexistent_user_xyz"), timeout)
      }
      ex.getMessage must include("nonexistent_user_xyz")
    }

    "map phone2 as None when mobile attribute is absent" taggedAs Tag("integration") in {
      val user = Await.result(repo.get("setup"), timeout)
      user.phone2 mustBe None
    }

    "map UserStatus from LDAP street attribute" taggedAs Tag("integration") in {
      val user = Await.result(repo.get("setup"), timeout)
      user.status mustBe UserStatus.active
    }

    "map roles from employeeType attribute" taggedAs Tag("integration") in {
      val user = Await.result(repo.get("setup"), timeout)
      user.roles must contain("admin")
      user.roles must not be empty
    }
  }

  "LdapUserRepository.listAllUsers" must {
    "return a non-empty list containing the setup user" taggedAs Tag("integration") in {
      val users = Await.result(repo.listAllUsers(), timeout)
      users must not be empty
      users.map(_.userName) must contain("setup")
    }
  }

  "LdapUserRepository.getOrEmpty" must {
    "return Some(LdapUser) for an existing user" taggedAs Tag("integration") in {
      val result = Await.result(repo.getOrEmpty("setup"), timeout)
      result mustBe defined
      result.get.userName mustBe "setup"
    }

    "return None for a non-existent user" taggedAs Tag("integration") in {
      val result = Await.result(repo.getOrEmpty("nonexistent_user_xyz"), timeout)
      result mustBe None
    }
  }

  "LdapUserRepository.findByRole" must {
    "return users with the specified role" taggedAs Tag("integration") in {
      val users = Await.result(repo.findByRole("admin"), timeout)
      users must not be empty
      users.map(_.userName) must contain("setup")
    }

    "return empty list for a non-existent role" taggedAs Tag("integration") in {
      val users = Await.result(repo.findByRole("nonexistent_role_xyz"), timeout)
      users mustBe empty
    }
  }

  "LdapUserRepository.findByStatus" must {
    "return users with active status" taggedAs Tag("integration") in {
      val users = Await.result(repo.findByStatus(UserStatus.active), timeout)
      users must not be empty
      users.map(_.userName) must contain("setup")
    }

    "return empty list for a status with no users" taggedAs Tag("integration") in {
      val users = Await.result(repo.findByStatus(UserStatus.blocked), timeout)
      users.map(_.userName) must not contain "setup"
    }
  }

  "LdapUserRepository.findByGeneMapper" must {
    "return the user matching the gene mapper id" taggedAs Tag("integration") in {
      val result = Await.result(repo.findByGeneMapper("sgm"), timeout)
      result mustBe defined
      result.get.userName mustBe "setup"
    }

    "return None for a non-existent gene mapper id" taggedAs Tag("integration") in {
      val result = Await.result(repo.findByGeneMapper("nonexistent_gm_xyz"), timeout)
      result mustBe None
    }
  }

  "LdapUserRepository.findSuperUsers" must {
    "not include setup user (superuser=false)" taggedAs Tag("integration") in {
      val users = Await.result(repo.findSuperUsers(), timeout)
      users.map(_.userName) must not contain "setup"
    }
  }

  "LdapUserRepository.create" must {
    "create a new user and return the userName" taggedAs Tag("integration") in {
      val result = Await.result(repo.create(testUser, "testpass123"), timeout)
      result mustBe testUserId

      val created = Await.result(repo.get(testUserId), timeout)
      created.userName mustBe testUserId
      created.firstName mustBe "Test"
      created.lastName mustBe "Integration"
      created.email mustBe "test@genis.local"
      created.roles must contain("admin")
      created.geneMapperId mustBe "tgm"
      created.phone1 mustBe "1234567890"
      created.phone2 mustBe Some("0987654321")
      created.status mustBe UserStatus.pending
      created.superuser mustBe false
    }

    "allow the created user to bind with the given password" taggedAs Tag("integration") in {
      Await.result(repo.create(testUser, "testpass123"), timeout)

      val bindResult = Await.result(repo.bind(testUserId, "testpass123"), timeout)
      bindResult mustBe true
    }
  }

  "LdapUserRepository.updateUser" must {
    "update user fields and return true" taggedAs Tag("integration") in {
      Await.result(repo.create(testUser, "testpass123"), timeout)

      val updatedUser = testUser.copy(
        firstName = "Updated",
        lastName = "Name",
        email = "updated@genis.local",
        geneMapperId = "ugm",
        phone1 = "1111111111",
        phone2 = Some("2222222222"),
        superuser = true
      )
      val result = Await.result(repo.updateUser(updatedUser), timeout)
      result mustBe true

      val fetched = Await.result(repo.get(testUserId), timeout)
      fetched.firstName mustBe "Updated"
      fetched.lastName mustBe "Name"
      fetched.email mustBe "updated@genis.local"
      fetched.geneMapperId mustBe "ugm"
      fetched.phone1 mustBe "1111111111"
      fetched.phone2 mustBe Some("2222222222")
      fetched.superuser mustBe true
    }

    "clear phone2 when set to None" taggedAs Tag("integration") in {
      Await.result(repo.create(testUser, "testpass123"), timeout)

      val updatedUser = testUser.copy(phone2 = None)
      Await.result(repo.updateUser(updatedUser), timeout)

      val fetched = Await.result(repo.get(testUserId), timeout)
      fetched.phone2 mustBe None
    }
  }

  "LdapUserRepository.setStatus" must {
    "change the user status" taggedAs Tag("integration") in {
      Await.result(repo.create(testUser, "testpass123"), timeout)

      val before = Await.result(repo.get(testUserId), timeout)
      before.status mustBe UserStatus.pending

      Await.result(repo.setStatus(testUserId, UserStatus.active), timeout)

      val after = Await.result(repo.get(testUserId), timeout)
      after.status mustBe UserStatus.active
    }
  }

  "LdapUserRepository.clearPass" must {
    "update status, totp secret, and password" taggedAs Tag("integration") in {
      Await.result(repo.create(testUser, "testpass123"), timeout)

      val newTotpSecret = "NEWSECRET".getBytes
      val result = Await.result(
        repo.clearPass(testUserId, UserStatus.pending_reset, "newpass456", newTotpSecret),
        timeout
      )
      result mustBe testUserId

      val fetched = Await.result(repo.get(testUserId), timeout)
      fetched.status mustBe UserStatus.pending_reset

      val bindResult = Await.result(repo.bind(testUserId, "newpass456"), timeout)
      bindResult mustBe true

      val oldBindResult = Await.result(repo.bind(testUserId, "testpass123"), timeout)
      oldBindResult mustBe false
    }
  }
