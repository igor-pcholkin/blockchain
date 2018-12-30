package core

import io.circe
import io.circe.syntax._
import io.circe.{Encoder, Printer}
import messages.{AddPeersMessage, NewBlockMessage, RequestAllStatementsMessage}

object Message {
  def serialize[T <: Message](msg: T)(implicit encoder: Encoder[T]): String = {
    val printer = Printer.noSpaces.copy(dropNullValues = true)
    printer.pretty(msg.asJson)
  }

  def deserialize(s: String): Option[Message] = {

    val deserializers = Stream(SignedStatement, NewBlockMessage, AddPeersMessage, RequestAllStatementsMessage)

    deserializers.map { d =>
      d.deserialize(s)
    }.find(_.isRight).flatMap {
      case Right(msg) => Some(msg)
      case _ => None
    }
  }

}

/** message is anything that is transferred between peers and requires serialization */
trait Message

trait MsgDeserializator {
  def deserialize(s: String): Either[circe.Error, Message]
}
