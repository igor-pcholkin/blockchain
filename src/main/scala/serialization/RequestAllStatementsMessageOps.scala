package serialization

import core.ObjectEncoder
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Encoder, Json}
import messages.RequestAllStatementsMessage

object RequestAllStatementsMessageOps extends ObjectEncoder[RequestAllStatementsMessage] {
  override def getEncoder: Encoder[RequestAllStatementsMessage] = new Encoder[RequestAllStatementsMessage] {
    final def apply(message: RequestAllStatementsMessage): Json = {
      Json.obj(
        ("messageType", "messages.RequestAllStatementsMessage".asJson),
        ("message", message.asJson)
      )
    }
  }
}
