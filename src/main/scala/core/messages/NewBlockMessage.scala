package core.messages

import io.circe.generic.auto._
import io.circe.parser.decode
import core.Block
import io.circe

object NewBlockMessage extends MsgDeserializator {
  override def deserialize(s: String): Either[circe.Error, NewBlockMessage] = decode[NewBlockMessage](s)
}

case class NewBlockMessage(block: Block) extends Message {
  override def dataToSign: Array[Byte] = Array[Byte]()
}
