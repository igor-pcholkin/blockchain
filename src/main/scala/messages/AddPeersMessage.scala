package messages

import core.{Message, Deserializator}
import io.circe
import io.circe.parser.decode
import io.circe.generic.auto._

object AddPeersMessage extends Deserializator {
  override def deserialize(s: String): Either[circe.Error, AddPeersMessage] = decode[AddPeersMessage](s)
}

case class AddPeersMessage(peers: Seq[String], sentFromIPAddress: String) extends Message
