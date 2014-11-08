package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

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
}
