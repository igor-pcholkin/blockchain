package serialization

import core.ObjectEncoder
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Encoder, Json}
import messages.RequestBlocksMessage

object RequestBlocksMessageOps extends ObjectEncoder[RequestBlocksMessage] {
  override def getEncoder: Encoder[RequestBlocksMessage] = new Encoder[RequestBlocksMessage] {
    final def apply(message: RequestBlocksMessage): Json = {
      Json.obj(
        ("messageType", "messages.RequestBlocksMessage".asJson),
        ("message", message.asJson)
      )
    }
  }
}
