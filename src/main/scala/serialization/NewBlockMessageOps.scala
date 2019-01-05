package serialization

import core.{ObjectDecoder, ObjectEncoder}
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import messages.NewBlockMessage

object NewBlockMessageOps extends ObjectEncoder[NewBlockMessage] with ObjectDecoder[NewBlockMessage] {
  override lazy val encoder: Encoder[NewBlockMessage] = (message: NewBlockMessage) => {
    Json.obj(
      ("decoder", "serialization.NewBlockMessageOps".asJson),
      ("message", message.asJson)
    )
  }

  override lazy val decoder: Decoder[NewBlockMessage] = deriveDecoder[NewBlockMessage]
}
