package json

import core.{ObjectDecoder, ObjectEncoder}
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import messages.NewBlockMessage

object NewBlockMessageJson extends ObjectEncoder[NewBlockMessage] with ObjectDecoder[NewBlockMessage] {
  override lazy val encoder: Encoder[NewBlockMessage] = (message: NewBlockMessage) => {
    Json.obj(
      ("decoder", "json.NewBlockMessageJson".asJson),
      ("message", message.asJson)
    )
  }

  override lazy val decoder: Decoder[NewBlockMessage] = deriveDecoder[NewBlockMessage]
}
