package serialization

import core.ObjectEncoder
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Encoder, Json}
import messages.NewBlockMessage

object NewBlockMessageOps extends ObjectEncoder[NewBlockMessage] {
  override def getEncoder: Encoder[NewBlockMessage] = new Encoder[NewBlockMessage] {
    final def apply(message: NewBlockMessage): Json = {
      Json.obj(
        ("messageType", "messages.NewBlockMessage".asJson),
        ("message", message.asJson)
      )
    }
  }
}
