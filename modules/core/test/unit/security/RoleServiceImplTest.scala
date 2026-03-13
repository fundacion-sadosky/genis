package unit.security

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.{times, verify, when}

import fixtures.SecurityFixtures
import security.RoleServiceImpl
import services.RolePermissionMapKey
import types.Permission
import user.RoleRepository

class RoleServiceImplTest extends AnyWordSpec with Matchers with MockitoSugar {

  "RoleServiceImpl.getRolePermissions" must {

    "call roleRepository.rolePermissionMap and cache the result on first call" in {
      val cache = new fixtures.StubCacheService
      val roleRepo = mock[RoleRepository]
      when(roleRepo.rolePermissionMap).thenReturn(SecurityFixtures.rolePermissionMap)

      val service = new RoleServiceImpl(roleRepo, cache)

      val result = service.getRolePermissions()

      result mustBe SecurityFixtures.rolePermissionMap
      verify(roleRepo, times(1)).rolePermissionMap
    }

    "return cached value without calling repository on subsequent calls" in {
      val cache = new fixtures.StubCacheService
      val roleRepo = mock[RoleRepository]
      when(roleRepo.rolePermissionMap).thenReturn(SecurityFixtures.rolePermissionMap)

      val service = new RoleServiceImpl(roleRepo, cache)

      // First call — populates cache
      service.getRolePermissions()
      // Second call — should use cache
      val result = service.getRolePermissions()

      result mustBe SecurityFixtures.rolePermissionMap
      // rolePermissionMap should have been called only once (first call)
      verify(roleRepo, times(1)).rolePermissionMap
    }
  }
}
