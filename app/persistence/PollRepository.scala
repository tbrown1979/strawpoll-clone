package persistence

import util._
import actors._
import akka.actor.ActorSystem
import akka.actor._
import models._
import java.net.URI
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
  def get(pollId: String): Future[Option[Poll]]
  def incrOption(pollId: String, optionIndex: Int): Future[Option[Long]]
  def createCustomPoll(newPoll: PollCreation, customId: Option[String] = None): Future[Poll]
  def resetPoll(pollId: String): Future[Option[Poll]]
}

trait RedisPollRepository extends PollRepository with PollFutureProvider {
  val prod = Option(System.getenv("REDISTOGO_URL"))
  Logger.info(prod.toString)
  Logger.info("")
  Logger.info("")
  Logger.info("")
  Logger.info("")
  val redis =
    if (!prod.isEmpty) {
      val redisUri = new URI(System.getenv("REDISCLOUD_URL"))
      val host = redisUri.getHost
      val port = redisUri.getPort
      Redis(host = host, port = port)
    }
    else Redis()

  Logger.info(s"Prod -- $prod")

  import redis.dispatcher

  def create(newPoll: PollCreation): Future[Poll] = {
    createCustomPoll(newPoll)
  }

  def resetPoll(pollId: String): Future[Option[Poll]] = {
    get(pollId).flatMap((o: Option[Poll]) =>
      o.fold(Future.successful(Option.empty[Poll]))
      (p => createCustomPoll(PollCreation(p.title, p.options), Some(pollId)).map(Some(_))))
  }

  def createCustomPoll(newPoll: PollCreation, customId: Option[String] = None): Future[Poll] = {
    val pollId = customId.fold(redis.incr("pollId").map(_.toString))(s => Future.successful(s))

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
      case true =>
        val total = redis.hIncrBy(s"poll:$pollId", "total", 1)
        val vote  = redis.hIncrBy(s"poll:$pollId", s"${optionIndex.toString}", 1).map(Some(_))
        vote
      case false => Future.successful(None)
    })
  }
}
