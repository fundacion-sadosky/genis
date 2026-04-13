package services

import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.i18n.{Lang, MessagesApi}

import inbox.{NotificationInfo, NotificationService, UserPendingInfo}
import security.{CryptoService, LdapUser, OTPService, RoleService, UserCredentials, UserRepository}
import types.Permission
import user.{ClearPassChallenge, ClearPassResponse, ClearPassSolicitud,
  SignupChallenge, SignupResponse, SignupSolicitude, UserStatus, UserView}

trait UserService:
  def signupRequest(solicitude: SignupSolicitude): Future[Either[String, SignupResponse]]
  def clearPassRequest(solicitude: ClearPassSolicitud): Future[Either[String, ClearPassResponse]]
  def signupConfirmation(confirmation: SignupChallenge): Future[Either[String, Int]]
  def clearPassConfirmation(confirmation: ClearPassChallenge): Future[Either[String, Int]]
  def listAllUsers(): Future[Seq[UserView]]
  def updateUser(user: UserView): Future[Boolean]
  def setStatus(userId: String, newStatus: UserStatus): Future[Either[String, Int]]
  def findUserAssignable: Future[Seq[security.User]]
  def getUser(userId: String): Future[UserView]
  def getUserOrEmpty(userId: String): Future[Option[UserView]]
  def findByStatus(status: UserStatus): Future[Seq[UserView]]
  def findByGeneMapper(geneMapper: String): Future[Option[UserView]]
  def findUserAssignableByRole(roleId: String): Future[Seq[security.User]]
  def findUsersIdWithPermission(permission: Permission): Future[Seq[String]]
  def findUsersIdWithPermissions(permissions: Seq[Permission]): Future[Seq[String]]
  def isSuperUser(userId: String): Future[Boolean]
  def isSuperUserByGeneMapper(geneMapperId: String): Future[Boolean]
  def findSuperUsers(): Future[Seq[String]]
  def sendNotifToAllSuperUsers(info: NotificationInfo, excepThis: Seq[String]): Unit

