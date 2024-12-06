package user

import java.util.UUID

import scala.Left
import scala.Right
import scala.concurrent.Future
import javax.inject.Inject
import javax.inject.Singleton

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import security.CryptoService
import security.OTPService
import services.{CacheService, ClearPassRequestKey, FullUserKey, SignupRequestKey}
import inbox.{NotificationInfo, NotificationService, UserPendingInfo}
import types.Permission
import play.api.i18n.Messages


abstract class UserService {
  def signupRequest(solicitude: SignupSolicitude): Future[Either[String, SignupResponse]]
  def clearPassRequest(solicitude: ClearPassSolicitud): Future[Either[String, ClearPassResponse]]
  def signupConfirmation(confirmation: SignupChallenge): Future[Either[String, Int]]
  def clearPassConfirmation(confirmation: ClearPassChallenge): Future[Either[String, Int]]
  def listAllUsers(): Future[Seq[UserView]]
  def updateUser(user: UserView): Future[Boolean]
  def setStatus(userId: String, newStatus: UserStatus.Value): Future[Either[String, Int]]
  def findUserAssignable: Future[Seq[User]]
  def getUser(userId: String): Future[UserView]
  def getUserOrEmpty(userId: String): Future[Option[UserView]]
  def findByStatus(status: UserStatus.Value): Future[Seq[UserView]]
  def findByGeneMapper(geneMapper: String): Future[Option[UserView]]
  def findUserAssignableByRole(roleId: String): Future[Seq[User]]
  def findUsersIdWithPermission(permission:Permission):Future[Seq[String]]
  def findUsersIdWithPermissions(permission:Seq[Permission]):Future[Seq[String]]
  def isSuperUser(userId: String) : Future[Boolean]
  def isSuperUserByGeneMapper(geneMapperId: String) : Future[Boolean]
  def findSuperUsers():Future[Seq[String]]
  def sendNotifToAllSuperUsers(info: NotificationInfo, excepThis: Seq[String])
}

