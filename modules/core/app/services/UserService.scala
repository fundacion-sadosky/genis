package services


trait UserService {
  def findUserAssignable: scala.concurrent.Future[Seq[types.User]]
}

import javax.inject.Singleton
import types.User

@Singleton
class UserServiceImpl extends UserService {
  override def findUserAssignable: scala.concurrent.Future[Seq[User]] = scala.concurrent.Future.successful(Seq.empty)
}
