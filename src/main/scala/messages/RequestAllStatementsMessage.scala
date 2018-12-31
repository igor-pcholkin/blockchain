package messages

import core.{Message, Deserializator}
import io.circe
import io.circe.generic.auto._
import io.circe.parser.decode

object RequestAllStatementsMessage extends Deserializator {
  override def deserialize(s: String): Either[circe.Error, RequestAllStatementsMessage] = decode[RequestAllStatementsMessage](s)
}

case class RequestAllStatementsMessage(sentFromIPAddress: String) extends Message
