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

  override def getOrEmpty(userName: String): Future[Option[LdapUser]] =
    Future.successful(None)

  override def create(user: LdapUser, password: String): Future[String] =
    Future.successful(user.userName)

  override def updateUser(user: LdapUser): Future[Boolean] =
    Future.successful(true)

  override def setStatus(userId: String, newStatus: user.UserStatus): Future[Unit] =
    Future.successful(())

  override def clearPass(userId: String, newStatus: user.UserStatus, password: String, encryptrdTotpSecret: Array[Byte]): Future[String] =
    Future.successful(userId)

  override def listAllUsers(): Future[Seq[LdapUser]] =
    Future.successful(Seq.empty)

  override def finfByUid(uids: Seq[String]): Future[Seq[LdapUser]] =
    Future.successful(Seq.empty)

  override def findByRole(roleId: String): Future[Seq[LdapUser]] =
    Future.successful(Seq.empty)

  override def findByStatus(status: user.UserStatus): Future[Seq[LdapUser]] =
    Future.successful(Seq.empty)

  override def findByGeneMapper(geneMapper: String): Future[Option[LdapUser]] =
    Future.successful(None)

  override def findSuperUsers(): Future[Seq[LdapUser]] =
    Future.successful(Seq.empty)

  override def blockingGet(userId: String, password: String): Either[String, LdapUser] =
    Left("StubUserRepository: no LDAP in tests")

  override def setTOTP(userId: String, totpenc: Array[Byte]): Future[Boolean] =
    Future.successful(true)
}

/** Stub de RoleRepository — devuelve mapa vacío de permisos */
@Singleton
class StubRoleRepository @Inject() ()(using ec: ExecutionContext) extends RoleRepository {
  override def getRoles: Future[Seq[Role]] = Future.successful(Seq.empty)
  override def rolePermissionMap: Map[String, Set[Permission]] = Map.empty
  override def addRole(role: Role): Future[Boolean] = Future.successful(false)
  override def updateRole(role: Role): Future[Boolean] = Future.successful(false)
  override def deleteRole(id: String): Future[Boolean] = Future.successful(false)
}
