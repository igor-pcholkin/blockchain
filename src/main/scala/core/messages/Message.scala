package core.messages

import io.circe
import io.circe.{Encoder, Printer}
import io.circe.syntax._

object Message {
  def serialize[T <: Message](msg: T) (implicit encoder: Encoder[T]) = {
    val printer = Printer.noSpaces.copy(dropNullValues = true)
    printer.pretty(msg.asJson)
  }

  def deserialize(s: String): Option[Message] = {

    val deserializers = Stream(InitPaymentMessage, NewBlockMessage, AddPeersMessage)

    deserializers.map { d =>
      d.deserialize(s)
    }.find(_.isRight).flatMap {
      case Right(msg) => Some(msg)
      case _ => None
    }
  }

}

abstract class Message {
  def dataToSign: Array[Byte] = Array[Byte]()

}

trait MsgDeserializator {
  def deserialize(s: String): Either[circe.Error, Message]
}
