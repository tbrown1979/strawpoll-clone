package actors

import util._
import models._
import persistence._
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

  def sendEventTo[T](pollId: String, event: T): Unit = {
    val poll = listeners.get(pollId)
    poll.foreach(_ foreach { _ ! event })
  }

  def eventSourceReceive: Receive = {
    case RegisterListener(pollId, listener) =>
      listeners.update(pollId, listeners.getOrElse(pollId, Vector()) :+ listener)
    case UnregisterListener(pollId, listener) =>
      listeners.update(pollId, listeners.getOrElseUpdate(pollId, Vector()) filter { _ != listener })
      if (listeners.isDefinedAt(pollId)) {
        if (listeners(pollId).isEmpty) listeners.remove(pollId)
      }
  }
}

class MasterSocketActor extends Actor with PollEventSource {
  def masterReceive: Receive = {
    case SocketVote(pollId) =>
      RedisPollRepository.get(pollId).map(_.foreach(poll =>
        sendEventTo(pollId, Votes(poll.tallies))))
  }

  def receive = eventSourceReceive orElse masterReceive
}
