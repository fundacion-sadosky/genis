package user

import scala.concurrent.Future
import javax.inject.Inject
import javax.inject.Singleton
import types.Permission
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import security.StaticAuthorisationOperation
import services.CacheService
import services.Keys
import types.Permission.LOGIN_SIGNUP
import play.api.i18n.{Messages, MessagesApi}

abstract class RoleService {
  def getRolePermissions(): Map[String, Set[Permission]]
  def getRoles(): Future[Seq[Role]]
  def getRolesForSignUp(): Future[Seq[Role]]
  def addRole(role: Role): Future[Boolean]
  def updateRole(role: Role): Future[Boolean]
  def deleteRole(id: String): Future[Either[String, Boolean]]
  def listPermissions(): Set[Permission]
  def translatePermission(action: String, resource: String): String
  def listOperations(): Set[String]
}

@Singleton
class RoleServiceImpl @Inject() (messagesApi: MessagesApi, roleRepository: RoleRepository, cacheService: CacheService, userRepository: UserRepository) extends RoleService {

  private def cleanCache = {
    cacheService.pop(Keys.roles)
    cacheService.pop(Keys.rolePermissionMap)
  }

  override def getRoles: Future[Seq[Role]] = {
    cacheService.asyncGetOrElse(Keys.roles)(roleRepository.getRoles)
  }

  override def getRolesForSignUp(): Future[Seq[Role]] = {
    getRoles().flatMap(roles => Future.successful(roles.map(role => Role(role.id, role.roleName, Set.empty))))
  }

  override def getRolePermissions: Map[String, Set[Permission]] = {
    cacheService.getOrElse(Keys.rolePermissionMap)(roleRepository.rolePermissionMap)
  }

  override def listPermissions(): Set[Permission] = {
    Permission.list
  }

  private def listFullOperations(): Set[StaticAuthorisationOperation] = {
    (Permission.list + LOGIN_SIGNUP).flatMap(permission => permission.operations)
  }

  def listOperations(): Set[String] = {
    listFullOperations().map(operation => operation.descriptionKey)
  }

  override def translatePermission(action: String, resource: String): String = {
    val validOperations = listFullOperations().filterNot(op => regexMatchQuality(action, resource)(op) match {
      case (0, _) => true
      case (_, 0) => true
      case _ => false
    })
    validOperations.minBy({ op =>
      val q = regexMatchQuality(action, resource)(op)
      q._1 + q._2
    }).descriptionKey
  }

  def regexMatchQuality(action: String, resource: String)(operation: StaticAuthorisationOperation): (Int, Int) = {
    (operation.action.findAllIn(action).size, operation.resource.findAllIn(resource).size)
  }

  override def addRole(role: Role): Future[Boolean] = {
    val p = roleRepository.addRole(role)
    p.foreach(_ => cleanCache)
    p.flatMap{ case true =>
      updateRole(role)
    case false => Future.successful(false)
    }

  }

  override def updateRole(role: Role): Future[Boolean] = {
    val p = roleRepository.updateRole(role)
    p.foreach(_ => cleanCache)
    p
  }

  override def deleteRole(id: String): Future[Either[String, Boolean]] = {
    userRepository.listAllUsers().flatMap { l =>
      implicit val messages: Messages = messagesApi.preferred(Seq.empty)
      if (l.count { _.roles.contains(id) } > 0)
        Future(Left(Messages("error.E0654")))
      else {
        roleRepository.deleteRole(id) map { res =>
          if (res) cleanCache
          Right(res)
        }
      }
    }
  }

}
