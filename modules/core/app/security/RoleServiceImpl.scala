package security

import javax.inject.{Inject, Singleton}
import scala.reflect.ClassTag

import services.{CacheService, RolePermissionMapKey}
import types.Permission
import user.RoleRepository

@Singleton
class RoleServiceImpl @Inject() (
    roleRepository: RoleRepository,
    cacheService: CacheService
) extends RoleService:

  override def getRolePermissions(): Map[String, Set[Permission]] =
    cacheService.getOrElse(RolePermissionMapKey)(roleRepository.rolePermissionMap)(
      using summon[ClassTag[Map[String, Set[Permission]]]]
    )
