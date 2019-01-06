package json

import core.{ObjectDecoder, ObjectEncoder}
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._
import messages.AddPeersMessage

object AddPeersMessageJson extends ObjectEncoder[AddPeersMessage] with ObjectDecoder[AddPeersMessage] {
  override lazy val encoder: Encoder[AddPeersMessage] = (message: AddPeersMessage) => {
    Json.obj(
      ("decoder", "json.AddPeersMessageJson".asJson),
      ("message", message.asJson)
    )
  }

  override lazy val decoder: Decoder[AddPeersMessage] = deriveDecoder[AddPeersMessage]

}
