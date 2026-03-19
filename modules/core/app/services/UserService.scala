package services


import scala.concurrent.Future

trait UserService {
  def findUserAssignable: Future[Seq[types.User]]
  def isSuperUser(userId: String): Future[Boolean]
}

import javax.inject.Singleton
import types.User

@Singleton
class UserServiceImpl extends UserService {
  override def findUserAssignable: Future[Seq[User]] = Future.successful(Seq.empty)
  override def isSuperUser(userId: String): Future[Boolean] = Future.successful(false)
}
