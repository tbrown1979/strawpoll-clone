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

  //maybe just return the id here?
  def create(poll: Poll): Future[Poll] = {
    val pollId = redis.incr("pollId")

    for {
      id <- pollId
      unit <- redis.hmSet[Int](s"poll:$id", poll.answers)
      answers <- redis.hGetAll[Int](s"poll:$id")
    } yield Poll(poll.title, Some(id), answers.getOrElse(poll.answers))
  }

}