@Singleton
class UserServiceImpl @Inject() (
    userRepository: UserRepository,
    cacheService: CacheService,
    cryptoService: CryptoService,
    otpService: OTPService,
    notiService: NotificationService,
    roleService: RoleService,
    messagesApi: MessagesApi
)(using ec: ExecutionContext) extends UserService:

  private val logger = Logger(this.getClass)

  private given Lang = Lang.defaultLang

  private def msg(key: String, args: Any*): String =
    messagesApi(key, args*)(using summon[Lang])

  private def calculateUserNameCandidates(firstName: String, lastName: String, email: String): Future[Seq[String]] =
    val normFirstName = firstName.replaceAll("""\W""", "").toLowerCase()
    val normLastName = lastName.replaceAll("""\W""", "").toLowerCase()
    val firstChar = normFirstName.charAt(0)

    val candidates = List(
      s"$firstChar$normLastName",
      s"$firstChar.$normLastName",
      s"$normLastName$firstChar",
      s"$normLastName.$firstChar",
      email.takeWhile(_ != '@'),
      s"${firstChar}_$normLastName",
      s"${normLastName}_$firstChar",
      s"$normFirstName.$normLastName",
      s"$normFirstName$normLastName",
      s"${normFirstName}_$normLastName",
      s"$normLastName.$normFirstName",
      s"$normLastName$normFirstName",
      s"${normLastName}_$normFirstName"
    )

    userRepository.finfByUid(candidates).map { currentUsers =>
      candidates.diff(currentUsers.map(_.userName)).take(3)
    }

  override def signupRequest(solicitude: SignupSolicitude): Future[Either[String, SignupResponse]] =
    val totpsecret = cryptoService.giveTotpSecret
    val (publicKey, privateKey) = cryptoService.giveRsaKeys
    val credentials = UserCredentials(publicKey, privateKey, totpsecret)

    calculateUserNameCandidates(solicitude.firstName, solicitude.lastName, solicitude.email).map { userNames =>
      val key = SignupRequestKey(UUID.randomUUID().toString)
      cacheService.set(key, (solicitude, credentials, userNames))
      val response = SignupResponse(key.token, totpsecret, userNames)
      Right(response)
    }

  override def clearPassRequest(solicitud: ClearPassSolicitud): Future[Either[String, ClearPassResponse]] =
    val totpsecret = cryptoService.giveTotpSecret
    val (publicKey, privateKey) = cryptoService.giveRsaKeys
    val credentials = UserCredentials(publicKey, privateKey, totpsecret)

    userRepository.get(solicitud.userName).flatMap { ldapUser =>
      ldapUser.status match
        case UserStatus.pending_reset =>
          val key = ClearPassRequestKey(UUID.randomUUID().toString)
          cacheService.set(key, (solicitud, credentials, solicitud.userName))
          val response = ClearPassResponse(key.token, totpsecret)
          Future.successful(Right(response))
        case _ =>
          Future.successful(Left(msg("error.E0641")))
    }.recoverWith {
      case _: Exception => Future.successful(Left(msg("error.E0641")))
    }

  override def signupConfirmation(confirmation: SignupChallenge): Future[Either[String, Int]] =
    val key = SignupRequestKey(confirmation.signupRequestId)
    cacheService.pop(key)(using summon[ClassTag[(SignupSolicitude, UserCredentials, Seq[String])]]).fold[Future[Either[String, Int]]] {
      Future.successful(Left(msg("error.E0802")))
    } { case (signupRequest, signupCredentials, userNameCandidates) =>
      if otpService.validate(confirmation.challengeResponse, signupCredentials.totpSecret) then
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
          signupRequest.superuser
        )

        val created = userRepository.create(user, signupRequest.password)
        created.foreach { userCreatedId =>
          roleService.getRolePermissions().filter {
            case (_, permissions) => permissions.contains(Permission.USER_CRUD)
          }.map { case (role, _) =>
            findUserAssignableByRole(role).foreach { admins =>
              admins.foreach(admin => notiService.push(admin.id, UserPendingInfo(userCreatedId)))
              sendNotifToAllSuperUsers(UserPendingInfo(userCreatedId), admins.map(_.id))
            }
          }
        }
        created.map(_ => Right(0))
      else
        Future.successful(Left(msg("error.E0803")))
    }

  override def clearPassConfirmation(confirmation: ClearPassChallenge): Future[Either[String, Int]] =
    val key = ClearPassRequestKey(confirmation.clearPassRequestId)
    cacheService.pop(key)(using summon[ClassTag[(ClearPassSolicitud, UserCredentials, String)]]).fold[Future[Either[String, Int]]] {
      Future.successful(Left(msg("error.E0802")))
    } { case (clearPassReq, signupCredentials, userName) =>
      if otpService.validate(confirmation.challengeResponse, signupCredentials.totpSecret) then
        val derivatedKey = cryptoService.generateDerivatedCredentials(clearPassReq.newPassword)

        val created = userRepository.clearPass(
          clearPassReq.userName,
          UserStatus.pending,
          clearPassReq.newPassword,
          cryptoService.encrypt(signupCredentials.totpSecret.getBytes, derivatedKey)
        )

        created.foreach { userCreatedId =>
          roleService.getRolePermissions().filter {
            case (_, permissions) => permissions.contains(Permission.USER_CRUD)
          }.map { case (role, _) =>
            findUserAssignableByRole(role).foreach { admins =>
              admins.foreach(admin => notiService.push(admin.id, UserPendingInfo(userCreatedId)))
              sendNotifToAllSuperUsers(UserPendingInfo(userCreatedId), admins.map(_.id))
            }
          }
        }
        created.map(_ => Right(0))
      else
        Future.successful(Left(msg("error.E0803")))
    }

  override def listAllUsers(): Future[Seq[UserView]] =
    userRepository.listAllUsers().map(_.map(LdapUser.toUserView))

  override def updateUser(user: UserView): Future[Boolean] =
    val res = userRepository.updateUser(LdapUser.fromUserView(user))
    res.foreach(_ => cacheService.pop(FullUserKey(user.userName, 0))(using summon[ClassTag[security.FullUser]]))
    res

  private val allowTransition = (a: UserStatus, b: UserStatus) => (a, b) match
    case (UserStatus.pending, _) => true
    case (UserStatus.pending_reset, UserStatus.pending) => true
    case (_, UserStatus.pending) => false
    case (_, _) => true

  override def setStatus(userId: String, newStatus: UserStatus): Future[Either[String, Int]] =
    userRepository.get(userId).flatMap { user =>
      val status = user.status
      if allowTransition(status, newStatus) then
        userRepository.setStatus(userId, newStatus).map { _ =>
          if status == UserStatus.pending then
            roleService.getRolePermissions().filter {
              case (_, permissions) => permissions.contains(Permission.USER_CRUD)
            }.map { case (role, _) =>
              findUserAssignableByRole(role).foreach { admins =>
                admins.foreach(admin => notiService.solve(admin.id, UserPendingInfo(userId)))
              }
            }
          cacheService.pop(FullUserKey(userId, 0))(using summon[ClassTag[security.FullUser]])
          Right(0)
        }.recover { case t: Throwable =>
            logger.error("Error al cambiar estado del usuario", t)
            Left("Error al cambiar estado del usuario")
          }
      else
        Future.successful(Left(msg("error.E0646", status, newStatus)))
    }

  override def findUserAssignable: Future[Seq[security.User]] =
    val result = roleService.getRolePermissions().filter {
      case (_, permissions) =>
        permissions.contains(Permission.DNA_PROFILE_CRUD) && permissions.contains(Permission.MATCHES_MANAGER)
    }.map { case (role, _) => findUserAssignableByRole(role) }.toList

    Future.sequence(result).map(_.flatten)

  override def findUserAssignableByRole(roleId: String): Future[Seq[security.User]] =
    userRepository.findByRole(roleId).map { users =>
      users.map(LdapUser.toUser(_, roleService.getRolePermissions())).filter(_.isAsignable)
    }

  override def getUser(userId: String): Future[UserView] =
    userRepository.get(userId).map(LdapUser.toUserView)

  override def getUserOrEmpty(userId: String): Future[Option[UserView]] =
    userRepository.getOrEmpty(userId).map(_.map(LdapUser.toUserView))

  override def findByStatus(status: UserStatus): Future[Seq[UserView]] =
    userRepository.findByStatus(status).map(_.map(LdapUser.toUserView))

  override def findByGeneMapper(geneMapper: String): Future[Option[UserView]] =
    userRepository.findByGeneMapper(geneMapper).map(_.map(LdapUser.toUserView))

  override def findUsersIdWithPermission(permission: Permission): Future[Seq[String]] =
    Future.sequence(
      roleService.getRolePermissions().filter {
        case (_, permissions) => permissions.contains(permission)
      }.map { case (role, _) =>
        findUserAssignableByRole(role).map(_.map(_.id))
      }
    ).map(_.flatten.toSet.toSeq)

  override def findUsersIdWithPermissions(permissions: Seq[Permission]): Future[Seq[String]] =
    Future.sequence(
      roleService.getRolePermissions().filter {
        case (_, rolePermissions) =>
          rolePermissions.intersect(permissions.toSet).size == permissions.size
      }.map { case (role, _) =>
        findUserAssignableByRole(role).map(_.map(_.id))
      }
    ).map(_.flatten.toSet.toSeq)

  override def isSuperUser(userId: String): Future[Boolean] =
    getUser(userId).map(_.superuser)

  override def isSuperUserByGeneMapper(geneMapperId: String): Future[Boolean] =
    findByGeneMapper(geneMapperId).map(_.exists(_.superuser))

  override def findSuperUsers(): Future[Seq[String]] =
    userRepository.findSuperUsers().map(
      _.map(LdapUser.toUser(_, roleService.getRolePermissions())).map(_.id).toSet.toSeq
    )

  override def sendNotifToAllSuperUsers(info: NotificationInfo, excepThis: Seq[String]): Unit =
    val usersToNotify = findSuperUsers().map(_.filter(user => !excepThis.contains(user)))
    usersToNotify.foreach(_.foreach(userId => notiService.push(userId, info)))
