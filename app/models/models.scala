package models

import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scredis.serialization._
import scredis.serialization.Implicits._

case class Vote(pollId: String, index: Int)
object Vote {
  implicit val voteReads: Reads[Vote] = (
    (JsPath \ "pollId").read[String] and
    (JsPath \ "index").read[Int]
  )(Vote.apply _)
}

//case class VoteCast(pollId: String)

case class Poll(
  title:   String,
  id:      String,
  tallies: Vector[Int],
  options: Seq[String]
)

object Poll {
  def zeroedTallies(options: Seq[String]): Vector[Int] = {
    println((0 to options.length - 1).toVector.map(_ => 0))
    (0 to options.length - 1).toVector.map(_ => 0)
  }

  implicit val pollWrites = new Writes[Poll] {
    def writes(poll: Poll) = Json.obj(
      "title"   -> poll.title,
      "id"      -> poll.id,
      "tallies" -> poll.tallies,
      "options"  -> poll.options
    )
  }

  implicit val pollReads: Reads[Poll] = (
    (JsPath \ "title").read[String] and
    (JsPath \ "id").read[String] and
    (JsPath \ "tallies").read[Vector[Int]] and
    (JsPath \ "options").read[Seq[String]]
  )(Poll.apply _)

  implicit def toPoll(pm: Map[String, Any], pollId: String): Option[Poll] = {
    try {
      val title = pm("title").asInstanceOf[String]
      val options = pm.keys.filter(_.toLowerCase.contains("option")).toList
        .sortWith((s1: String, s2: String) => s1.last.toInt < s2.last.toInt)
        .map(o => pm(o)).toSeq.asInstanceOf[Seq[String]]
      val tallies = pm.keys.filter(o => o.forall(_.isDigit)).toVector
        .sorted.map(d => pm(d).toString.toInt).asInstanceOf[Vector[Int]]

      Some(Poll(title, pollId, tallies, options))
    } catch {
      case e: Throwable =>
        Logger.info(e.toString)
        None
    }
  }

  implicit def pollToMap(poll: Poll): Map[String, String] = {
    val titleMap  = Map("title" -> poll.title)
    val id        = Map("id" -> poll.id)
    val tallyMap  = (0 to poll.tallies.length - 1)
      .map(_.toString).zip(poll.tallies.map(_.toString))
    val optionMap = poll.options.foldLeft(Map[String,String]() -> 0)(
      (b, a) => (b._1.+(s"Option${b._2}" -> a) -> (b._2 + 1)))._1
    titleMap ++ tallyMap ++ optionMap ++ id
  }
}
