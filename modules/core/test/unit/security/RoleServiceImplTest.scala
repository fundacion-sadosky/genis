package unit.security

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}

import fixtures.{SecurityFixtures, UserFixtures}
import play.api.i18n.DefaultMessagesApi
import security.{LdapUser, RoleServiceImpl, UserRepository}
import services.RolePermissionMapKey
import types.Permission
import user.{Role, RoleRepository}

class RoleServiceImplTest extends AnyWordSpec with Matchers with MockitoSugar {

  given ExecutionContext = ExecutionContext.global
  private val timeout: Duration = Duration(5, "seconds")

  private val messagesApi = new DefaultMessagesApi(Map(
    "default" -> Map(
      "error.E0654" -> "E0654: Existen usuarios asociados al rol."
    )
  ))

  private def buildService(
      roleRepo: RoleRepository = mock[RoleRepository],
      cache: fixtures.StubCacheService = new fixtures.StubCacheService,
      userRepo: UserRepository = mock[UserRepository]
  ): RoleServiceImpl =
    new RoleServiceImpl(roleRepo, cache, userRepo, messagesApi)

  // ── getRolePermissions (existing tests) ─────────────────────────

  "RoleServiceImpl.getRolePermissions" must {

    "call roleRepository.rolePermissionMap and cache the result on first call" in {
      val cache = new fixtures.StubCacheService
      val roleRepo = mock[RoleRepository]
      when(roleRepo.rolePermissionMap).thenReturn(SecurityFixtures.rolePermissionMap)

      val service = buildService(roleRepo = roleRepo, cache = cache)

      val result = service.getRolePermissions()

      result mustBe SecurityFixtures.rolePermissionMap
      verify(roleRepo, times(1)).rolePermissionMap
    }

    "return cached value without calling repository on subsequent calls" in {
      val cache = new fixtures.StubCacheService
      val roleRepo = mock[RoleRepository]
      when(roleRepo.rolePermissionMap).thenReturn(SecurityFixtures.rolePermissionMap)

      val service = buildService(roleRepo = roleRepo, cache = cache)

      service.getRolePermissions()
      val result = service.getRolePermissions()

      result mustBe SecurityFixtures.rolePermissionMap
      verify(roleRepo, times(1)).rolePermissionMap
    }
  }

  // ── deleteRole ──────────────────────────────────────────────────

  "RoleServiceImpl.deleteRole" must {

    "reject deletion when role is assigned to users" in {
      val userRepo = mock[UserRepository]
      val ldapUser = UserFixtures.ldapUser.copy(roles = Seq("geneticist"))
      when(userRepo.listAllUsers()).thenReturn(Future.successful(Seq(ldapUser)))

      val service = buildService(userRepo = userRepo)

      val result = Await.result(service.deleteRole("geneticist"), timeout)

      result.isLeft mustBe true
      result match
        case Left(error) => error must include("E0654")
        case _ => fail("Expected Left")
    }

    "succeed when role has no assigned users" in {
      val userRepo = mock[UserRepository]
      when(userRepo.listAllUsers()).thenReturn(Future.successful(Seq.empty))

      val roleRepo = mock[RoleRepository]
      when(roleRepo.deleteRole("orphan-role")).thenReturn(Future.successful(true))

      val service = buildService(roleRepo = roleRepo, userRepo = userRepo)

      val result = Await.result(service.deleteRole("orphan-role"), timeout)

      result mustBe Right(true)
    }
  }

  // ── listPermissions ─────────────────────────────────────────────

  "RoleServiceImpl.listPermissions" must {

    "return non-empty Permission set" in {
      val service = buildService()

      val result = service.listPermissions()

      result.size must be > 0
      result must contain(Permission.DNA_PROFILE_CRUD)
    }
  }

  // ── listOperations ──────────────────────────────────────────────

  "RoleServiceImpl.listOperations" must {

    "return non-empty operation description set" in {
      val service = buildService()

      val result = service.listOperations()

      result.size must be > 0
      result must contain("Login")
    }
  }

  // ── translatePermission ─────────────────────────────────────────

  "RoleServiceImpl.translatePermission" must {

    "translate GET /geneticist/50 to GeneticistRead" in {
      val service = buildService()

      val result = service.translatePermission("GET", "/geneticist/50")

      result mustBe "GeneticistRead"
    }
  }

  // ── addRole ─────────────────────────────────────────────────────

  "RoleServiceImpl.addRole" must {

    "call repo.addRole then updateRole and clean cache" in {
      val roleRepo = mock[RoleRepository]
      when(roleRepo.addRole(any[Role])).thenReturn(Future.successful(true))
      when(roleRepo.updateRole(any[Role])).thenReturn(Future.successful(true))

      val service = buildService(roleRepo = roleRepo)

      val role = Role("new-role", "New Role", Set(Permission.DNA_PROFILE_CRUD))
      val result = Await.result(service.addRole(role), timeout)

      result mustBe true
      verify(roleRepo).addRole(role)
      verify(roleRepo).updateRole(role)
    }

    "return false when repo.addRole fails" in {
      val roleRepo = mock[RoleRepository]
      when(roleRepo.addRole(any[Role])).thenReturn(Future.successful(false))

      val service = buildService(roleRepo = roleRepo)

      val role = Role("fail-role", "Fail", Set.empty)
      val result = Await.result(service.addRole(role), timeout)

      result mustBe false
    }
  }

  // ── updateRole ──────────────────────────────────────────────────

  "RoleServiceImpl.updateRole" must {

    "delegate to repo and return result" in {
      val roleRepo = mock[RoleRepository]
      when(roleRepo.updateRole(any[Role])).thenReturn(Future.successful(true))

      val service = buildService(roleRepo = roleRepo)

      val role = Role("existing", "Updated", Set(Permission.USER_CRUD))
      val result = Await.result(service.updateRole(role), timeout)

      result mustBe true
    }
  }

  // ── getRoles ────────────────────────────────────────────────────

  "RoleServiceImpl.getRoles" must {

    "return roles from repository via cache" in {
      val roleRepo = mock[RoleRepository]
      val roles = Seq(SecurityFixtures.geneticistRole, SecurityFixtures.clerkRole)
      when(roleRepo.getRoles).thenReturn(Future.successful(roles))

      val service = buildService(roleRepo = roleRepo)

      val result = Await.result(service.getRoles, timeout)

      result mustBe roles
    }
  }

  // ── getRolesForSignUp ───────────────────────────────────────────

  "RoleServiceImpl.getRolesForSignUp" must {

    "return roles with empty permissions" in {
      val roleRepo = mock[RoleRepository]
      val roles = Seq(SecurityFixtures.geneticistRole)
      when(roleRepo.getRoles).thenReturn(Future.successful(roles))

      val service = buildService(roleRepo = roleRepo)

      val result = Await.result(service.getRolesForSignUp(), timeout)

      result.size mustBe 1
      result.head.id mustBe "geneticist"
      result.head.permissions mustBe empty
    }
  }
}
