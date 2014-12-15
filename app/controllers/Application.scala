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
  val masterSocketActor = Akka.system.actorOf(Props(new MasterSocketActor))

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def apiSocketPoll(id: String) = WebSocket.acceptWithActor[JsValue, JsValue] {
    request =>
      out => PollSocketActor.props(out, masterSocketActor, id)
  }

  def demo = WebSocket.acceptWithActor[JsValue, JsValue] {
    request =>
      out => PollSocketActor.props(out, masterSocketActor, "demo")
  }

  //refactor
  def poll(id: String) = Action.async {
    RedisPollRepository.get(id).map(
      _.fold(Ok(views.html.error("Poll not found")))((p: Poll) => Ok(views.html.poll(p)))
    )
  }
  def viewResults(id: String) = Action.async {
    RedisPollRepository.get(id).map(
      _.fold(Ok(views.html.error("Poll not found")))((p: Poll) => Ok(views.html.results(p)))
    )
  }
  //--

  def castVote = Action.async(parse.json) {
    req => {
      val maybeVote = req.body.validate[Vote]
      maybeVote fold (
        errors => {
          scala.concurrent.Future(BadRequest(
            Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
        },
        vote => {
          val voteCount = RedisPollRepository.incrOption(vote.pollId, vote.index)
          voteCount.map(o =>
            o.fold(Ok(Json.obj("status" -> "failed", "message" -> "Invalid selection")))(
              c => {
                //.get, but should always be there so not a huge deal
                masterSocketActor ! SocketVote(vote.pollId)
                Ok(Json.obj("status" -> "ok", "count" -> c))
              }
            )
          )
        }
      )
    }
  }

  def newPoll = Action.async(parse.json) {
    req => {
      val maybePoll = req.body.validate[PollCreation]
      maybePoll.fold(
        errors => {
          scala.concurrent.Future(BadRequest(
            Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
        },
        toCreate => {
          val poll = RedisPollRepository.create(toCreate)
          poll.map(p => Ok(Json.obj("id" -> p.id)))
        }
      )
    }
  }
}
