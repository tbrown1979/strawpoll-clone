package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Vote(pollId: String)
case object VoteCast

case class Poll(title: String, id: Option[Long], answers: Map[String,Int])

object Poll {
  implicit val pollWrites = new Writes[Poll] {
    def writes(poll: Poll) = Json.obj(
      "title" -> poll.title,
      "id" -> poll.id,
      "answers" -> poll.answers
    )
  }

  implicit val pollReads: Reads[Poll] = (
    (JsPath \ "title").read[String] and
    (JsPath \ "id").readNullable[Long] and
    (JsPath \ "answers").read[Map[String,Int]]
  )(Poll.apply _)
}
