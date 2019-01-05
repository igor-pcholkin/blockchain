package messages

import core.{Deserializator, Message, ObjectDecoder}
import io.circe
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.decode
import io.circe.syntax._

object RequestAllStatementsMessage extends Deserializator with ObjectDecoder[Message] {
  override def deserialize(s: String): Either[circe.Error, RequestAllStatementsMessage] = decode[RequestAllStatementsMessage](s)
  override def getDecoder: Decoder[Message] = deriveDecoder[RequestAllStatementsMessage].asInstanceOf[Decoder[Message]]
}

case class RequestAllStatementsMessage() extends Message