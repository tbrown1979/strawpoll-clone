package controllers

import actors._
import akka.actor._
import play.api.Play.current
import play.api._
import play.api.libs.json._
import play.api.mvc._

object Application extends Controller {
  val redisRepo = new RedisPollRepository{}

  val masterSocketActor = Props(new MasterSocketActor)

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def socket = WebSocket.acceptWithActor[JsValue, JsValue] {
    request => out => PollSocketActor.props(out)
  }

  def newPoll = Action(parse.json) {
    req => redisRepo.createPoll()
  }

}
