package messages

import io.circe.generic.auto._
import io.circe.parser.decode
import core.{Block, Message, Deserializator}
import io.circe

object NewBlockMessage extends Deserializator {
  override def deserialize(s: String): Either[circe.Error, NewBlockMessage] = decode[NewBlockMessage](s)
}

case class NewBlockMessage(block: Block, sentFromIPAddress: String) extends Message
