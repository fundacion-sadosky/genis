package services

import scala.concurrent.Future

case class UserView(
  userName: String,
  firstName: String,
  lastName: String,
  email: String,
  roles: Seq[String],
  status: user.UserStatus,
  geneMapperId: String,
  phone1: String,
  phone2: Option[String] = None,
  superuser: Boolean = false
)

object UserView {
  import play.api.libs.json.*
  implicit val format: Format[UserView] = Json.format[UserView]
}

trait UserService {
  def findUserAssignable: Future[Seq[types.User]]
  def isSuperUser(userId: String): Future[Boolean]
  def getUserOrEmpty(userId: String): Future[Option[UserView]]
}

import javax.inject.Singleton
import types.User

@Singleton
class UserServiceImpl extends UserService {
  override def findUserAssignable: Future[Seq[User]] = Future.successful(Seq.empty)
  override def isSuperUser(userId: String): Future[Boolean] = Future.successful(false)
  override def getUserOrEmpty(userId: String): Future[Option[UserView]] = Future.successful(None)
}
