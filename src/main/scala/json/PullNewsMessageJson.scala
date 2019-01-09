package json

import core.{ObjectDecoder, ObjectEncoder}
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import messages.PullNewsMessage

object PullNewsMessageJson extends ObjectEncoder[PullNewsMessage] with ObjectDecoder[PullNewsMessage] {
  override lazy val encoder: Encoder[PullNewsMessage] = (message: PullNewsMessage) => {
    Json.obj(
      ("decoder", "json.PullNewsMessageJson".asJson),
      ("message", message.asJson)
    )
  }

  override lazy val decoder: Decoder[PullNewsMessage] = deriveDecoder[PullNewsMessage]

}
