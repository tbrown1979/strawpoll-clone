package models

import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scredis.serialization._
import scredis.serialization.Implicits._

case class Vote(pollId: String)
case object VoteCast

case class PollCreation(
  title: String,
  options: Seq[String]
)

object PollCreation {
  implicit val pollWrites = new Writes[PollCreation] {
    def writes(poll: PollCreation) = Json.obj(
      "title"   -> poll.title,
      "options"  -> poll.options
    )
  }

  implicit val pollReads: Reads[PollCreation] = (
    (JsPath \ "title").read[String] and
    (JsPath \ "options").read[Seq[String]]
  )(PollCreation.apply _)
}

case class Poll(
  title:   String,
  id:      String,
  tallies: Map[String,Int],
  options:  Seq[String]
)

object Poll {
  def zeroedTallies(options: Seq[String]): Map[String,Int] = {
    (0 to options.length - 1).toList.map(n => (n.toString -> 0)).toMap
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
    (JsPath \ "tallies").read[Map[String,Int]] and
    (JsPath \ "options").read[Seq[String]]
  )(Poll.apply _)

  implicit def toPoll(pm: Map[String, Any], pollId: String): Option[Poll] = {
    try {
      val title = pm("title").asInstanceOf[String]
      val options = pm.keys.filter(_.toLowerCase.contains("option")).map(o => pm(o)).toSeq.asInstanceOf[Seq[String]]
      val tallies = pm.keys.filter(o => o.forall(_.isDigit)).map(d => (d -> pm(d))).toMap.asInstanceOf[Map[String,Int]]
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
    val tallyMap  = poll.tallies.map(tally => (tally._1, tally._2.toString))
    val optionMap = poll.options.foldLeft(Map[String,String]() -> 0)(
      (b, a) => (b._1.+(s"Option${b._2}" -> a) -> (b._2 + 1)))._1
    titleMap ++ tallyMap ++ optionMap ++ id
  }
}
