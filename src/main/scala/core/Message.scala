package core

import io.circe
import io.circe.syntax._
import io.circe._
import messages._
import io.circe.Encoder

object Serializator {
  def serialize[T <: Serializable](msg: T)(implicit encoder: Encoder[T]): String = {
    val printer = Printer.noSpaces.copy(dropNullValues = true)
    printer.pretty(msg.asJson)
  }

  def deserialize(s: String): Option[Serializable] = {

    val deserializers = Stream(SignedStatementMessage, NewBlockMessage, AddPeersMessage, RequestBlocksMessage, RequestAllStatementsMessage)

    deserializers.map { d =>
      d.deserialize(s)
    }.find(_.isRight).flatMap {
      case Right(msg) => Some(msg)
      case _ => None
    }
  }

}

/** message is any service information that peers use and exchange each which other and requires serialization */
trait Message

/**
  * Message envelope allows to segregate fields which should not be considered a part of message,
  * e.g. sentFromIPAddress - in order not to process the same message several times when it comes from different nodes.
  */
case class MessageEnvelope(message: Message, sentFromIPAddress: String) extends Serializable

trait Deserializator {
  def deserialize(s: String): Either[circe.Error, Serializable]
}
