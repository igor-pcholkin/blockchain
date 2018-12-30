package messages

import core.{Message, MsgDeserializator}
import io.circe
import io.circe.generic.auto._
import io.circe.parser.decode

object RequestAllStatementsMessage extends MsgDeserializator {
  override def deserialize(s: String): Either[circe.Error, RequestAllStatementsMessage] = decode[RequestAllStatementsMessage](s)
}

case class RequestAllStatementsMessage(toIPAddress: String) extends Message
