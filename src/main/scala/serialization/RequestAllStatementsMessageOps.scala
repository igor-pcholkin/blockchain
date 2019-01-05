package serialization

import core.{ObjectDecoder, ObjectEncoder}
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import messages.RequestAllStatementsMessage

object RequestAllStatementsMessageOps extends ObjectEncoder[RequestAllStatementsMessage] with ObjectDecoder[RequestAllStatementsMessage] {
  override lazy val encoder: Encoder[RequestAllStatementsMessage] = (message: RequestAllStatementsMessage) => {
    Json.obj(
      ("decoder", "serialization.RequestAllStatementsMessageOps".asJson),
      ("message", message.asJson)
    )
  }

  override lazy val decoder: Decoder[RequestAllStatementsMessage] = deriveDecoder[RequestAllStatementsMessage]

}
