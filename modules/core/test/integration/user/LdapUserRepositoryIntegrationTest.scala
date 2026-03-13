package integration.user

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

import com.unboundid.ldap.sdk._

import user.{LdapUserRepository, UserStatus}

class LdapUserRepositoryIntegrationTest
    extends AnyWordSpec with Matchers with BeforeAndAfterAll:

  private val timeout = 10.seconds
  private val ldapHost = "localhost"
  private val ldapPort = 1389
  private val usersDn = "ou=Users,dc=genis,dc=local"

  private var connectionPool: LDAPConnectionPool = _
  private var searchConnection: LDAPConnection = _
  private var repo: LdapUserRepository = _

  override protected def beforeAll(): Unit =
    super.beforeAll()
    searchConnection = new LDAPConnection(ldapHost, ldapPort)
    val poolConnection = new LDAPConnection(ldapHost, ldapPort)
    connectionPool = new LDAPConnectionPool(poolConnection, 2)
    repo = new LdapUserRepository(connectionPool, searchConnection, usersDn)

  override protected def afterAll(): Unit =
    connectionPool.close()
    searchConnection.close()
    super.afterAll()

  "LdapUserRepository.bind" must {
    "return true for valid credentials" in {
      val result = Await.result(repo.bind("setup", "pass"), timeout)
      result mustBe true
    }

    "return false for wrong password" in {
      val result = Await.result(repo.bind("setup", "wrongpassword"), timeout)
      result mustBe false
    }

    "return false for non-existent user" in {
      val result = Await.result(repo.bind("nonexistent_user_xyz", "pass"), timeout)
      result mustBe false
    }
  }

  "LdapUserRepository.get" must {
    "return LdapUser with correct fields for existing user" in {
      val user = Await.result(repo.get("setup"), timeout)
      user.userName mustBe "setup"
      user.firstName mustBe "s"
      user.lastName mustBe "etup"
      user.email mustBe "setup@genis.local"
      user.geneMapperId mustBe "setup"
      user.phone1 mustBe "0000000000"
      user.superuser mustBe true
    }

    "throw NoSuchElementException for non-existent user" in {
      val ex = intercept[NoSuchElementException] {
        Await.result(repo.get("nonexistent_user_xyz"), timeout)
      }
      ex.getMessage must include("nonexistent_user_xyz")
    }

    "map phone2 as None when mobile attribute is absent" in {
      val user = Await.result(repo.get("setup"), timeout)
      user.phone2 mustBe None
    }

    "map UserStatus from LDAP street attribute" in {
      val user = Await.result(repo.get("setup"), timeout)
      user.status mustBe UserStatus.active
    }

    "map roles from employeeType attribute" in {
      val user = Await.result(repo.get("setup"), timeout)
      user.roles must contain("admin")
      user.roles must not be empty
    }
  }
