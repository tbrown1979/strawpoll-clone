package persistence

import actors._
import akka.actor._
import play.api.Play.current
import play.api._
import play.api.libs.json._
import play.api.mvc._
import scredis._
import akka.actor.ActorSystem
import play.api.libs.concurrent.Akka

// trait Repository[T] {
//   def get(id: String): Option[T]
//   def update(id: String)
// }

// trait PollRepository {
//   def get(pollId: String): Option[Poll]

// }

trait RedisPollRepository {
  val redis = Redis()
  def addPoll


  val redis = Redis()
  import redis.dispatcher
  val timeout = 5 seconds

  val test = redis.incr("pollNumber")

  val plest: Future[Option[Map[String,String]]] = for {
    n <- test
    u <- test.map(n => redis.hmSet(s"poll:$n", Map("1" -> 4, "2" -> 2)))
    g <- {println("running after"); redis.hGetAll(s"poll:$n")}
  } yield g


  plest onComplete {
    case Success(c) =>
      println(c)
    //redis.quit()
    case _ => println("broke")
  }
}
