package serialization

import core.ObjectEncoder
import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._
import messages.AddPeersMessage

object AddPeersMessageOps extends ObjectEncoder[AddPeersMessage] {
  override def getEncoder: Encoder[AddPeersMessage] = new Encoder[AddPeersMessage] {
    final def apply(message: AddPeersMessage): Json = {
      Json.obj(
        ("messageType", "messages.AddPeersMessage".asJson),
        ("message", message.asJson)
      )
    }
  }
}
