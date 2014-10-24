package controllers

import actors._
import play.api.Play.current
import play.api._
import play.api.libs.json._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def socket = WebSocket.acceptWithActor[JsValue, JsValue] {
    request => out => PollSocketActor.props(out)
  }

}
