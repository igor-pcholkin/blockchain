package messages

import core.{Deserializator, Message, ObjectDecoder}
import io.circe.parser.decode
import io.circe
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._

object RequestBlocksMessage extends Deserializator with ObjectDecoder[Message] {
  override def deserialize(s: String): Either[circe.Error, RequestBlocksMessage] = decode[RequestBlocksMessage](s)
  override def getDecoder: Decoder[Message] = deriveDecoder[RequestBlocksMessage].asInstanceOf[Decoder[Message]]
}

case class RequestBlocksMessage(fromBlockNo: Int) extends Message {

  override lazy val encoder: Encoder[Message] = new Encoder[RequestBlocksMessage] {
    final def apply(message: RequestBlocksMessage): Json = {
      Json.obj(
        ("messageType", "messages.RequestBlocksMessage".asJson),
        ("message", message.asJson)
      )
    }
  }.asInstanceOf[Encoder[Message]]

}

