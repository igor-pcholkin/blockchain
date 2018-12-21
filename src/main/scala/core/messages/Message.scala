package core.messages

import io.circe.{Printer, Encoder}
import io.circe.syntax._

object Message {
  def serialize[T <: Message](msg: T) (implicit encoder: Encoder[T]) = {
    val printer = Printer.noSpaces.copy(dropNullValues = true)
    printer.pretty(msg.asJson)
  }
}

abstract class Message {
  def dataToSign: Array[Byte]

}

