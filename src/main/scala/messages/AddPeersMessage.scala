package messages

import io.circe
import io.circe.parser.decode
import io.circe.generic.auto._

object AddPeersMessage extends MsgDeserializator {
  override def deserialize(s: String): Either[circe.Error, AddPeersMessage] = decode[AddPeersMessage](s)
}

case class AddPeersMessage(peers: Seq[String]) extends Message
