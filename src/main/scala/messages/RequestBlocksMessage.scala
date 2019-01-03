package messages

import core.{Block, Deserializator, Message}
import io.circe.parser.decode
import io.circe
import io.circe.generic.auto._

object RequestBlocksMessage extends Deserializator {
  override def deserialize(s: String): Either[circe.Error, RequestBlocksMessage] = decode[RequestBlocksMessage](s)
}

case class RequestBlocksMessage(fromBlockNo: Int, sentFromIPAddress: String) extends Message

