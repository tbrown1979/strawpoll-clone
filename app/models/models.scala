package models

import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scredis.serialization._
import scredis.serialization.Implicits._

case class SocketVote(pollId: String)

case class Vote(pollId: String, index: Int)
object Vote {
  implicit val voteReads: Reads[Vote] = (
    (JsPath \ "pollId").read[String] and
    (JsPath \ "index").read[Int]
  )(Vote.apply _)
}

//case class VoteCast(pollId: String)
case class Votes(tallies: Vector[Int], total: Int)
object Votes {
  implicit val votesWrites = new Writes[Votes] {
    def writes(votes: Votes) = Json.obj(
      "tallies" -> votes.tallies,
      "total"   -> votes.total
    )
  }
}

case class Poll(
  title:   String,
  id:      String,
  tallies: Vector[Int],
  options: Seq[String],
  total:   Int = 0
)

object Poll {
  def zeroedTallies(options: Seq[String]): Vector[Int] = {
    (0 to options.length - 1).toVector.map(_ => 0)
  }

  implicit val pollWrites = new Writes[Poll] {
    def writes(poll: Poll) = {
      Json.obj(
        "title"   -> poll.title,
        "id"      -> poll.id,
        "tallies" -> poll.tallies,
        "options" -> poll.options,
        "total"   -> poll.total
      )
    }
  }

  implicit val pollReads: Reads[Poll] = (
    (JsPath \ "title").read[String] and
    (JsPath \ "id").read[String] and
    (JsPath \ "tallies").read[Vector[Int]] and
    (JsPath \ "options").read[Seq[String]] and
    (JsPath \ "total").read[Int]
  )(Poll.apply _)

  implicit def toPoll(pm: Map[String, Any], pollId: String): Option[Poll] = {
    try {
      val title = pm("title").asInstanceOf[String]
      val options = pm.keys.filter(_.toLowerCase.contains("option")).toList
        .sortWith((s1: String, s2: String) => s1.last.toInt < s2.last.toInt)
        .map(o => pm(o)).toSeq.asInstanceOf[Seq[String]]
      val tallies = pm.keys.filter(o => o.forall(_.isDigit)).toVector
        .map(d => d.toString.toInt).sorted
        .map(d => pm(d.toString).toString.toInt).asInstanceOf[Vector[Int]]
      val total = pm("total").asInstanceOf[String].toInt

      Some(Poll(title, pollId, tallies, options, total))
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
    Logger.info(optionMap.toString)
    val total     = Map("total" -> poll.total.toString)
    titleMap ++ tallyMap ++ optionMap ++ id ++ total
  }
}
