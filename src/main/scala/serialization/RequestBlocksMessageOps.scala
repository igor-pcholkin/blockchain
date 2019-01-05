package serialization

import core.{ObjectDecoder, ObjectEncoder}
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import messages.RequestBlocksMessage

object RequestBlocksMessageOps extends ObjectEncoder[RequestBlocksMessage] with ObjectDecoder[RequestBlocksMessage] {
  override lazy val encoder: Encoder[RequestBlocksMessage] = (message: RequestBlocksMessage) => {
    Json.obj(
      ("decoder", "serialization.RequestBlocksMessageOps".asJson),
      ("message", message.asJson)
    )
  }

  override lazy val decoder: Decoder[RequestBlocksMessage] = deriveDecoder[RequestBlocksMessage]

}
