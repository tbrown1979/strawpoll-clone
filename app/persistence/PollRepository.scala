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

  // implicit object PollWriter extends Writer[Poll] {
  //   private val utf16StringWriter = new StringWriter("UTF-16")

  //   override def writeImpl(poll: Poll): Array[Byte] = {
  //     utf16StringWriter.write(s"title ${poll.title} options ${poll.answers.toString}")
  //   }
  // }
  implicit object PollReader extends Reader[Poll] {
    val utf16StringReader = new StringReader("UTF-16")

    override def readImpl(bytes: Array[Byte]): Poll = {
      val split = utf16StringReader.read(bytes).split(":")
      //println(IOUtils.toString(bytes))
      Poll("asdfasdf", None, Map[String, Int]("d" -> 4), Seq())
    }
  }

  //maybe just return the id here?
  def create(poll: Poll): Future[Poll] = {
    val pollId = redis.incr("pollId")
    //val answerJson = Json.stringify(Json.toJson(poll.answers))
    for {
      id <- pollId
      name = s"poll:$id"
      unit <- redis.hmSet[Int](name, poll.options)
      unit <- redis.hmSet[String](name, poll.fields.foldLeft(Map[String,String]() -> 0)
        ((b, a) => (b._1.+(s"Option$b._2" -> a) -> (b._2 + 1)))._1)
      unit <- redis.hmSet[String](name, Map("title" -> poll.title))
      //answers <- redis.hGet[String](name, "answers")
    } yield Poll(poll.title, Some(id.toString), poll.options, poll.fields)
  }

  def get(pollId: String): Future[Option[Poll]] = {
    val pollName = s"poll:$pollId"
    val poll = redis.hGetAll[Poll](pollName)
    //val answers = redis.hGet[String](pollName, "answers")

    for {
      o <- poll
    } yield for {
      m <- o
    } yield m.getOrElse(pollName, Poll("", None, Map(),Seq()))

    // for {
    //   maybeT <- title
    //   maybeA <- answers
    // } yield for {
    //   t <- maybeT
    //   a <- maybeA
    //   aMap = Json.parse(a).validate[Map[String,Int]]
    // } yield Poll(t, Some(pollId), aMap.get)
  }

}
