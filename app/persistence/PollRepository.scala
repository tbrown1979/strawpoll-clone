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

  def get(pollId: String): Future[Option[Poll]] = {
    val pollName = s"poll:$pollId"
    val pollMap = redis.hGetAll[String](pollName)
    for {
      o <- pollMap
    } yield for {
      m <- o
      poll <- Poll.toPoll(m, pollId)
    } yield poll
  }

  def incrOption(pollId: String, optionIndex: Int): Future[Option[Long]] = {
    val check = redis.hExists(s"poll:$pollId", s"${optionIndex.toString}")
    check.flatMap(exists => exists match {
      case true => redis.hIncrBy(s"poll:$pollId", s"${optionIndex.toString}", 1).map(Some(_))
      case false => Future.successful(None)
    })
  }
}
