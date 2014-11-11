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
  def create(poll: Poll): Future[Poll]
}

trait RedisPollRepository extends PollRepository with PollFutureProvider {
  val redis = Redis()
  import redis.dispatcher

  private def pollToMap(poll: Poll): Map[String, String] = {
    val titleMap  = Map("title" -> poll.title)
    val tallyMap  = poll.tallies.map(tally => (tally._1, tally._2.toString))
    val optionMap = poll.options.foldLeft(Map[String,String]() -> 0)(
      (b, a) => (b._1.+(s"Option${b._2}" -> a) -> (b._2 + 1)))._1
    titleMap ++ tallyMap ++ optionMap
  }


  def create(poll: Poll): Future[Poll] = {
    val pollId = redis.incr("pollId")

    for {
      id <- pollId
      name = s"poll:$id"
      // unit <- redis.hmSet[Int](name, poll.tallies)
      // unit <- redis.hmSet[String](name, poll.options.foldLeft(Map[String,String]() -> 0)
      //   ((b, a) => (b._1.+(s"Option${b._2}" -> a) -> (b._2 + 1)))._1)
      // unit <- redis.hmSet[String](name, Map("title" -> poll.title))
      unit <- redis.hmSet[String](name, pollToMap(poll))
    } yield Poll(poll.title, Some(id.toString), poll.tallies, poll.options)
  }

  def get(pollId: String): Future[Poll] = {
    val pollName = s"poll:$pollId"
    val poll = redis.hGetAll[String](pollName)
    lazy val exception = throw new Exception("Didn't exist!")
    for {
      o <- poll
    } yield o.fold(exception)(Poll.toPoll(_, pollId).fold(exception)(p => p))
  }
}
