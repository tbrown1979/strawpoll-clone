package util

import play.api.Play.current
import akka.actor.ActorSystem
import play.api.libs.concurrent.Akka

trait FutureProvider {
  type ExecutionContext = scala.concurrent.ExecutionContext
  type Future[T] = scala.concurrent.Future[T]


  val Future = scala.concurrent.Future
  def system: ActorSystem
  def future[T](f: => T)(implicit ec: ExecutionContext): Future[T]
}


trait PollFutureProvider extends FutureProvider {
  val system = Akka.system

  def future[T](f: => T)(implicit ec: ExecutionContext): Future[T] = {
    val future = Future.apply(f)(ec)
    future.onFailure {
      case t: Throwable => println("Exception from the future")
    } (ec)
    future
  }
}
