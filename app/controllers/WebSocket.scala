package actors

import util._
import akka.actor._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.ws.WS
import play.api.libs.json._
import play.api.libs.json.{JsValue, Json}
import play.api.Logger
//import org.joda.time.DateTime

object PollSocketActor {
  def props(out: ActorRef, socketManager: ActorRef, pollId: String) =
    Props(new PollSocketActor(out, socketManager, pollId))
}

class PollSocketActor(out: ActorRef, socketManager: ActorRef, pollId: String) extends Actor {
  import PollEventSource._
  socketManager ! RegisterListener(pollId, self)

  def receive = {
    case msg: JsValue => out ! s"I received your message: $msg"
  }

  override def postStop() = {
    socketManager ! UnregisterListener(pollId, self)
  }
}
