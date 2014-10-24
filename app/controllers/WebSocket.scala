package strawpoll

import util._
import akka.actor._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.ws.WS
import play.api.libs.json.{JsValue, Json}
import play.api.Logger
import org.joda.time.DateTime

object StrawPoll extends PollFutureProvider {



}