@Singleton
class UserServiceImpl @Inject() (
    userRepository: UserRepository,
    cacheService: CacheService,
    cryptoService: CryptoService,
    otpService: OTPService,
    notiService: NotificationService,
    roleService: RoleService) extends UserService {

  def calculateUserNameCandidates(firstName: String, lastName: String, email: String): Future[Seq[String]] = {
    val normFirstName = firstName.replace("""\W""", "").toLowerCase()
    val normLastName = lastName.replace("""\W""", "").toLowerCase()
    val firstChar = normFirstName.charAt(0)

    val candidates = (firstChar + normLastName) ::
      (firstChar + "." + normLastName) ::
      (normLastName + firstChar) ::
      (normLastName + "." + firstChar) ::
      (email.takeWhile { _ == '@' }) ::
      (firstChar + "_" + normLastName) ::
      (normLastName + "_" + firstChar) ::
      (normFirstName + "." + normLastName) ::
      (normFirstName + normLastName) ::
      (normFirstName + "_" + normLastName) ::
      (normLastName + "." + normFirstName) ::
      (normLastName + normFirstName) ::
      (normLastName + "_" + normFirstName) ::
      Nil

    userRepository.finfByUid(candidates) map { currentUsers =>
      candidates.diff(currentUsers map { _.userName }).take(3)
    }
  }
  override def clearPassRequest(solicitud: ClearPassSolicitud): Future[Either[String, ClearPassResponse]] = {
    val totpsecret = cryptoService.giveTotpSecret
    val (publicKey, privateKey) = cryptoService.giveRsaKeys
    val credentials = UserCredentials(
      publicKey,
      privateKey,
      totpsecret)

    userRepository.get(solicitud.userName).flatMap((ldapUser) => {

      ldapUser.status match {
        case UserStatus.pending_reset => {
          val key = ClearPassRequestKey(UUID.randomUUID().toString())
          cacheService.set(key, (solicitud, credentials,solicitud.userName))
          val response = ClearPassResponse(key.token, totpsecret)
           Future.successful(Right(response))
        }
        case _ => {
          Future.successful(Left(Messages("error.E0641")))
        }
      }

    }).recoverWith{
      case _:Exception => Future.successful(Left(Messages("error.E0641")))
    }

  }

  override def signupRequest(solicitude: SignupSolicitude): Future[Either[String, SignupResponse]] = {

    val totpsecret = cryptoService.giveTotpSecret
    val (publicKey, privateKey) = cryptoService.giveRsaKeys

    val credentials = UserCredentials(
      publicKey,
      privateKey,
      totpsecret)

    calculateUserNameCandidates(solicitude.firstName, solicitude.lastName, solicitude.email) map { userNames =>

      val key = SignupRequestKey(UUID.randomUUID().toString())
      cacheService.set(key, (solicitude, credentials, userNames))
      val response = SignupResponse(key.token, totpsecret, userNames)
      Right(response)

    }
  }

  override def signupConfirmation(confirmation: SignupChallenge): Future[Either[String, Int]] = {
    val key = SignupRequestKey(confirmation.signupRequestId)
    cacheService.pop(key).fold[Future[Either[String, Int]]] {
      Future.successful(Left(Messages("error.E0802")))
    } {
      case (signupRequest, signupCredentials, userNameCandidates) =>
        if (otpService.validate(confirmation.challengeResponse, signupCredentials.totpSecret)) {
          val derivatedKey = cryptoService.generateDerivatedCredentials(signupRequest.password)
          val user = LdapUser(
            userNameCandidates(confirmation.choosenUserName),
            signupRequest.firstName,
            signupRequest.lastName,
            signupRequest.email,
            signupRequest.roles,
            signupRequest.geneMapperId,
            signupRequest.phone1,
            signupRequest.phone2,
            UserStatus.pending,
            cryptoService.encrypt(signupCredentials.publicKey, derivatedKey),
            cryptoService.encrypt(signupCredentials.privateKey, derivatedKey),
            cryptoService.encrypt(signupCredentials.totpSecret.getBytes, derivatedKey),
            signupRequest.superuser)

          val created = userRepository.create(user, signupRequest.password)
          created.onSuccess {
            case (userCreatedId) =>
              roleService.getRolePermissions().filter {
                case (_, permissions) =>
                  permissions.contains(Permission.USER_CRUD)
              }.map {
                case (role, _) =>
                  this.findUserAssignableByRole(role).foreach {
                    admins =>
                      admins.foreach { admin =>
                        notiService.push(admin.id, UserPendingInfo(userCreatedId))
                      }
                      sendNotifToAllSuperUsers(UserPendingInfo(userCreatedId), admins.map(_.id))
                  }
              }

          }
          created.map { _ => Right(0) }
        } else {
          Future.successful(Left(Messages("error.E0803")))
        }
    }
  }
  override def clearPassConfirmation(confirmation: ClearPassChallenge): Future[Either[String, Int]] = {
    val key = ClearPassRequestKey(confirmation.clearPassRequestId)
    cacheService.pop(key).fold[Future[Either[String, Int]]] {
      Future.successful(Left(Messages("error.E0802")))
    } {
      case (signupRequest, signupCredentials, userName) =>
        if (otpService.validate(confirmation.challengeResponse, signupCredentials.totpSecret)) {
          val derivatedKey = cryptoService.generateDerivatedCredentials(signupRequest.newPassword)

          val created = userRepository.clearPass(signupRequest.userName,UserStatus.pending,
            signupRequest.newPassword,
            cryptoService.encrypt(signupCredentials.totpSecret.getBytes, derivatedKey))

          created.onSuccess {
            case (userCreatedId) =>
              roleService.getRolePermissions().filter {
                case (_, permissions) =>
                  permissions.contains(Permission.USER_CRUD)
              }.map {
                case (role, _) =>
                  this.findUserAssignableByRole(role).foreach {
                    admins =>
                      admins.foreach { admin =>
                        notiService.push(admin.id, UserPendingInfo(userCreatedId))
                      }
                      sendNotifToAllSuperUsers(UserPendingInfo(userCreatedId), admins.map(_.id))
                  }
              }

          }
          created.map { _ => Right(0) }
          Future.successful(Right(0))
        } else {
          Future.successful(Left(Messages("error.E0803")))
        }
    }
  }
  override def listAllUsers(): Future[Seq[UserView]] = {
    userRepository.listAllUsers.map(_.map(LdapUser.toUserView))
  }

  override def updateUser(user: UserView): Future[Boolean] = {
    val res = userRepository.updateUser(LdapUser.fromUserView(user))
    res.onSuccess { case _ => cacheService.pop(FullUserKey(user.userName, 0)) }
    res
  }

  private val allowTransition = (a: UserStatus.Value, b: UserStatus.Value) => (a, b) match {
    case (UserStatus.pending, _) => true
    case (UserStatus.pending_reset, UserStatus.pending) => true
    case (_, UserStatus.pending) => false
    case (_, _)                  => true
  }

  override def setStatus(userId: String, newStatus: UserStatus.Value): Future[Either[String, Int]] = {
    userRepository.get(userId).flatMap { user =>
      val status = user.status

      if (allowTransition(status, newStatus)) {
        userRepository
          .setStatus(userId, newStatus)
          .map { _ =>
            if (status == UserStatus.pending) {
              roleService.getRolePermissions().filter {
                case (_, permissions) =>
                  permissions.contains(Permission.USER_CRUD)
              }.map {
                case (role, _) =>
                  this.findUserAssignableByRole(role).onSuccess {
                    case admins =>
                      admins.foreach { admin =>
                        notiService.solve(admin.id, UserPendingInfo(userId))
                      }
                  }
              }
            }
            cacheService.pop(FullUserKey(userId, 0))
            Right(0)
          }.recover { case t: Throwable => Left(t.getMessage) }
      } else {
        Future.successful(Left(Messages("error.E0646", status ,newStatus)))
      }
    }
  }

  override def findUserAssignable: Future[Seq[User]] = {

    val result = roleService.getRolePermissions().filter {
      case (role, permissions) =>
        permissions.contains(Permission.DNA_PROFILE_CRUD) && permissions.contains(Permission.MATCHES_MANAGER)
    }.map { case (role, _) => this.findUserAssignableByRole(role) }.toList

    Future.sequence(result).map { _.flatten }
  }

  override def findUserAssignableByRole(roleId: String): Future[Seq[User]] = {
    userRepository.findByRole(roleId).map { users => users.map(LdapUser.toUser(_, roleService.getRolePermissions())).filter { _.isAsignable } }
  }

  override def getUser(userId: String): Future[UserView] = {
    userRepository.get(userId).map { LdapUser.toUserView }
  }

  override def getUserOrEmpty(userId: String): Future[Option[UserView]] = {
    userRepository.getOrEmpty(userId).map {
      case Some(user) => Some(LdapUser.toUserView(user))
      case None => None
    }
  }

  override def findByStatus(status: UserStatus.Value): Future[Seq[UserView]] = {
    userRepository.findByStatus(status).map { _.map { LdapUser.toUserView } }
  }

  override def findByGeneMapper(geneMapper: String): Future[Option[UserView]] = {
    userRepository.findByGeneMapper(geneMapper).map {
      case Some(user) => Some(LdapUser.toUserView(user))
      case None => None}
  }
  override def findUsersIdWithPermission(permission:Permission):Future[Seq[String]] = {
    Future.sequence(roleService.getRolePermissions().filter {
      case (_, permissions) =>
        permissions.contains(permission)
    }.map {
      case (role, _) =>
        this.findUserAssignableByRole(role).map(list => {
          list.map(_.id)
        })
    })
      .map(res => {
        res.flatten.toSet.toSeq
      })
  }
  
  override def findUsersIdWithPermissions(
    permissions:Seq[Permission]
  ):Future[Seq[String]] = {
    Future
      .sequence(
        roleService
          .getRolePermissions()
          .filter {
            case (_, rolePermissions) =>
              rolePermissions
                .intersect(permissions.toSet)
                .size
                .equals(permissions.size)
          }.map {
            case (role, _) =>
              this
                .findUserAssignableByRole(role)
                .map(list => { list.map(_.id) })
          }
      )
      .map(
        res => { res.flatten.toSet.toSeq }
      )
  }

  override def isSuperUser(userId: String): Future[Boolean] = {
    getUser(userId).map(user => user.superuser)
  }

  override def isSuperUserByGeneMapper(geneMapperId: String): Future[Boolean] = {
    findByGeneMapper(geneMapperId).map(userOption => userOption.get.superuser)
  }

  override def findSuperUsers(): Future[Seq[String]] = {
    userRepository.findSuperUsers().map(_.map(LdapUser.toUser(_, roleService.getRolePermissions())).map(_.id)).map(_.toSet.toSeq)
  }

  override def sendNotifToAllSuperUsers(info: NotificationInfo, excepThis: Seq[String]) = {
    val usersToNotify = this.findSuperUsers().map(_.filter(user => !excepThis.contains(user)))
    usersToNotify.map(list => list.foreach { userId: String => notiService.push(userId, info) })
  }
}