package controllers

import actors._
import akka.actor._
import models._
import persistence._
import play.api.Play.current
import play.api._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc._
import akka.actor.ActorSystem
import play.api.libs.concurrent.Akka
import scala.util.{Success, Failure}
import play.api.Logger
import util._


object Application extends Controller {

  val redisRepo = new RedisPollRepository{}

  val masterSocketActor = Akka.system.actorOf(Props(new MasterSocketActor))

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def apiSocketPoll(id: String) = WebSocket.acceptWithActor[JsValue, JsValue] {
    request =>
      out => PollSocketActor.props(out, masterSocketActor, id)
  }

  def poll(id: String) = Action.async {
    redisRepo.get(id).map(p => Ok(views.html.poll(p)))
  }

  def newPoll = Action.async(parse.json) {
    req => {
      try {
        val maybePoll = req.body.as[Poll]
        val error = Ok(Json.obj("error" -> "Encountered error"))
        val resp  = (id: String) => Ok(Json.obj("id" -> id))
        maybePoll.map(p => p.idresp())
      } catch {
        case e: JsResultException =>
          Logger.info(e.toString)
          scala.concurrent.Future(BadRequest("Invalid json!"))
        case e: Exception =>
          Logger.info("Something happened!" + e.toString)
          scala.concurrent.Future(BadRequest(""))
      }
    }
  }
}
