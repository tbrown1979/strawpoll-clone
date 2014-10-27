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
//import org.joda.time.DateTime

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
      listeners(pollId) = listeners(pollId) :+ listener
    case UnregisterListener(pollId, listener) =>
      listeners(pollId) = listeners(pollId) filter { _ != listener }
  }
}

class MasterSocketActor extends Actor with PollEventSource {

  def receive = eventSourceReceive
}
