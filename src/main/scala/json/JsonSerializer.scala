package json

import io.circe.syntax._
import io.circe._
import io.circe.Encoder

object JsonSerializer {
  def serialize[T <: Serializable](msg: T)(implicit encoder: Encoder[T]): String = {
    val printer = Printer.noSpaces.copy(dropNullValues = true)
    printer.pretty(msg.asJson)
  }
}
