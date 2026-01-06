package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import repositories.UserRepository
import scala.concurrent.ExecutionContext

@Singleton
class UserController @Inject()(val controllerComponents: ControllerComponents, userRepository: UserRepository)(implicit ec: ExecutionContext) extends BaseController {

  def list = Action.async { implicit request: Request[AnyContent] =>
    userRepository.list().map { users =>
      Ok(Json.toJson(users))
    }
  }
}
