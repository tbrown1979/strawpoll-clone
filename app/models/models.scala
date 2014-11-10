package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import scredis.serialization._
import scredis.serialization.Implicits._

case class Vote(pollId: String)
case object VoteCast

case class Poll(
  title:   String,
  id:      Option[String],
  options: Map[String,Int],
  fields:  Seq[String]
)

object Poll {
  implicit val pollWrites = new Writes[Poll] {
    def writes(poll: Poll) = Json.obj(
      "title"   -> poll.title,
      "id"      -> poll.id,
      "options" -> poll.options,
      "fields"  -> poll.fields
    )
  }

  implicit val pollReads: Reads[Poll] = (
    (JsPath \ "title").read[String] and
    (JsPath \ "id").readNullable[String] and
    (JsPath \ "options").read[Map[String,Int]] and
    (JsPath \ "fields").read[Seq[String]]
  )(Poll.apply _)

  def toPoll(pm: Map[String, Any], pollId: String): Option[Poll] = {
    try {
      val title = pm("title").asInstanceOf[String]
      val fields = pm.keys.filter(_.toLowerCase.contains("option")).map(o => pm(o)).toSeq.asInstanceOf[Seq[String]]
      val options = pm.keys.filter(o => o.forall(_.isDigit)).map(d => (d -> pm(d))).toMap.asInstanceOf[Map[String,Int]]
      Some(Poll(title, Some(pollId), options, fields))
    } catch {
      case _: Throwable => None
    }
  }


  // implicit object PollWriter extends Writer[Poll] {
  //   private val utf16StringWriter = new StringWriter("UTF-16")

  //   override def writeImpl(poll: Poll): Array[Byte] = {
  //     utf16StringWriter.write(s"title ${poll.title} options ${poll.answers.toString}")
  //   }
  // }
  // implicit object PollReader extends Reader[Poll] {
  //   val utf16StringReader = new StringReader("UTF-16")

  //   override def readImpl(bytes: Array[Byte]): Poll = {
  //     val split = utf16StringReader.read(bytes).split(":")
  //     Poll("asdfasdf", None, Map[String, Int]("d" -> 4))
  //   }
  // }

}
