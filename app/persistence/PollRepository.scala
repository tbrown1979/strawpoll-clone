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
    val answerJson = Json.stringify(Json.toJson(poll.answers))
    for {
      id <- pollId
      name = s"poll:$id"
      unit <- redis.hmSet[String](name, Map("answers" -> answerJson))
      unit <- redis.hmSet[String](name, Map("title" -> poll.title))
      //answers <- redis.hGet[String](name, "answers")
    } yield Poll(poll.title, Some(id.toString), poll.answers)
  }

  def get(pollId: String): Future[Option[Poll]] = {
    val pollName = s"poll:$pollId"
    val title = redis.hGet[String](pollName, "title")
    val answers = redis.hGet[String](pollName, "answers")

    for {
      maybeT <- title
      maybeA <- answers
    } yield for {
      t <- maybeT
      a <- maybeA
      aMap = Json.parse(a).validate[Map[String,Int]]
    } yield Poll(t, Some(pollId), aMap.get)
  }

}
