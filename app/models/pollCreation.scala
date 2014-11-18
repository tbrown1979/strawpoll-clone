package models

import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scredis.serialization._
import scredis.serialization.Implicits._

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
