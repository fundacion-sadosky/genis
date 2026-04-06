package security

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

import play.api.i18n.{MessagesApi, Lang}
import services.{CacheService, RolePermissionMapKey, RolesKey}
import types.Permission
import types.Permission.LOGIN_SIGNUP
import user.RoleRepository

@Singleton
class RoleServiceImpl @Inject() (
    roleRepository: RoleRepository,
    cacheService: CacheService,
    userRepository: UserRepository,
    messagesApi: MessagesApi
)(using ec: ExecutionContext) extends RoleService:

  private def cleanCache(): Unit =
    cacheService.pop(RolesKey)(using summon[ClassTag[Seq[user.Role]]])
    cacheService.pop(RolePermissionMapKey)(using summon[ClassTag[Map[String, Set[Permission]]]])

  override def getRolePermissions(): Map[String, Set[Permission]] =
    cacheService.getOrElse(RolePermissionMapKey)(roleRepository.rolePermissionMap)(
      using summon[ClassTag[Map[String, Set[Permission]]]]
    )

  override def getRoles: Future[Seq[user.Role]] =
    cacheService.asyncGetOrElse(RolesKey)(roleRepository.getRoles)(
      using summon[ClassTag[Seq[user.Role]]], ec
    )

  override def addRole(role: user.Role): Future[Boolean] =
    roleRepository.addRole(role).flatMap {
      case true  => updateRole(role)
      case false => Future.successful(false)
    }

  override def updateRole(role: user.Role): Future[Boolean] =
    roleRepository.updateRole(role).map { result =>
      if result then cleanCache()
      result
    }

  override def deleteRole(id: String): Future[Either[String, Boolean]] =
    userRepository.listAllUsers().flatMap { users =>
      if users.count(_.roles.contains(id)) > 0 then
        Future.successful(Left(messagesApi("error.E0654")(Lang.defaultLang)))
      else
        roleRepository.deleteRole(id).map { res =>
          if res then cleanCache()
          Right(res)
        }
    }

  override def getRolesForSignUp(): Future[Seq[user.Role]] =
    getRoles.map(_.map(role => user.Role(role.id, role.roleName, Set.empty)))

  override def listPermissions(): Set[Permission] =
    Permission.list

  private def listFullOperations(): Set[StaticAuthorisationOperation] =
    (Permission.list + LOGIN_SIGNUP).flatMap(_.operations)

  override def listOperations(): Set[String] =
    listFullOperations().map(_.descriptionKey)

  override def translatePermission(action: String, resource: String): String =
    val validOperations = listFullOperations().filterNot { op =>
      regexMatchQuality(action, resource)(op) match
        case (0, _) => true
        case (_, 0) => true
        case _      => false
    }
    validOperations.minBy { op =>
      val q = regexMatchQuality(action, resource)(op)
      q._1 + q._2
    }.descriptionKey

  private def regexMatchQuality(action: String, resource: String)(operation: StaticAuthorisationOperation): (Int, Int) =
    (operation.action.findAllIn(action).size, operation.resource.findAllIn(resource).size)
