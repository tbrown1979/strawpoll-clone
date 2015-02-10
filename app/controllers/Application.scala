package controllers

import actors._
import akka.actor.ActorSystem
import akka.actor._
import java.util.concurrent.TimeUnit;
import models._
import persistence._
import play.api.Logger
import play.api.Play.current
import play.api._
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Success, Failure}
import util._

object Application extends Controller {
  val pollRepo: PollRepository = new RedisPollRepository{}
  val masterSocketActor = Akka.system.actorOf(Props(new MasterSocketActor(pollRepo)))

  val demoPoll = PollCreation("Demo", Seq("Option1", "Option2", "Option3"))
  pollRepo.createCustomPoll(demoPoll, Some("demo")).onComplete {
    case Success(p) => {
      Akka.system.scheduler.schedule(0 milliseconds, 200 milliseconds){
        val randInt = scala.util.Random.nextInt(3)
        votePoll("demo", randInt)
      }
      Akka.system.scheduler.schedule(0 milliseconds, 120000 milliseconds) {
        pollRepo.resetPoll("demo")
        val randInt = scala.util.Random.nextInt(3)
        votePoll("demo", randInt)
      }
    }
    case Failure(e) => Logger.info("Error encountered with Demo creation: " + e.toString)
  }

  def index = Action {
    Ok(views.html.index())
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
    pollRepo.get(id).map(
      _.fold(Ok(views.html.error("Poll not found")))((p: Poll) => Ok(views.html.poll(p)))
    )
  }

  def getPoll(id: String) = Action.async {
    val errorMsg = Ok(Json.obj("status" -> "KO", "message" -> "Poll not found"))
    pollRepo.get(id).map(_.fold(errorMsg)
      (p => Ok(Json.toJson(p)))
    )
  }

  def viewResults(id: String) = Action.async {
    pollRepo.get(id).map(
      _.fold(Ok(views.html.error("Poll not found")))((p: Poll) => Ok(views.html.results(p)))
    )
  }
  //--

  def votePoll(pollId: String, index: Int): Future[Option[Long]] = {
    val voteCount = pollRepo.incrOption(pollId, index)
    voteCount.foreach(_ => masterSocketActor ! SocketVote(pollId))
    voteCount
  }

  def castVote = Action.async(parse.json) {
    req => {
      val maybeVote = req.body.validate[Vote]
      maybeVote fold (
        errors => {
          scala.concurrent.Future(BadRequest(
            Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
        },
        vote => {
          val voteCount = votePoll(vote.pollId, vote.index)
          voteCount.map(o =>
            o.fold(Ok(Json.obj("status" -> "failed", "message" -> "Invalid selection")))(
              c => {
                //.get, but should always be there so not a huge deal
                Ok(Json.obj("status" -> "ok", "count" -> c))
              }
            )
          )
        }
      )
    }
  }

  def newPoll = Action.async(parse.json) {
    import PollValidation._
    req => {
      val maybePoll = req.body.validate[PollCreation]
      maybePoll.fold(
        errors => {
          Future.successful(BadRequest(
            Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
        },
        toCreate => {
          val poll: Future[JsValue] =
            if (!isValid(toCreate)) {
              Future.successful(pollInvalidJson)
            } else {
              pollRepo.create(toCreate).map(p => Json.obj("id" -> p.id))
            }
          poll.map(Ok(_))
        }
      )
    }
  }
}

object PollValidation {
  def isValid(poll: PollCreation): Boolean = {
    val vTitle = poll.title.length > 1
    val vOptions = poll.options.length > 1
    return vTitle && vOptions
  }

  val pollInvalidJson: JsValue = {
    Json.obj("status" -> "error", "message" -> "Must contain title and at least two options")
  }

}
