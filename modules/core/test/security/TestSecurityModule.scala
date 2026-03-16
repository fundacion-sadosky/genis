package security

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}
import types.Permission
import user.RoleRepository
import user.Role

/** Stub de UserRepository — siempre falla el bind (sin LDAP real) */
@Singleton
class StubUserRepository @Inject() ()(using ec: ExecutionContext) extends UserRepository {
  override def bind(userName: String, password: String): Future[Boolean] =
    Future.successful(false)

  override def get(userName: String): Future[LdapUser] =
    Future.failed(new UnsupportedOperationException("StubUserRepository: no LDAP in tests"))
}

/** Stub de RoleRepository — devuelve mapa vacío de permisos */
@Singleton
class StubRoleRepository @Inject() ()(using ec: ExecutionContext) extends RoleRepository {
  override def getRoles: Future[Seq[Role]] = Future.successful(Seq.empty)
  override def rolePermissionMap: Map[String, Set[Permission]] = Map.empty

  // Métodos agregados para compatibilidad
  override def addRole(role: Role): Future[Boolean] = Future.successful(false)
  override def updateRole(role: Role): Future[Boolean] = Future.successful(false)
  override def deleteRole(id: String): Future[Either[String, Boolean]] = Future.successful(Left("Not implemented"))
}
