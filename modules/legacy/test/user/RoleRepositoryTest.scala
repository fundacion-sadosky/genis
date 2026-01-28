package user

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import javax.inject.Inject
import javax.inject.Singleton
import specs.PdgSpec
import scala.util.Random
import security.CryptoServiceImpl
import types.Permission
import play.api.libs.concurrent.Akka
import org.scalatest.Ignore


@Ignore
class RoleRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)

  val factory = new LdapConnectionPoolFactory(app.configuration.getConfig("ldap.default").get)

  val cp = factory.createConnectionPool

  val conn = factory.createSingleConnection

  val rolesDn = app.configuration.getString("ldap.default.rolesDn").get

  val adminDn = app.configuration.getString("ldap.default.adminDn").get

  val adminPassword = app.configuration.getString("ldap.default.adminPassword").get

  "A Role respository" must {
    "get al available roles" in {
      val repo = new LdapRoleRepository(Akka.system, cp, conn, rolesDn, adminDn, adminPassword)
      val f = repo.getRoles
      val res = Await.result(f, duration)

      res.toSeq mustBe Seq(Role("admin","Administrador",Set(Permission.PEDIGREE_CRUD, Permission.BIO_MAT_CRUD, Permission.LABORATORY_CRUD, Permission.ALLELIC_FREQ_DB_CRUD, Permission.ALLELIC_FREQ_DB_VIEW, Permission.CATEGORY_CRUD, Permission.USER_CRUD, Permission.ROLE_CRUD, Permission.GENETICIST_CRUD)),
          Role("role1","Description for Role 1",Set(Permission.DNA_PROFILE_CRUD, Permission.PROFILE_DATA_CRUD, Permission.ROLE_CRUD)), 
          Role("role2","Description for Role 2",Set()))
    }
  }
  
  "A Role respository" must {
    "add a role" in {
      val repo = new LdapRoleRepository(Akka.system, cp, conn, rolesDn, adminDn, adminPassword)
      val f = repo.addRole(Role("role3", "new role", Set()))
      val res = Await.result(f, duration)
      res mustBe true
    }
  }
  
  "A Role respository" must {
    "update a role" in {
      val repo = new LdapRoleRepository(Akka.system, cp, conn, rolesDn, adminDn, adminPassword)
      val f = repo.updateRole(Role("role1", "Role 1", Set(Permission.ALLELIC_FREQ_DB_CRUD, Permission.ALLELIC_FREQ_DB_VIEW)))
      val res = Await.result(f, duration)
      res mustBe true
    }
  }
  
  "A Role respository" must {
    "delete a role" in {
      val repo = new LdapRoleRepository(Akka.system, cp, conn, rolesDn, adminDn, adminPassword)
      val f = repo.deleteRole("role2")
      val res = Await.result(f, duration)
      res mustBe true
    }
  }
}