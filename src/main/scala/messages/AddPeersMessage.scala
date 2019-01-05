package messages

import core.{Deserializator, Message, ObjectDecoder}
import io.circe
import io.circe.{Decoder, Encoder, Json}
import io.circe.parser.decode
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._

object AddPeersMessage extends Deserializator with ObjectDecoder[Message] {
  override def deserialize(s: String): Either[circe.Error, AddPeersMessage] = decode[AddPeersMessage](s)
  override def getDecoder: Decoder[Message] = deriveDecoder[AddPeersMessage].asInstanceOf[Decoder[Message]]
}

case class AddPeersMessage(peers: Seq[String]) extends Message {

  override lazy val encoder: Encoder[Message] = new Encoder[AddPeersMessage] {
    final def apply(message: AddPeersMessage): Json = {
      Json.obj(
        ("messageType", "messages.AddPeersMessage".asJson),
        ("message", message.asJson)
      )
    }
  }.asInstanceOf[Encoder[Message]]

}
