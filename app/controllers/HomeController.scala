package controllers

import play.api.mvc._
import javax.inject._

/**
 * Controlador Home - Sirve las páginas estáticas
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def login(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.login())
  }
  
  def dashboard(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.dashboard())
  }

  def inboxContent(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.inbox()).as("text/html")
  }
}
