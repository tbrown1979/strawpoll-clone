package actors

import util._
import models._
import akka.actor._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.ws.WS
import play.api.libs.json._
import play.api.libs.json.{JsValue, Json}
import play.api.Logger

object PollEventSource {
  case class RegisterListener(pollId: String, listener: ActorRef)
  case class UnregisterListener(pollId: String, listener: ActorRef)
}

trait PollEventSource { this: Actor =>
  import PollEventSource._

  var listeners = scala.collection.mutable.Map[String, Vector[ActorRef]]()

  def sendEventTo[T](poll: String, event: T): Unit =
    listeners(poll) foreach { _ ! event }

  def eventSourceReceive: Receive = {
    case RegisterListener(pollId, listener) =>
      //need to double check this/test this
      listeners.update(pollId, listeners.getOrElseUpdate(pollId, Vector(listener)) :+ listener)
      println(listeners.get(pollId))
    case UnregisterListener(pollId, listener) =>
      //need to make sure to clear out the pollIDs that don't have any actorRefs in their vector
      listeners.update(pollId, listeners.getOrElseUpdate(pollId, Vector()) filter { _ != listener })
      println(listeners.get(pollId).map(_.length))
      listeners.get(pollId) match {
        case Some(Vector()) => {println("done listening $pollId"); listeners.remove(pollId)}
      }
  }
}

class MasterSocketActor extends Actor with PollEventSource {
  def masterReceive: Receive = {
    case Vote(pollId) =>
      sendEventTo(pollId, VoteCast)
  }

  def receive = eventSourceReceive orElse masterReceive
}
