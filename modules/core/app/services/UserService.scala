package services


trait UserService {
  def findUserAssignable: scala.concurrent.Future[Seq[types.User]]

  // Métodos agregados para compatibilidad
  def signupRequest(solicitude: Any): scala.concurrent.Future[Either[String, types.User]]
  def clearPassRequest(solicitude: Any): scala.concurrent.Future[Either[String, types.User]]
  def signupConfirmation(confirmation: Any): scala.concurrent.Future[Either[String, Int]]
  def clearPassConfirmation(confirmation: Any): scala.concurrent.Future[Either[String, Int]]
  def listAllUsers(): scala.concurrent.Future[Seq[types.User]]
  def setStatus(userId: String, status: Any): scala.concurrent.Future[Either[String, Int]]
  def updateUser(user: Any): scala.concurrent.Future[Int]
}

import javax.inject.{Inject, Singleton}
import types.User
import security.AuthService
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserServiceImpl @Inject()(
  authService: AuthService
)(implicit ec: ExecutionContext) extends UserService {
  override def findUserAssignable: Future[Seq[User]] = Future.successful(Seq.empty)

  import play.api.libs.json.{JsValue}

  override def signupRequest(solicitude: Any): Future[Either[String, User]] = {
    val js = solicitude.asInstanceOf[JsValue]
    val userName = (js \ "userName").asOpt[String].getOrElse("")
    val password = (js \ "password").asOpt[String].getOrElse("")
    val otp = types.TotpToken((js \ "otp").asOpt[String].getOrElse(""))
    authService.authenticate(userName, password, otp).map {
      case Some(fullUser) =>
        val su = fullUser.userDetail
        // types.User: (firstName: String, lastName: String, id: Long)
        val tu = types.User(
          firstName = su.firstName,
          lastName = su.lastName,
          id = try { su.id.toLong } catch { case _: Throwable => 0L }
        )
        Right(tu)
      case None => Left("Authentication failed or user not found")
    }
  }

  override def clearPassRequest(solicitude: Any): Future[Either[String, User]] = {
    val js = solicitude.asInstanceOf[JsValue]
    val userName = (js \ "userName").asOpt[String].getOrElse("")
    val password = (js \ "password").asOpt[String].getOrElse("")
    val otp = types.TotpToken((js \ "otp").asOpt[String].getOrElse(""))
    authService.authenticate(userName, password, otp).map {
      case Some(fullUser) =>
        val su = fullUser.userDetail
        val tu = types.User(
          firstName = su.firstName,
          lastName = su.lastName,
          id = try { su.id.toLong } catch { case _: Throwable => 0L }
        )
        Right(tu)
      case None => Left("Authentication failed or user not found")
    }
  }

  override def signupConfirmation(confirmation: Any): Future[Either[String, Int]] = Future.successful(Left("Not implemented"))
  override def clearPassConfirmation(confirmation: Any): Future[Either[String, Int]] = Future.successful(Left("Not implemented"))
  override def listAllUsers(): Future[Seq[User]] = Future.successful(Seq.empty)
  override def setStatus(userId: String, status: Any): Future[Either[String, Int]] = Future.successful(Left("Not implemented"))
  override def updateUser(user: Any): Future[Int] = Future.successful(0)
}
