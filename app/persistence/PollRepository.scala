package persistence

import util._
import actors._
import akka.actor.ActorSystem
import akka.actor._
import models._
import play.api.Play.current
import play.api._
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import play.api.mvc._
import scala.util.{Success, Failure}
import scredis._
import scredis.serialization._
import scredis.serialization.Implicits._

trait PollRepository extends FutureProvider {
  def create(poll: PollCreation): Future[Poll]
}

trait RedisPollRepository extends PollRepository with PollFutureProvider {
  val redis = Redis()
  import redis.dispatcher

  def create(newPoll: PollCreation): Future[Poll] = {
    val pollId = redis.incr("pollId")

    for {
      id <- pollId
      name = s"poll:$id"
      p = Poll(newPoll.title, id.toString, Poll.zeroedTallies(newPoll.options), newPoll.options)
      unit <- redis.hmSet[String](name, Poll.pollToMap(p))
    } yield p
  }

  def get(pollId: String): Future[Poll] = {
    val pollName = s"poll:$pollId"
    val poll = redis.hGetAll[String](pollName)
    lazy val exception = throw new Exception("Didn't exist!")
    for {
      o <- poll
    } yield o.fold(exception)(Poll.toPoll(_, pollId).fold(exception)(p => p))
  }

  def incrOption(pollId: String, optionIndex: String): Future[Long] =
    redis.hIncrBy(s"poll:$pollId", optionIndex, 1)
}
