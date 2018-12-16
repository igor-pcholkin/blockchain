package core

import java.time.LocalDateTime

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

abstract class Message {
  def serialize: String
}

object InitPaymentMessage {
  def deserialize(s: String) = {
    decode[InitPaymentMessage](s)
  }
}

case class InitPaymentMessage(createdBy: String, from: String, to: String, money: Money, timestamp: LocalDateTime,
                              fromSignature: Option[Array[Byte]] = None) extends Message {
  def dataToSign = (createdBy + from + to + money + timestamp).getBytes

  def serialize: String = this.asJson.toString

}