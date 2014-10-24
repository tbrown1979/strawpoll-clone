package strawpoll

import util._
import akka.actor._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.ws.WS
import play.api.libs.json.{JsValue, Json}
import play.api.Logger
//import org.joda.time.DateTime

object PollSocketActor {
  def props(out: ActorRef) = Props(new PollSocketActor(out))
}

class PollSocketActor(out: ActorRef) extends Actor {
  def receive = {
    case msg: String => out ! s"I received your message: $msg"
  }
}
