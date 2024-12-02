package controllers

import javax.inject.Singleton

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

@Singleton
class Status extends Controller with JsonActions {

  def status = Action.async {
    Future.successful(Ok(Json.obj("status" -> "UP")))
  }

}
