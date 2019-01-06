package json

import core.{Message, MessageEnvelope}
import io.circe.{Decoder, HCursor, Json}
import io.circe.Encoder
import io.circe.syntax._
import io.circe.parser.decode

import json.MessageJson._

object MessageEnvelopeJson {
  def deserialize(s: String): Either[io.circe.Error, MessageEnvelope] = {
    decode[MessageEnvelope](s)(envelopeDecoder)
  }

  implicit lazy val envelopeEncoder = new Encoder[MessageEnvelope] {
    override final def apply(messageEnvelope: MessageEnvelope) = {
      val message = messageEnvelope.message
      Json.obj(
        ("message", message.asJson),
        ("sentFromIPAddress", messageEnvelope.sentFromIPAddress.asJson)
      )
    }
  }

  implicit lazy val envelopeDecoder = new Decoder[MessageEnvelope] {
    override final def apply(c: HCursor) = {
      for {
        message <- c.downField("message").as[Message]
        sentFromIPAddress <- c.downField("sentFromIPAddress").as[String]
      } yield {
        new MessageEnvelope(message, sentFromIPAddress)
      }
    }
  }
}
