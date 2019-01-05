package messages

import io.circe.generic.auto._
import io.circe.parser.decode
import core.{Block, Deserializator, Message, ObjectDecoder}
import io.circe
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._

object NewBlockMessage extends Deserializator with ObjectDecoder[Message] {
  override def deserialize(s: String): Either[circe.Error, NewBlockMessage] = decode[NewBlockMessage](s)
  override def getDecoder: Decoder[Message] = deriveDecoder[NewBlockMessage].asInstanceOf[Decoder[Message]]
}

case class NewBlockMessage(block: Block, blockNo: Int) extends Message {

  override lazy val encoder: Encoder[Message] = new Encoder[NewBlockMessage] {
    final def apply(message: NewBlockMessage): Json = {
      Json.obj(
        ("messageType", "messages.NewBlockMessage".asJson),
        ("message", message.asJson)
      )
    }
  }.asInstanceOf[Encoder[Message]]

}
