package core

import io.circe
import io.circe.syntax._
import io.circe.{Encoder, Printer}
import messages._

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

/** message is anything that is transferred between peers and requires serialization */
trait Message extends Serializable {
  def sentFromIPAddress: String
}

trait Deserializator {
  def deserialize(s: String): Either[circe.Error, Serializable]
}
