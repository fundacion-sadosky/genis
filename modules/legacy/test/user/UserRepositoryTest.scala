package user


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import javax.inject.Inject
import javax.inject.Singleton
import org.scalatest.Ignore
import com.unboundid.ldap.sdk.LDAPException
import specs.PdgSpec
import scala.util.Random
import security.CryptoServiceImpl
import play.api.libs.concurrent.Akka

@Ignore
class UserRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)

  val factory = new LdapConnectionPoolFactory(app.configuration.getConfig("ldap.default").get)

  val cp = factory.createConnectionPool

  val conn = factory.createSingleConnection

  val usersDn = app.configuration.getString("ldap.default.usersDn").get

  val adminDn = app.configuration.getString("ldap.default.adminDn").get

  val adminPassword = app.configuration.getString("ldap.default.adminPassword").get

  "A UserRepository" must {

    /*SOLO UTIL PARA ADECUAR LOS USUARIOS VIEJOS AL NUEVO FORMATO
   * "A User respository" must {
    "set TOTP" in {
      val userRepository = new LdapUserRepository(app, cp, conn, usersDn, adminDn, adminPassword)
      
      val crypto = new CryptoServiceImpl(2048)
      val credentials = crypto.generateDerivatedCredentials("soyadmintist")
      val totpenc = crypto.encrypt("K5AXU2S527ML7BWE".getBytes, credentials)
      
      val bindFuture = userRepository.setTOTP("tst-admintist", totpenc)
      val success = Await.result(bindFuture, duration)
      
      
      val futUser = userRepository.get("tst-admintist")
      val user = Await.result(futUser, duration)
      
      println(user)
      
      success mustBe true
    }

  }*/

    "authenticate and retrieve user against LDAP" in {
      val userRepository = new LdapUserRepository(Akka.system, cp, conn, usersDn, adminDn, adminPassword)
      val bindFuture = userRepository.bind("esurijon", "sarasa")
      val success = Await.result(bindFuture, duration)
      success mustBe true
    }

    "retrieve user details from LDAP" in {
      val userRepository = new LdapUserRepository(Akka.system, cp, conn, usersDn, adminDn, adminPassword)
      val bindFuture = userRepository.get("esurijon")
      val success = Await.result(bindFuture, duration)
    }

    "retrieve users by role" in {
      val userRepository = new LdapUserRepository(Akka.system, cp, conn, usersDn, adminDn, adminPassword)
      val findFuture = userRepository.findByRole("clerk")
      val users = Await.result(findFuture, duration)
      users.size mustBe 1
    }

    "create user" in {

      val userRepository = new LdapUserRepository(Akka.system, cp, conn, usersDn, adminDn, adminPassword)

      val uid = "pepe"
      val password = "password"
      val user = LdapUser(
        uid,
        "eze",
        "suri",
        "email",
        Seq("admin"),
        "uid",
        "41188080",
        None,
        UserStatus.pending,
        "123456789".getBytes(),
        "123456789".getBytes(),
        "123456789".getBytes())

      val addFuture = userRepository.create(user, "password")

      Await.result(addFuture, duration)

      val bindFuture = userRepository.bind(uid, password)
      val bindResult = Await.result(bindFuture, duration)
      bindResult mustBe true
    }

    /*
    "modify the status of a user" in {
      
      val userRepository = new LdapUserRepository(app, cp, conn, usersDn, adminDn, adminPassword)
      
      val result = userRepository.setStatus("mbravo", UserStatus.active)
      
      val realResult = Await.result(result, duration)
      
      println(realResult)
      
    }
*/

    "list all users" in {
      val userRepository = new LdapUserRepository(Akka.system, cp, conn, usersDn, adminDn, adminPassword)
      val findFuture = userRepository.listAllUsers()
      val users = Await.result(findFuture, duration)
    }


//    "list all roles" in {
//      val roleRepository = new LdapLogRepository (app, cp, conn)
//      val findFuture = roleRepository.listAllRoles()
//      val roles = Await.result(findFuture, duration)
//      println(roles)
//    }

    "update user details" in {

      val userRepository = new LdapUserRepository(Akka.system, cp, conn, usersDn, adminDn, adminPassword)

      val uid = "esurijon"
      val password = "password"
      val modifiedUser = LdapUser(uid, "eze", "suri", "email", Seq("admin"), "esurijon", "41114444", Some("1541114444"),
        UserStatus.active, Array.emptyByteArray, Array.emptyByteArray, Array.emptyByteArray)

      val updateFuture = userRepository.updateUser(modifiedUser)
      Await.result(updateFuture, duration)

      val user = Await.result(userRepository.get("esurijon"), duration)

      user.firstName mustBe "eze"
      user.lastName mustBe "suri"
      user.email mustBe "email"
      user.roles mustBe Seq("admin")
      user.phone1 mustBe "41114444"
      user.phone2 mustBe Some("1541114444")
      user.status mustBe UserStatus.active

    }

    "update user with no mobile" in {

      val userRepository = new LdapUserRepository(Akka.system, cp, conn, usersDn, adminDn, adminPassword)

      val uid = "esurijon"
      val password = "password"
      val modifiedUser = LdapUser(uid, "eze", "suri", "email", Seq("admin"), "esurijon", "41114444", None,
        UserStatus.active, Array.emptyByteArray, Array.emptyByteArray, Array.emptyByteArray)

      val updateFuture = userRepository.updateUser(modifiedUser)
      Await.result(updateFuture, duration)

      val user = Await.result(userRepository.get("esurijon"), duration)

      user.phone2 mustBe None
    }

    "retrieve user details by gene mapper" in {
      val userRepository = new LdapUserRepository(Akka.system, cp, conn, usersDn, adminDn, adminPassword)
      val bindFuture = userRepository.findByGeneMapper("esurijon")
      val result = Await.result(bindFuture, duration)

      result.isDefined mustBe true
      result.get.userName mustBe "esurijon"
    }

    "retrieve user details by gene mapper - no results" in {
      val userRepository = new LdapUserRepository(Akka.system, cp, conn, usersDn, adminDn, adminPassword)
      val bindFuture = userRepository.findByGeneMapper("jerkovicm")
      val result = Await.result(bindFuture, duration)

      result.isDefined mustBe false
    }

    "retrieve user details by gene mapper multiple times" in {
      val userRepository = new LdapUserRepository(Akka.system, cp, conn, usersDn, adminDn, adminPassword)
      val bindFuture = userRepository.findByGeneMapper("esurijon")
      val result = Await.result(bindFuture, duration)

      Await.result(Future.sequence((1 to 10).map(n => userRepository.findByGeneMapper("esurijon"))), duration)

      result.isDefined mustBe true
      result.get.userName mustBe "esurijon"
    }

    "reset password" in {

      val userRepository = new LdapUserRepository(Akka.system, cp, conn, usersDn, adminDn, adminPassword)

      val uid = "pepe2"
      val password = "password"
      val user = LdapUser(
        uid,
        "eze",
        "suri",
        "email",
        Seq("admin"),
        "uid",
        "41188080",
        None,
        UserStatus.pending,
        "123456789".getBytes(),
        "123456789".getBytes(),
        "123456789".getBytes())

      val addFuture = userRepository.create(user, "password")

      Await.result(addFuture, duration)

      val bindFuture = userRepository.bind(uid, password)
      val bindResult = Await.result(bindFuture, duration)
      bindResult mustBe true

      val resetPassFuture = Await.result(userRepository.clearPass("pepe2",UserStatus.pending,"qwer123","asda".getBytes()), duration)

      resetPassFuture mustBe "pepe2"

    }

    "reset password with inexistent user" in {

      val userRepository = new LdapUserRepository(Akka.system, cp, conn, usersDn, adminDn, adminPassword)

      val uid = "pepe3"
      val password = "password"
      val user = LdapUser(
        uid,
        "eze",
        "suri",
        "email",
        Seq("admin"),
        "uid",
        "41188080",
        None,
        UserStatus.pending,
        "123456789".getBytes(),
        "123456789".getBytes(),
        "123456789".getBytes())

      val addFuture = userRepository.create(user, "password")

      Await.result(addFuture, duration)

      val bindFuture = userRepository.bind(uid, password)
      val bindResult = Await.result(bindFuture, duration)
      bindResult mustBe true

      a[LDAPException] must be thrownBy {
        Await.result(userRepository.clearPass("pepe4", UserStatus.pending, "qwer123", "asda".getBytes()), duration)
      }

    }

  }

}