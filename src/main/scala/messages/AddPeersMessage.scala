package messages

import core.{Deserializator, Message, ObjectDecoder}
import io.circe
import io.circe.Decoder
import io.circe.parser.decode
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder

object AddPeersMessage extends Deserializator with ObjectDecoder[Message] {
  override def deserialize(s: String): Either[circe.Error, AddPeersMessage] = decode[AddPeersMessage](s)
  override def getDecoder: Decoder[Message] = deriveDecoder[AddPeersMessage].asInstanceOf[Decoder[Message]]
}

case class AddPeersMessage(peers: Seq[String]) extends Message
